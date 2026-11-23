package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.str;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScopeService {
    private final BusinessDb db;

    public ScopeService(BusinessDb db) {
        this.db = db;
    }

    public Set<String> descendants(IdentityService.Actor actor) {
        if (actor.orgUnitId() == null) return Set.of();
        var rows =
                db.rows(
                        "SELECT id,parent_id FROM org_unit WHERE enterprise_id=?",
                        actor.enterpriseId());
        Set<String> nodes = new HashSet<>(Set.of(actor.orgUnitId()));
        boolean changed;
        do {
            changed = false;
            for (var row : rows)
                if (nodes.contains(str(row, "parent_id"))) changed |= nodes.add(str(row, "id"));
        } while (changed);
        return nodes;
    }

    public Set<String> managed(IdentityService.Actor actor) {
        actor.manager();
        Set<String> orgs = descendants(actor), users = new HashSet<>();
        for (var row :
                db.rows(
                        "SELECT u.id,u.org_unit_id,MIN(CASE r.role_code WHEN 'FOUNDER' THEN 0 WHEN"
                            + " 'DEPARTMENT_MANAGER' THEN 1 WHEN 'TEAM_LEAD' THEN 2 WHEN 'EMPLOYEE'"
                            + " THEN 3 ELSE 99 END) AS business_rank FROM app_user u JOIN user_role"
                            + " r ON r.enterprise_id=u.enterprise_id AND r.user_id=u.id WHERE"
                            + " u.enterprise_id=? GROUP BY u.id,u.org_unit_id",
                        actor.enterpriseId())) {
            int rank = ((Number) row.get("business_rank")).intValue();
            if (!actor.id().equals(row.get("id"))
                    && rank > actor.businessRank()
                    && rank != 99
                    && (actor.has("FOUNDER") || orgs.contains(str(row, "org_unit_id"))))
                users.add(str(row, "id"));
        }
        return users;
    }

    public Set<String> activeManaged(IdentityService.Actor actor) {
        var users = managed(actor);
        Set<String> active = new HashSet<>();
        for (var row :
                db.rows(
                        "SELECT id FROM app_user WHERE enterprise_id=? AND status='ACTIVE'",
                        actor.enterpriseId()))
            if (users.contains(str(row, "id"))) active.add(str(row, "id"));
        return active;
    }

    public boolean readable(IdentityService.Actor actor, String owner) {
        actor.business();
        return actor.id().equals(owner)
                || (actor.businessRank() < 3 && managed(actor).contains(owner));
    }

    /**
     * 任务接收人授权：全企业在职业务用户（不受组织层级与子树限制）+ 创建者本人。
     * 任何业务用户（含 EMPLOYEE）都可创建派发任务，也允许只派给自己；
     * 日志范围、成员通讯录、审批者-提交者关系等授权仍走 {@link #managed}。
     */
    public Set<String> taskRecipients(IdentityService.Actor actor, Collection<String> recipients) {
        actor.business();
        var allowed = activeBusinessUsers(actor);
        allowed.add(actor.id());
        return allowed;
    }

    /** 本企业 ACTIVE 且拥有业务角色（FOUNDER/DEPARTMENT_MANAGER/TEAM_LEAD/EMPLOYEE）的用户。 */
    public Set<String> activeBusinessUsers(IdentityService.Actor actor) {
        Set<String> users = new HashSet<>();
        for (var row :
                db.rows(
                        "SELECT DISTINCT u.id FROM app_user u JOIN user_role r ON"
                            + " r.enterprise_id=u.enterprise_id AND r.user_id=u.id WHERE"
                            + " u.enterprise_id=? AND u.status='ACTIVE' AND u.employment_status='IN_SERVICE' AND r.role_code IN"
                            + " ('FOUNDER','DEPARTMENT_MANAGER','TEAM_LEAD','EMPLOYEE')",
                        actor.enterpriseId())) users.add(str(row, "id"));
        return users;
    }

    public void read(IdentityService.Actor actor, String owner) {
        if (!readable(actor, owner)) throw BusinessException.missing();
    }
}
