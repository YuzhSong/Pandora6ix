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
    private final CredentialHandoffService handoffs;
    private final PersonnelResponsibilityPolicy responsibility;

    public DirectoryService(BusinessDb db, IdentityService identity, CredentialHandoffService handoffs, PersonnelResponsibilityPolicy responsibility) {
        this.db = db;
        this.identity = identity;
        this.handoffs = handoffs;
        this.responsibility = responsibility;
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
            if (("DEPARTMENT".equals(kind) && !"COMPANY".equals(p.get("kind")))
                    || "TEAM".equals(p.get("kind"))
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
        if (number(old, "version") != c.expectedVersion()) checkVersion(0);
        if (!c.kind().equals(old.get("kind"))) {
            if ("COMPANY".equals(old.get("kind")) || "COMPANY".equals(c.kind()))
                throw BusinessException.invalid("Company root kind cannot be changed.");
            if (!db.rows("SELECT id FROM org_unit WHERE enterprise_id=? AND parent_id=?",
                    a.enterpriseId(), id).isEmpty())
                throw BusinessException.invalid("Move child organizations before changing kind.");
            String incompatible = "TEAM".equals(c.kind()) ? "DEPARTMENT_MANAGER" : "TEAM_LEAD";
            if (!db.rows("SELECT u.id FROM app_user u JOIN user_role r ON r.enterprise_id=u.enterprise_id AND r.user_id=u.id WHERE u.enterprise_id=? AND u.org_unit_id=? AND r.role_code=?",
                    a.enterpriseId(), id, incompatible).isEmpty())
                throw BusinessException.invalid("Manager roles do not match the new organization kind.");
        }
        orgParent(a, id, c.kind(), c.parentId());
        checkVersion(
                db.update(
                        "UPDATE org_unit SET name=?,kind=?,parent_id=?,version=version+1 WHERE"
                                + " enterprise_id=? AND id=? AND version=?",
                        c.name(),
                        c.kind(),
                        c.parentId(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        return view(db.entity("org_unit", a.enterpriseId(), id));
    }

    @Transactional
    public void deleteOrg(IdentityService.Actor a, String id, Contracts.OrgDelete c) {
        a.admin();
        lockEnterprise(a);
        var old = db.entity("org_unit", a.enterpriseId(), id);
        if (number(old, "version") != c.expectedVersion()) checkVersion(0);
        if ("COMPANY".equals(old.get("kind")))
            throw new BusinessException(409, "ORG_ROOT_DELETE_FORBIDDEN", "Company root cannot be deleted.");
        if (!db.rows("SELECT id FROM org_unit WHERE enterprise_id=? AND parent_id=?", a.enterpriseId(), id).isEmpty())
            throw new BusinessException(409, "ORG_HAS_CHILDREN", "Move or delete child organizations first.");
        if (!db.rows("SELECT id FROM app_user WHERE enterprise_id=? AND org_unit_id=?", a.enterpriseId(), id).isEmpty())
            throw new BusinessException(409, "ORG_HAS_MEMBERS", "Move all assigned accounts, including disabled accounts, first.");
        checkVersion(db.update("DELETE FROM org_unit WHERE enterprise_id=? AND id=? AND version=?",
                a.enterpriseId(), id, c.expectedVersion()));
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

    public Map<String, Object> user(IdentityService.Actor a, String id) {
        a.admin();
        var row = db.entity("app_user", a.enterpriseId(), id);
        var result = view(row);
        result.put("gender", row.get("gender"));
        result.put("idNumber", row.get("id_number"));
        result.put("phoneNumber", row.get("phone_number"));
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
        validatePhone(c.phoneNumber());
        userRefs(a, id, c.orgUnitId(), c.supervisorUserId(), c.roles());
        db.update(
                "INSERT INTO"
                    + " app_user(id,enterprise_id,login_name,password_hash,display_name,org_unit_id,supervisor_user_id,job_title,gender,id_number,phone_number)"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                id,
                a.enterpriseId(),
                c.loginName(),
                identity.encode(c.password()),
                c.displayName(),
                c.orgUnitId(),
                c.supervisorUserId(),
                c.jobTitle(), c.gender(), c.idNumber(), c.phoneNumber());
        roles(a, id, c.roles());
        if (!c.roles().contains("ADMIN")) handoffs.capture(a.enterpriseId(), id, c.password(), str(db.entity("app_user", a.enterpriseId(), id), "password_hash"));
        return user(a, id);
    }

    @Transactional
    public Map<String, Object> patchUser(
            IdentityService.Actor a, String id, Contracts.UserPatch c) {
        a.admin();
        lockEnterprise(a);
        var old = db.entity("app_user", a.enterpriseId(), id);
        if ("DEPARTED".equals(old.get("employment_status")))
            throw new BusinessException(409,"EMPLOYEE_DEPARTED","Employee has departed.");
        if (number(old, "version") != c.expectedVersion()) checkVersion(0);
        if (c.loginName() != null) {
            if (c.loginName().isBlank() || !c.loginName().equals(c.loginName().strip()))
                throw new BusinessException(
                        422, "VALIDATION_FAILED", "Login name must not be blank or have surrounding whitespace.",
                        Map.of("fieldErrors", List.of(Map.of(
                                "field", "loginName", "message", "must not be blank or have surrounding whitespace"))));
            if (!db.rows(
                    "SELECT id FROM app_user WHERE enterprise_id=? AND login_name=? AND id<>?",
                    a.enterpriseId(), c.loginName(), id).isEmpty())
                throw new BusinessException(
                        409, "LOGIN_NAME_TAKEN", "This login name is already used in the company.");
        }
        validatePhone(c.phoneNumber());
        if (!Set.of("ACTIVE", "DISABLED").contains(c.status()))
            throw BusinessException.invalid("Unknown account status.");
        responsibility.assertRelease(a,old,c.orgUnitId(),c.roles(),c.status());
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
                            + " login_name=COALESCE(?,login_name),display_name=?,org_unit_id=?,supervisor_user_id=?,job_title=?,status=?,gender=COALESCE(?,gender),id_number=COALESCE(?,id_number),phone_number=COALESCE(?,phone_number),version=version+1"
                            + " WHERE enterprise_id=? AND id=? AND version=?",
                        c.loginName(),
                        c.displayName(),
                        c.orgUnitId(),
                        c.supervisorUserId(),
                        c.jobTitle(),
                        c.status(),
                        c.gender(), c.idNumber(), c.phoneNumber(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        roles(a, id, c.roles());
        if (c.roles().contains("ADMIN") || "DISABLED".equals(c.status())) handoffs.forget(a.enterpriseId(), id);
        if ("DISABLED".equals(c.status()))
            db.update(
                    "UPDATE auth_session SET revoked=TRUE WHERE enterprise_id=? AND user_id=?",
                    a.enterpriseId(),
                    id);
        return user(a, id);
    }

    @Transactional
    public Map<String, Object> resetPassword(IdentityService.Actor a, String id, Contracts.UserPasswordReset c) {
        a.admin();
        lockEnterprise(a);
        var old = db.lock("app_user", a.enterpriseId(), id);
        if ("DEPARTED".equals(old.get("employment_status")))
            throw new BusinessException(409,"EMPLOYEE_DEPARTED","Employee has departed.");
        if (identity.actor(id, a.enterpriseId(), str(old, "org_unit_id"), null).has("ADMIN"))
            throw new BusinessException(403, "ADMIN_PASSWORD_RESET_FORBIDDEN", "Use the administrator's own password-change flow.");
        if (number(old, "version") != c.expectedVersion()) checkVersion(0);
        String hash = identity.encode(c.newPassword());
        checkVersion(db.update("UPDATE app_user SET password_hash=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",
                hash, a.enterpriseId(), id, c.expectedVersion()));
        db.update("UPDATE auth_session SET revoked=TRUE WHERE enterprise_id=? AND user_id=?", a.enterpriseId(), id);
        handoffs.capture(a.enterpriseId(), id, c.newPassword(), hash);
        return user(a, id);
    }

    private void validatePhone(String phone) {
        if (phone != null && phone.replaceAll("[^0-9]", "").length() < 6)
            throw BusinessException.invalid("phoneNumber must contain at least six digits.");
    }
}
