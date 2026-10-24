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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class OperationLogService {
    private final BusinessDb db; private final JsonMapper json;
    public OperationLogService(BusinessDb db, JsonMapper json) { this.db = db; this.json = json; }
    public void record(IdentityService.Actor actor, String objectId, String objectType, String module,
                       String action, String source, String summary, Map<String,Object> before, Map<String,Object> after) {
        var actorRow = db.entity("app_user", actor.enterpriseId(), actor.id());
        String actorName = str(actorRow, "display_name"), actorLogin = str(actorRow, "login_name");
        String objectLabel = after == null ? null : Objects.toString(after.get("displayName"), null);
        if (objectLabel == null && before != null) objectLabel = Objects.toString(before.get("displayName"), null);
        if (objectLabel == null) objectLabel = summary;
        Object requestId = null;
        var attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes)
            requestId = servletAttributes.getRequest().getAttribute(com.pandora6ix.backend.api.error.RequestIdFilter.ATTRIBUTE_NAME);
        Object beforeVersion = before == null ? null : before.get("version");
        Object afterVersion = after == null ? null : after.get("version");
        db.update("INSERT INTO operation_log(id,enterprise_id,actor_user_id,object_id,object_type,module,action,source,result,summary,request_id,before_values,after_values,actor_display_name,actor_login_name,object_label,before_version,after_version) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                UUID.randomUUID().toString(), actor.enterpriseId(), actor.id(), objectId, objectType, module, action, source, "SUCCESS", summary,
                requestId, before == null ? null : json.writeValueAsString(before), after == null ? null : json.writeValueAsString(after),
                actorName, actorLogin, objectLabel, beforeVersion, afterVersion);
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
    private Map<String,Object> summary(Map<String,Object> row) { Object actorName=row.get("actor_display_name")!=null?row.get("actor_display_name"):row.get("actor_name"); Object actorLogin=row.get("actor_login_name")!=null?row.get("actor_login_name"):row.get("actor_login"); return fields("id",str(row,"id"),"occurredAt",row.get("occurred_at"),"actor",fields("id",row.get("actor_user_id"),"displayName",actorName,"loginName",actorLogin),"object",fields("id",row.get("object_id"),"type",str(row,"object_type"),"label",row.get("object_label")),"module",str(row,"module"),"action",str(row,"action"),"source",str(row,"source"),"result",str(row,"result"),"summary",str(row,"summary")); }
    public Map<String,Object> detail(IdentityService.Actor actor, String id) {
        actor.admin(); var row=db.optional("SELECT * FROM operation_log WHERE enterprise_id=? AND id=?",actor.enterpriseId(),id).orElseThrow(BusinessException::missing);
        var out=summary(row); out.put("requestId",row.get("request_id")); out.put("failureReason",row.get("failure_reason")); out.put("before",snapshot(row.get("before_values"),row.get("before_version"))); out.put("after",snapshot(row.get("after_values"),row.get("after_version")));
        var history=db.rows("SELECT id,after_version,occurred_at,actor_display_name,action FROM operation_log WHERE enterprise_id=? AND object_type=? AND object_id=? AND result='SUCCESS' ORDER BY occurred_at,id",actor.enterpriseId(),str(row,"object_type"),str(row,"object_id"));
        out.put("versions",history.stream().map(v->fields("logId",str(v,"id"),"version",v.get("after_version"),"occurredAt",v.get("occurred_at"),"actorName",v.get("actor_display_name"),"action",str(v,"action"))).toList()); return out;
    }
    @SuppressWarnings("unchecked") private Object snapshot(Object raw, Object version) { if(raw==null)return null; Map<String,Object> m=json.readValue(String.valueOf(raw),Map.class); List<Map<String,Object>> fs=m.entrySet().stream().filter(e->!"version".equals(e.getKey())&&!sensitive(e.getKey())).map(e->fields("key",e.getKey(),"label",label(e.getKey()),"value",display(e.getValue()))).toList(); return fields("version",version,"fields",fs); }
    private boolean sensitive(String key) { String normalized=key.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT); return normalized.contains("password")||normalized.contains("token")||normalized.contains("idnumber")||normalized.contains("phonenumber")||normalized.contains("credential"); }
    private String label(String key) { return switch(key) { case "orgUnitName" -> "所属组织"; case "supervisorName" -> "直属上级"; case "displayName" -> "姓名"; case "loginName" -> "登录账号"; case "jobTitle" -> "职位"; case "orgUnitId" -> "组织 ID"; case "supervisorUserId" -> "上级人员 ID"; case "roles" -> "角色"; case "status" -> "账号状态"; case "employmentStatus" -> "在职状态"; case "departureDate" -> "离职日期"; default -> key; }; }
    private String display(Object value) { if(value==null)return null; if(value instanceof Collection<?> values)return String.join("、",values.stream().map(String::valueOf).toList()); return String.valueOf(value); }
}
