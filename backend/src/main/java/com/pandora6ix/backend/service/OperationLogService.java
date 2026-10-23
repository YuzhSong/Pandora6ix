package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;
import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class OperationLogService {
    private final BusinessDb db; private final JsonMapper json;
    public OperationLogService(BusinessDb db, JsonMapper json) { this.db = db; this.json = json; }
    public void record(IdentityService.Actor actor, String objectId, String objectType, String module,
                       String action, String source, String summary, Map<String,Object> before, Map<String,Object> after) {
        db.update("INSERT INTO operation_log(id,enterprise_id,actor_user_id,object_id,object_type,module,action,source,result,summary,request_id,before_values,after_values) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(), actor.enterpriseId(), actor.id(), objectId, objectType, module, action, source, "SUCCESS", summary,
                null, before == null ? null : json.writeValueAsString(before), after == null ? null : json.writeValueAsString(after));
    }
    public Map<String,Object> list(IdentityService.Actor actor, String keyword, String module, String action, String source,
                                   String result, OffsetDateTime from, OffsetDateTime to, String cursor, String requestedSnapshot, int limit) {
        actor.admin();
        String q = "SELECT l.*,u.display_name AS actor_name,u.login_name AS actor_login FROM operation_log l LEFT JOIN app_user u ON u.enterprise_id=l.enterprise_id AND u.id=l.actor_user_id WHERE l.enterprise_id=?";
        List<Object> args = new ArrayList<>(List.of(actor.enterpriseId()));
        if (keyword != null && !keyword.isBlank()) { q += " AND (LOWER(l.summary) LIKE LOWER(?) OR LOWER(COALESCE(u.display_name,'')) LIKE LOWER(?) OR LOWER(COALESCE(u.login_name,'')) LIKE LOWER(?))"; String k=Pages.like(keyword.trim()); args.add(k); args.add(k); args.add(k); }
        if (module != null && !module.isBlank()) { q += " AND l.module=?"; args.add(module); }
        if (action != null && !action.isBlank()) { q += " AND l.action=?"; args.add(action); }
        if (source != null && !source.isBlank()) { q += " AND l.source=?"; args.add(source); }
        if (result != null && !result.isBlank()) { q += " AND l.result=?"; args.add(result); }
        if (from != null) { q += " AND l.occurred_at>=?"; args.add(from); }
        if (to != null) { q += " AND l.occurred_at<?"; args.add(to); }
        String scope = actor.enterpriseId()+":operation-logs:"+String.join("|", Objects.toString(keyword,""),Objects.toString(module,""),Objects.toString(action,""),Objects.toString(source,""),Objects.toString(result,""),Objects.toString(from,""),Objects.toString(to,""));
        String snapshotToken = requestedSnapshot;
        Instant cutoff = Instant.now();
        if (snapshotToken != null) {
            try {
                String[] parts = new String(Base64.getUrlDecoder().decode(snapshotToken), StandardCharsets.UTF_8).split("\\|",2);
                if (parts.length != 2 || !parts[0].equals(IdentityService.hash(scope))) throw new IllegalArgumentException();
                cutoff = Instant.parse(parts[1]);
            } catch (RuntimeException ex) { throw BusinessException.invalid("Snapshot token is invalid or its filters changed; reload the list."); }
        } else {
            snapshotToken = Base64.getUrlEncoder().withoutPadding().encodeToString((IdentityService.hash(scope)+"|"+cutoff).getBytes(StandardCharsets.UTF_8));
        }
        q += " AND l.occurred_at<=?"; args.add(OffsetDateTime.ofInstant(cutoff, java.time.ZoneOffset.UTC));
        String pageScope = scope+":"+cutoff;
        var page = Pages.sqlPage(db,q,args,cursor,limit,pageScope,List.of("occurred_at","id"),true,this::summary);
        var countArgs = new ArrayList<>(args);
        var total = db.one("SELECT COUNT(*) AS total FROM ("+q+") counted", countArgs.toArray());
        page.put("total", number(total,"total"));
        page.put("snapshotToken", snapshotToken);
        return page;
    }
    private Map<String,Object> summary(Map<String,Object> row) { return fields("id",str(row,"id"),"occurredAt",row.get("occurred_at"),"actor",fields("id",row.get("actor_user_id"),"displayName",str(row,"actor_name"),"loginName",row.get("actor_login")),"object",fields("id",row.get("object_id"),"type",str(row,"object_type"),"label",str(row,"summary")),"module",str(row,"module"),"action",str(row,"action"),"source",str(row,"source"),"result",str(row,"result"),"summary",str(row,"summary")); }
    public Map<String,Object> detail(IdentityService.Actor actor, String id) {
        actor.admin(); var row=db.optional("SELECT l.*,u.display_name AS actor_name,u.login_name AS actor_login FROM operation_log l LEFT JOIN app_user u ON u.enterprise_id=l.enterprise_id AND u.id=l.actor_user_id WHERE l.enterprise_id=? AND l.id=?",actor.enterpriseId(),id).orElseThrow(BusinessException::missing);
        var out=summary(row); out.put("requestId",row.get("request_id")); out.put("failureReason",row.get("failure_reason")); out.put("before",snapshot(row.get("before_values"))); out.put("after",snapshot(row.get("after_values"))); out.put("versions",List.of(fields("logId",id,"version",0,"occurredAt",row.get("occurred_at"),"actorName",str(row,"actor_name"),"action",str(row,"action")))); return out;
    }
    @SuppressWarnings("unchecked") private Object snapshot(Object raw) { if(raw==null)return null; Map<String,Object> m=json.readValue(String.valueOf(raw),Map.class); List<Map<String,Object>> fs=m.entrySet().stream().map(e->fields("key",e.getKey(),"label",e.getKey(),"value",e.getValue()==null?null:String.valueOf(e.getValue()))).toList(); return fields("version",0,"fields",fs); }
}
