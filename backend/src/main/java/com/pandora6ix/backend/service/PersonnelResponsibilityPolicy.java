package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;
import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class PersonnelResponsibilityPolicy {
    private final BusinessDb db;
    private final IdentityService identity;
    public PersonnelResponsibilityPolicy(BusinessDb db, IdentityService identity) { this.db=db; this.identity=identity; }
    public void assertRelease(IdentityService.Actor a,Map<String,Object> old,String nextOrg,List<String> nextRoles,String nextStatus) {
        String id=str(old,"id"), org=str(old,"org_unit_id");
        var roles=identity.actor(id,a.enterpriseId(),org,null).roles();
        for(String role:List.of("FOUNDER","DEPARTMENT_MANAGER","TEAM_LEAD")) {
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
