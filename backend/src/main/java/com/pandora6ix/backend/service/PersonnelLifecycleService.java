package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.*;
import com.pandora6ix.backend.repository.BusinessDb;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import java.time.*;
import java.util.*;

@Service
public class PersonnelLifecycleService {
    private final BusinessDb db;
    private final DirectoryService directory;
    private final IdentityService identity;
    private final JsonMapper json;
    private final OperationLogService operationLogs;
    public PersonnelLifecycleService(BusinessDb db, DirectoryService directory, IdentityService identity, JsonMapper json, OperationLogService operationLogs) {
        this.db=db; this.directory=directory; this.identity=identity; this.json=json; this.operationLogs=operationLogs;
    }
    private Map<String,Object> lock(IdentityService.Actor a, String id, long version) {
        a.admin();
        db.one("SELECT id FROM enterprise WHERE id=? FOR UPDATE",a.enterpriseId());
        var old=db.entity("app_user",a.enterpriseId(),id);
        if(number(old,"version")!=version) BusinessException.checkVersion(0);
        if("DEPARTED".equals(old.get("employment_status"))) throw new BusinessException(409,"EMPLOYEE_DEPARTED","Employee has departed.");
        return old;
    }
    private List<String> roles(IdentityService.Actor a, Map<String,Object> user) {
        return new ArrayList<>(identity.actor(str(user,"id"),a.enterpriseId(),str(user,"org_unit_id"),null).roles());
    }
    private LocalDate today(IdentityService.Actor a) {
        String zone=str(db.one("SELECT * FROM enterprise WHERE id=?",a.enterpriseId()),"business_timezone");
        return LocalDate.now(ZoneId.of(zone==null?"UTC":zone));
    }
    private Map<String,Object> snapshot(IdentityService.Actor a, Map<String,Object> user) {
        String orgId=str(user,"org_unit_id"), supervisorId=str(user,"supervisor_user_id");
        String orgName=orgId==null?null:str(db.entity("org_unit",a.enterpriseId(),orgId),"name");
        String supervisorName=null;
        if(supervisorId!=null) supervisorName=str(db.entity("app_user",a.enterpriseId(),supervisorId),"display_name");
        return fields("version",number(user,"version"),"displayName",str(user,"display_name"),"loginName",str(user,"login_name"),
            "employmentStatus",user.get("employment_status"),"departureDate",str(user,"departure_date"),
            "jobTitle",user.get("job_title"),"orgUnitId",user.get("org_unit_id"),
            "orgUnitName",orgName,"supervisorUserId",supervisorId,"supervisorName",supervisorName,
            "roles",roles(a,user),"status",user.get("status"));
    }
    private void record(IdentityService.Actor a, String id, String kind, LocalDate date, Map<String,Object> before) {
        var actor=db.entity("app_user",a.enterpriseId(),a.id());
        db.update("INSERT INTO personnel_change(id,enterprise_id,user_id,actor_user_id,kind,effective_date,before_values,after_values,actor_display_name,actor_login_name) VALUES(?,?,?,?,?,?,?,?,?,?)",
            id(),a.enterpriseId(),id,a.id(),kind,date,json.writeValueAsString(before),
            json.writeValueAsString(snapshot(a,db.entity("app_user",a.enterpriseId(),id))),str(actor,"display_name"),str(actor,"login_name"));
    }
    @Transactional
    public Map<String,Object> depart(IdentityService.Actor a,String id,Contracts.UserDeparture c) {
        var old=lock(a,id,c.expectedVersion());
        if(id.equals(a.id())) throw new BusinessException(409,"SELF_DEPARTURE_FORBIDDEN","Cannot process own departure.");
        if(c.departureDate().isAfter(today(a))) throw BusinessException.invalid("Departure date cannot be in the future.");
        if(!db.rows("SELECT id FROM app_user WHERE enterprise_id=? AND supervisor_user_id=? AND employment_status='IN_SERVICE'",a.enterpriseId(),id).isEmpty())
            throw new BusinessException(409,"DIRECT_REPORTS_REMAIN","Adjust direct reports before departure.");
        var before=snapshot(a,old);
        var updated=directory.patchUser(a,id,new Contracts.UserPatch(c.expectedVersion(),str(old,"display_name"),str(old,"org_unit_id"),null,str(old,"job_title"),"DISABLED",roles(a,old),null,null,null,null));
        db.update("UPDATE app_user SET employment_status='DEPARTED',departure_date=? WHERE enterprise_id=? AND id=?",c.departureDate(),a.enterpriseId(),id);
        record(a,id,"DEPARTURE",c.departureDate(),before);
        operationLogs.record(a,id,"USER","PERSONNEL","DEPARTURE","WEB","办理离职",before,snapshot(a,db.entity("app_user",a.enterpriseId(),id)));
        updated.put("employmentStatus","DEPARTED"); updated.put("departureDate",c.departureDate().toString());
        return updated;
    }
    @Transactional
    public Map<String,Object> changeJob(IdentityService.Actor a,String id,Contracts.UserJobChange c) {
        var old=lock(a,id,c.expectedVersion());
        if(id.equals(a.id())) throw new BusinessException(409,"SELF_JOB_CHANGE_FORBIDDEN","Ask another administrator to adjust this account.");
        var before=snapshot(a,old);
        validateHierarchy(a,id,c.supervisorUserId(),c.roles());
        var updated=directory.patchUser(a,id,new Contracts.UserPatch(c.expectedVersion(),str(old,"display_name"),c.orgUnitId(),c.supervisorUserId(),c.jobTitle(),str(old,"status"),c.roles(),null,null,null,null));
        db.update("UPDATE auth_session SET revoked=TRUE WHERE enterprise_id=? AND user_id=?",a.enterpriseId(),id);
        record(a,id,"JOB_CHANGE",today(a),before);
        operationLogs.record(a,id,"USER","PERSONNEL","JOB_CHANGE","WEB","调整员工职位",before,snapshot(a,db.entity("app_user",a.enterpriseId(),id)));
        return updated;
    }
    private int rank(Collection<String> r) {
        return r.contains("FOUNDER")?0:r.contains("DEPARTMENT_MANAGER")?1:r.contains("TEAM_LEAD")?2:r.contains("EMPLOYEE")?3:99;
    }

    @Transactional
    public Map<String,Object> replaceManager(IdentityService.Actor a,String id,Contracts.ManagerReplacement c) {
        var old=lock(a,id,c.expectedVersion());
        if(id.equals(a.id()) || c.successorUserId().equals(a.id()) || id.equals(c.successorUserId()))
            throw new BusinessException(409,"INVALID_MANAGER_SUCCESSOR","Choose a different employee; current administrator cannot participate.");
        var successor=lock(a,c.successorUserId(),c.successorExpectedVersion());
        var oldRoles=roles(a,old); int oldRank=rank(oldRoles);
        if(oldRank>2 || !"ACTIVE".equals(old.get("status")) || !"ACTIVE".equals(successor.get("status")))
            throw new BusinessException(409,"INVALID_MANAGER_SUCCESSOR","Both managers must be active and in service.");
        String role=oldRank==0?"FOUNDER":oldRank==1?"DEPARTMENT_MANAGER":"TEAM_LEAD";
        String org=str(old,"org_unit_id");
        if(org==null) throw BusinessException.invalid("Manager organization is required.");
        String kind=str(db.entity("org_unit",a.enterpriseId(),org),"kind");
        if(!kind.equals(oldRank==0?"COMPANY":oldRank==1?"DEPARTMENT":"TEAM")) throw BusinessException.invalid("Manager organization does not match role.");
        var successorRoles=roles(a,successor);
        if(!((rank(successorRoles)==oldRank && Objects.equals(org,str(successor,"org_unit_id"))) || (rank(successorRoles)==3 && db.rows("SELECT id FROM app_user WHERE enterprise_id=? AND supervisor_user_id=? AND employment_status='IN_SERVICE'",a.enterpriseId(),c.successorUserId()).isEmpty())))
            throw new BusinessException(409,"INVALID_MANAGER_SUCCESSOR","Choose an employee without other manager responsibilities.");
        String parent=oldRank==0?null:str(old,"supervisor_user_id");
        if(Objects.equals(parent,c.successorUserId())) throw new BusinessException(409,"INVALID_MANAGER_SUCCESSOR","Successor cannot be the current manager's supervisor.");
        if(parent!=null) {
            var superior=db.entity("app_user",a.enterpriseId(),parent);
            if(!"ACTIVE".equals(superior.get("status")) || rank(roles(a,superior))>=oldRank)
                throw BusinessException.invalid("Correct the current manager's supervisor first.");
        }
        var affected=new LinkedHashMap<String,Map<String,Object>>();
        affected.put(id,snapshot(a,old)); affected.put(c.successorUserId(),snapshot(a,successor));
        var reports=db.rows("SELECT * FROM app_user WHERE enterprise_id=? AND supervisor_user_id=? AND employment_status='IN_SERVICE'",a.enterpriseId(),id);
        for(var report:reports) {
            String reportId=str(report,"id");
            if(reportId.equals(c.successorUserId())) continue;
            if(rank(roles(a,report))<=oldRank) throw BusinessException.invalid("A direct report has an incompatible rank.");
            affected.put(reportId,snapshot(a,report));
        }
        var nextRoles=new LinkedHashSet<>(successorRoles); nextRoles.removeAll(Set.of("FOUNDER","DEPARTMENT_MANAGER","TEAM_LEAD")); nextRoles.add(role);
        replaceRoles(a,c.successorUserId(),nextRoles);
        BusinessException.checkVersion(db.update("UPDATE app_user SET org_unit_id=?,supervisor_user_id=?,job_title=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",org,parent,c.successorJobTitle(),a.enterpriseId(),c.successorUserId(),c.successorExpectedVersion()));
        var formerRoles=new LinkedHashSet<>(oldRoles); formerRoles.removeAll(Set.of("FOUNDER","DEPARTMENT_MANAGER","TEAM_LEAD")); formerRoles.add("EMPLOYEE");
        replaceRoles(a,id,formerRoles);
        BusinessException.checkVersion(db.update("UPDATE app_user SET supervisor_user_id=?,job_title=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",c.successorUserId(),c.formerJobTitle(),a.enterpriseId(),id,c.expectedVersion()));
        for(String userId:affected.keySet()) {
            if(!userId.equals(id) && !userId.equals(c.successorUserId()))
                db.update("UPDATE app_user SET supervisor_user_id=?,version=version+1 WHERE enterprise_id=? AND id=?",c.successorUserId(),a.enterpriseId(),userId);
            db.update("UPDATE auth_session SET revoked=TRUE WHERE enterprise_id=? AND user_id=?",a.enterpriseId(),userId);
            record(a,userId,"JOB_CHANGE",today(a),affected.get(userId));
            operationLogs.record(a,userId,"USER","PERSONNEL","MANAGER_REPLACEMENT","WEB","更换负责人",affected.get(userId),snapshot(a,db.entity("app_user",a.enterpriseId(),userId)));
        }
        return fields("items",affected.keySet().stream().map(userId->directory.user(a,userId)).toList());
    }
    private void replaceRoles(IdentityService.Actor a,String id,Collection<String> roles) {
        db.update("DELETE FROM user_role WHERE enterprise_id=? AND user_id=?",a.enterpriseId(),id);
        for(String role:roles) db.update("INSERT INTO user_role VALUES(?,?,?)",a.enterpriseId(),id,role);
    }
    private void validateHierarchy(IdentityService.Actor a,String id,String supervisor,List<String> r) {
        int own=rank(r);
        if(supervisor!=null) {
            var s=db.entity("app_user",a.enterpriseId(),supervisor);
            if(own==99 || rank(roles(a,s))>=own) throw BusinessException.invalid("Supervisor must have a higher business rank.");
        }
        for(var report:db.rows("SELECT * FROM app_user WHERE enterprise_id=? AND supervisor_user_id=? AND employment_status='IN_SERVICE'",a.enterpriseId(),id))
            if(own>=rank(roles(a,report)) || own>=3) throw new BusinessException(409,"REPORTING_ADJUSTMENT_REQUIRED","Adjust direct reports before changing roles.");
    }
    public Map<String,Object> history(IdentityService.Actor a,String id) {
        a.admin(); db.entity("app_user",a.enterpriseId(),id);
        var rows=db.rows("SELECT * FROM personnel_change WHERE enterprise_id=? AND user_id=? ORDER BY recorded_at DESC,id DESC",a.enterpriseId(),id);
        return fields("items",rows.stream().map(row->{ var value=view(row); value.put("actorName",row.get("actor_display_name")); value.put("actorLoginName",row.get("actor_login_name")); value.put("beforeValues",json.readValue(str(row,"before_values"),Map.class)); value.put("afterValues",json.readValue(str(row,"after_values"),Map.class)); return value; }).toList());
    }
}
