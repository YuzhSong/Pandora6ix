package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;
import com.pandora6ix.backend.repository.BusinessDb;
import org.springframework.stereotype.Service;
import java.util.*;

/** Whitelisted business snapshots; called inside each successful mutation transaction. */
@Service
public class MutationAudit {
    private final BusinessDb db;
    private final OperationLogService logs;
    public MutationAudit(BusinessDb db, OperationLogService logs) { this.db=db; this.logs=logs; }
    private String person(IdentityService.Actor actor, String id) {
        if(id==null) return null;
        return db.optional("SELECT display_name FROM app_user WHERE enterprise_id=? AND id=?",actor.enterpriseId(),id).map(r->str(r,"display_name")).orElse(null);
    }
    public Map<String,Object> task(IdentityService.Actor actor,String id) {
        var row=db.entity("task",actor.enterpriseId(),id);
        var snapshot=fields("version",row.get("version"),"title",row.get("title"),"description",row.get("description"),
                "kind",row.get("task_kind"),"priorityLevel",row.get("priority_level"),"primaryTagId",row.get("primary_tag_id"),
                "reviewStatus",row.get("review_status"),"startDate",row.get("start_date"),"endDate",row.get("end_date"),
                "timeKind",row.get("time_kind"),"startMinute",row.get("start_minute"),"endMinute",row.get("end_minute"),
                "creatorUserId",row.get("creator_user_id"),"creatorName",person(actor,str(row,"creator_user_id")));
        snapshot.put("recipients",db.rows("SELECT a.recipient_user_id,u.display_name,a.execution_status,a.version FROM task_assignment a JOIN app_user u ON u.enterprise_id=a.enterprise_id AND u.id=a.recipient_user_id WHERE a.enterprise_id=? AND a.task_id=? ORDER BY a.id",actor.enterpriseId(),id));
        snapshot.put("tags",tags(actor,"task_tag","task_id",id));
        return snapshot;
    }
    public Map<String,Object> journal(IdentityService.Actor actor,String id) {
        var row=db.entity("work_log",actor.enterpriseId(),id);
        return fields("version",row.get("version"),"workDate",row.get("work_date"),"authorUserId",row.get("author_user_id"),
                "authorName",person(actor,str(row,"author_user_id")),"primaryTagId",row.get("primary_tag_id"),
                "tags",tags(actor,"work_log_tag","work_log_id",id),
                "entries",db.rows("SELECT id,content,display_order,version FROM work_log_entry WHERE enterprise_id=? AND work_log_id=? ORDER BY display_order,id",actor.enterpriseId(),id));
    }
    private List<Map<String,Object>> tags(IdentityService.Actor actor,String table,String column,String id) {
        return db.rows("SELECT t.id,t.name,t.color_hex FROM "+table+" x JOIN enterprise_tag t ON t.enterprise_id=x.enterprise_id AND t.id=x.tag_id WHERE x.enterprise_id=? AND x."+column+"=? ORDER BY t.id",actor.enterpriseId(),id);
    }
    public Map<String,Object> assignment(IdentityService.Actor actor,String id) {
        var row=db.entity("task_assignment",actor.enterpriseId(),id);
        return fields("version",row.get("version"),"taskId",row.get("task_id"),"recipientUserId",row.get("recipient_user_id"),
                "recipientName",person(actor,str(row,"recipient_user_id")),"executionStatus",row.get("execution_status"),
                "progressPercent",row.get("progress_percent"),"workStatus",row.get("work_status"),"completedAt",row.get("completed_at"));
    }
    public void record(IdentityService.Actor actor,String id,String type,String module,String action,String summary,Map<String,Object> before,Map<String,Object> after) {
        logs.record(actor,id,type,module,action,OperationLogService.clientSource("SYSTEM"),summary,before,after);
    }
}
