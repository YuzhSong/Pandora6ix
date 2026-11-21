package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;
import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PersonnelResponsibilityPolicy {
    private static final List<String> LEADERSHIP_ROLES = List.of("FOUNDER", "DEPARTMENT_MANAGER", "TEAM_LEAD");
    private final BusinessDb db;
    private final IdentityService identity;
    public PersonnelResponsibilityPolicy(BusinessDb db, IdentityService identity) { this.db=db; this.identity=identity; }

    String organizationalRole(Collection<String> roles) {
        return LEADERSHIP_ROLES.stream().filter(roles::contains).findFirst()
                .orElse(roles.contains("EMPLOYEE") ? "EMPLOYEE" : "ADMIN");
    }

    boolean validOrganization(Collection<String> roles, String kind) {
        return switch (organizationalRole(roles)) {
            case "EMPLOYEE", "TEAM_LEAD" -> "TEAM".equals(kind);
            case "DEPARTMENT_MANAGER" -> "DEPARTMENT".equals(kind);
            case "FOUNDER" -> "COMPANY".equals(kind);
            default -> !"DEPARTMENT".equals(kind);
        };
    }

    void assertOrganization(Collection<String> roles, String kind) {
        if (!validOrganization(roles, kind)) {
            throw new BusinessException(422, "PERSONNEL_ORGANIZATION_MISMATCH", "Employees require a team and only its manager may be directly assigned to a department.");
        }
    }

    void assertAvailable(IdentityService.Actor actor, String organization, Collection<String> roles, Set<String> excludedUsers) {
        if (!occupiedRoles(actor, organization, roles, excludedUsers).isEmpty()) {
            throw new BusinessException(409, "LEADERSHIP_ROLE_TAKEN", "This leadership position is already assigned.");
        }
    }

    Set<String> occupiedRoles(IdentityService.Actor actor, String organization, Collection<String> roles, Set<String> excludedUsers) {
        String holders = "SELECT u.id FROM app_user u JOIN user_role r ON r.enterprise_id=u.enterprise_id AND r.user_id=u.id"
                + " WHERE u.enterprise_id=? AND r.role_code=? AND u.employment_status='IN_SERVICE'";
        Set<String> occupied = new HashSet<>();
        for (String role : LEADERSHIP_ROLES) {
            if (!roles.contains(role)) continue;
            var occupants = role.equals("FOUNDER")
                    ? db.rows(holders, actor.enterpriseId(), role)
                    : db.rows(holders + " AND u.org_unit_id=?", actor.enterpriseId(), role, organization);
            if (occupants.stream().anyMatch(row -> !excludedUsers.contains(str(row, "id")))) {
                occupied.add(role);
            }
        }
        return occupied;
    }

    String organizationSupervisor(IdentityService.Actor actor, String organization, Collection<String> roles, Set<String> excludedUsers) {
        if (roles.contains("ADMIN") || roles.contains("FOUNDER")) return null;
        int rank = roles.contains("DEPARTMENT_MANAGER") ? 1 : roles.contains("TEAM_LEAD") ? 2 : 3;
        Set<String> visited = new HashSet<>();
        while (organization != null && visited.add(organization)) {
            var unit = db.entity("org_unit", actor.enterpriseId(), organization);
            String role = switch (str(unit, "kind")) {
                case "TEAM" -> "TEAM_LEAD";
                case "DEPARTMENT" -> "DEPARTMENT_MANAGER";
                default -> "FOUNDER";
            };
            if (LEADERSHIP_ROLES.indexOf(role) < rank) {
                String sql = "SELECT u.id FROM app_user u JOIN user_role r ON r.enterprise_id=u.enterprise_id AND r.user_id=u.id"
                        + " WHERE u.enterprise_id=? AND r.role_code=? AND u.employment_status='IN_SERVICE' AND u.status='ACTIVE'";
                var candidates = role.equals("FOUNDER") ? db.rows(sql + " ORDER BY u.id", actor.enterpriseId(), role)
                        : db.rows(sql + " AND u.org_unit_id=? ORDER BY u.id", actor.enterpriseId(), role, organization);
                for (var candidate : candidates) {
                    String id = str(candidate, "id");
                    if (!excludedUsers.contains(id)) return id;
                }
            }
            organization = str(unit, "parent_id");
        }
        return null;
    }
    public void assertRelease(IdentityService.Actor a,Map<String,Object> old,String nextOrg,List<String> nextRoles,String nextStatus) {
        String id=str(old,"id"), org=str(old,"org_unit_id");
        var roles=identity.actor(id,a.enterpriseId(),org,null).roles();
        for(String role:LEADERSHIP_ROLES) {
            if(!roles.contains(role) || ("ACTIVE".equals(nextStatus) && nextRoles.contains(role) && (role.equals("FOUNDER") || Objects.equals(org,nextOrg)))) continue;
            if(!db.rows("SELECT id FROM app_user WHERE enterprise_id=? AND supervisor_user_id=? AND employment_status='IN_SERVICE'",a.enterpriseId(),id).isEmpty())
                throw new BusinessException(409,"DIRECT_REPORTS_REMAIN","Replace the manager and reassign direct reports first.");
            boolean needed;
            if(role.equals("FOUNDER")) needed=!db.rows("SELECT id FROM org_unit WHERE enterprise_id=? AND kind<>'COMPANY'",a.enterpriseId()).isEmpty();
            else needed=!db.rows("SELECT id FROM org_unit WHERE enterprise_id=? AND parent_id=?",a.enterpriseId(),org).isEmpty()
                || !db.rows("SELECT id FROM app_user WHERE enterprise_id=? AND org_unit_id=? AND id<>? AND employment_status='IN_SERVICE'",a.enterpriseId(),org,id).isEmpty();
            if(!needed) continue;
            String condition=role.equals("FOUNDER")?"":" AND u.org_unit_id=?";
            Object[] args=role.equals("FOUNDER")?new Object[]{a.enterpriseId(),id,role}:new Object[]{a.enterpriseId(),id,role,org};
            if(db.rows("SELECT u.id FROM app_user u JOIN user_role r ON r.enterprise_id=u.enterprise_id AND r.user_id=u.id WHERE u.enterprise_id=? AND u.id<>? AND r.role_code=? AND u.status='ACTIVE' AND u.employment_status='IN_SERVICE'"+condition,args).isEmpty())
                throw new BusinessException(409,"MANAGER_SUCCESSOR_REQUIRED","Replace the organization manager before removing this role or disabling the account.");
        }
    }
}
