package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.api.BusinessException.checkVersion;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.*;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DirectoryService {
    private static final Set<String> ROLES =
            Set.of("ADMIN", "FOUNDER", "DEPARTMENT_MANAGER", "TEAM_LEAD", "EMPLOYEE");
    private final BusinessDb db;
    private final IdentityService identity;

    public DirectoryService(BusinessDb db, IdentityService identity) {
        this.db = db;
        this.identity = identity;
    }

    public Map<String, Object> enterprise(IdentityService.Actor a) {
        a.admin();
        return view(db.one("SELECT * FROM enterprise WHERE id=?", a.enterpriseId()));
    }

    @Transactional
    public Map<String, Object> enterprisePatch(
            IdentityService.Actor a, Contracts.EnterprisePatch c) {
        a.admin();
        identity.validateZone(c.businessTimezone());
        if (c.name() != null && c.name().isBlank())
            throw BusinessException.invalid("Enterprise name must not be blank.");
        checkVersion(
                db.update(
                        "UPDATE enterprise SET"
                            + " name=COALESCE(?,name),business_timezone=COALESCE(?,business_timezone),version=version+1"
                            + " WHERE id=? AND version=?",
                        c.name(),
                        c.businessTimezone(),
                        a.enterpriseId(),
                        c.expectedVersion()));
        return enterprise(a);
    }

    public Map<String, Object> orgs(IdentityService.Actor a, String cursor, int limit) {
        a.admin();
        return Pages.sqlPage(
                db,
                "SELECT * FROM org_unit WHERE enterprise_id=?",
                List.of(a.enterpriseId()),
                cursor,
                limit,
                a.enterpriseId() + ":orgs",
                List.of("name", "id"),
                false,
                BusinessDb::view);
    }

    private void orgParent(IdentityService.Actor a, String id, String kind, String parent) {
        if (!Set.of("COMPANY", "DEPARTMENT", "TEAM").contains(kind))
            throw BusinessException.invalid("Unknown organization kind.");
        if ("COMPANY".equals(kind)) {
            if (parent != null)
                throw BusinessException.invalid("Company root cannot have a parent.");
            var roots =
                    db.rows(
                            "SELECT id FROM org_unit WHERE enterprise_id=? AND kind='COMPANY'",
                            a.enterpriseId());
            if (roots.stream().anyMatch(r -> !Objects.equals(r.get("id"), id)))
                throw BusinessException.invalid("The enterprise already has a company root.");
        } else {
            if (parent == null)
                throw BusinessException.invalid("Department and team require a parent.");
            var p = db.entity("org_unit", a.enterpriseId(), parent);
            if ("TEAM".equals(p.get("kind"))
                    || ("TEAM".equals(kind) && !"DEPARTMENT".equals(p.get("kind"))))
                throw BusinessException.invalid("Organization parent kind is invalid.");
        }
        Set<String> seen = new HashSet<>();
        String next = parent;
        while (next != null) {
            if (next.equals(id) || !seen.add(next))
                throw BusinessException.invalid("Organization hierarchy cannot contain a cycle.");
            next = str(db.entity("org_unit", a.enterpriseId(), next), "parent_id");
        }
    }

    @Transactional
    public Map<String, Object> createOrg(IdentityService.Actor a, Contracts.OrgCreate c) {
        a.admin();
        lockEnterprise(a);
        String id = id();
        orgParent(a, id, c.kind(), c.parentId());
        db.update(
                "INSERT INTO org_unit(id,enterprise_id,name,kind,parent_id) VALUES(?,?,?,?,?)",
                id,
                a.enterpriseId(),
                c.name(),
                c.kind(),
                c.parentId());
        return view(db.entity("org_unit", a.enterpriseId(), id));
    }

    @Transactional
    public Map<String, Object> patchOrg(IdentityService.Actor a, String id, Contracts.OrgPatch c) {
        a.admin();
        lockEnterprise(a);
        var old = db.entity("org_unit", a.enterpriseId(), id);
        if (!c.kind().equals(old.get("kind")))
            throw BusinessException.policy(
                    "Changing an organization's kind requires a reviewed hierarchy migration.");
        orgParent(a, id, c.kind(), c.parentId());
        checkVersion(
                db.update(
                        "UPDATE org_unit SET name=?,parent_id=?,version=version+1 WHERE"
                                + " enterprise_id=? AND id=? AND version=?",
                        c.name(),
                        c.parentId(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        return view(db.entity("org_unit", a.enterpriseId(), id));
    }

    private void lockEnterprise(IdentityService.Actor a) {
        db.one("SELECT id FROM enterprise WHERE id=? FOR UPDATE", a.enterpriseId());
    }

    public Map<String, Object> users(IdentityService.Actor a, String q, String cursor, int limit) {
        a.admin();
        String query = q == null ? "" : q.strip().toLowerCase(Locale.ROOT);
        return Pages.sqlPage(
                db,
                "SELECT * FROM app_user WHERE enterprise_id=? AND (LOWER(display_name) LIKE ?"
                        + " ESCAPE '!' OR LOWER(login_name) LIKE ? ESCAPE '!')",
                List.of(a.enterpriseId(), Pages.like(query), Pages.like(query)),
                cursor,
                limit,
                a.enterpriseId() + ":users:" + query,
                List.of("display_name", "id"),
                false,
                row -> user(a, str(row, "id")));
    }

    private Map<String, Object> user(IdentityService.Actor a, String id) {
        var row = db.entity("app_user", a.enterpriseId(), id);
        var result = view(row);
        result.put(
                "roles",
                identity.actor(id, a.enterpriseId(), str(row, "org_unit_id"), null).roles());
        return result;
    }

    private void userRefs(
            IdentityService.Actor a, String id, String org, String supervisor, List<String> roles) {
        if (!ROLES.containsAll(roles)) throw BusinessException.invalid("Unknown role code.");
        if (org != null) {
            var unit = db.entity("org_unit", a.enterpriseId(), org);
            String kind = str(unit, "kind");
            if (roles.contains("DEPARTMENT_MANAGER") && !kind.equals("DEPARTMENT")
                    || roles.contains("TEAM_LEAD") && !kind.equals("TEAM"))
                throw BusinessException.invalid(
                        "Manager roles require the corresponding department or team node.");
        } else if (roles.contains("DEPARTMENT_MANAGER") || roles.contains("TEAM_LEAD"))
            throw BusinessException.invalid("Managers require an organization node.");
        Set<String> seen = new HashSet<>();
        String next = supervisor;
        while (next != null) {
            if (next.equals(id) || !seen.add(next))
                throw BusinessException.invalid("Supervisor hierarchy cannot contain a cycle.");
            next = str(db.entity("app_user", a.enterpriseId(), next), "supervisor_user_id");
        }
        if (supervisor != null) {
            var s = db.entity("app_user", a.enterpriseId(), supervisor);
            var actor = identity.actor(supervisor, a.enterpriseId(), str(s, "org_unit_id"), null);
            if (actor.businessRank() >= 3 || !"ACTIVE".equals(s.get("status")))
                throw BusinessException.invalid(
                        "The direct supervisor must be an active business manager.");
        }
    }

    private void roles(IdentityService.Actor a, String id, List<String> roles) {
        db.update(
                "DELETE FROM user_role WHERE enterprise_id=? AND user_id=?", a.enterpriseId(), id);
        new LinkedHashSet<>(roles)
                .forEach(
                        role ->
                                db.update(
                                        "INSERT INTO user_role VALUES(?,?,?)",
                                        a.enterpriseId(),
                                        id,
                                        role));
    }

    @Transactional
    public Map<String, Object> createUser(IdentityService.Actor a, Contracts.UserCreate c) {
        a.admin();
        lockEnterprise(a);
        String id = id();
        userRefs(a, id, c.orgUnitId(), c.supervisorUserId(), c.roles());
        db.update(
                "INSERT INTO"
                    + " app_user(id,enterprise_id,login_name,password_hash,display_name,org_unit_id,supervisor_user_id,job_title)"
                    + " VALUES(?,?,?,?,?,?,?,?)",
                id,
                a.enterpriseId(),
                c.loginName(),
                identity.encode(c.password()),
                c.displayName(),
                c.orgUnitId(),
                c.supervisorUserId(),
                c.jobTitle());
        roles(a, id, c.roles());
        return user(a, id);
    }

    @Transactional
    public Map<String, Object> patchUser(
            IdentityService.Actor a, String id, Contracts.UserPatch c) {
        a.admin();
        lockEnterprise(a);
        var old = db.entity("app_user", a.enterpriseId(), id);
        if (!Set.of("ACTIVE", "DISABLED").contains(c.status()))
            throw BusinessException.invalid("Unknown account status.");
        userRefs(a, id, c.orgUnitId(), c.supervisorUserId(), c.roles());
        if (identity.actor(id, a.enterpriseId(), str(old, "org_unit_id"), null).has("ADMIN")
                && (!c.roles().contains("ADMIN") || !c.status().equals("ACTIVE"))) {
            if (db.rows(
                            "SELECT u.id FROM app_user u JOIN user_role r ON"
                                + " r.enterprise_id=u.enterprise_id AND r.user_id=u.id WHERE"
                                + " u.enterprise_id=? AND u.status='ACTIVE' AND r.role_code='ADMIN'"
                                + " AND u.id<>?",
                            a.enterpriseId(),
                            id)
                    .isEmpty())
                throw new BusinessException(
                        409, "LAST_ADMIN_REQUIRED", "Keep at least one active administrator.");
        }
        checkVersion(
                db.update(
                        "UPDATE app_user SET"
                            + " display_name=?,org_unit_id=?,supervisor_user_id=?,job_title=?,status=?,version=version+1"
                            + " WHERE enterprise_id=? AND id=? AND version=?",
                        c.displayName(),
                        c.orgUnitId(),
                        c.supervisorUserId(),
                        c.jobTitle(),
                        c.status(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        roles(a, id, c.roles());
        if ("DISABLED".equals(c.status()))
            db.update(
                    "UPDATE auth_session SET revoked=TRUE WHERE enterprise_id=? AND user_id=?",
                    a.enterpriseId(),
                    id);
        return user(a, id);
    }
}
