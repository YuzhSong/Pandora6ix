package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.api.BusinessException.checkVersion;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.*;
import com.pandora6ix.backend.domain.task.*;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
public class TaskService
        implements com.pandora6ix.backend.application.tasks.TaskUseCases,
                com.pandora6ix.backend.application.calendar.CalendarUseCases {
    private final MutationAudit audit;
    private final BusinessDb db;
    private final IdentityService identity;
    private final ScopeService scope;
    private final TagService tags;
    private final JsonMapper json;
    private final NotificationService notifications;

    public TaskService(
            BusinessDb db,
            MutationAudit audit,
            IdentityService identity,
            ScopeService scope,
            TagService tags,
            JsonMapper json,
            NotificationService notifications) {
        this.audit = audit;
        this.db = db;
        this.identity = identity;
        this.scope = scope;
        this.tags = tags;
        this.json = json;
        this.notifications = notifications;
    }

    public Map<String, Object> candidates(
            IdentityService.Actor a, String q, String cursor, int limit) {
        var managed = scope.managed(a);
        String search = q == null ? "" : q.strip().toLowerCase(Locale.ROOT);
        StringBuilder sql =
                new StringBuilder(
                        "SELECT u.id,u.display_name,u.org_unit_id,u.job_title,o.name AS"
                            + " organization_name,d.id AS department_id,d.name AS department_name,u.supervisor_user_id FROM app_user u LEFT JOIN"
                            + " org_unit o ON o.enterprise_id=u.enterprise_id AND"
                            + " o.id=u.org_unit_id LEFT JOIN org_unit d ON d.enterprise_id=u.enterprise_id AND d.kind='DEPARTMENT' AND d.id=CASE WHEN o.kind='DEPARTMENT' THEN o.id WHEN o.kind='TEAM' THEN o.parent_id ELSE NULL END WHERE u.enterprise_id=? AND u.status='ACTIVE'");
        List<Object> args = new ArrayList<>(List.of(a.enterpriseId()));
        if (managed.isEmpty()) sql.append(" AND 1=0");
        else {
            sql.append(" AND u.id IN (").append(Pages.placeholders(managed)).append(")");
            args.addAll(managed);
        }
        sql.append(
                " AND (LOWER(u.display_name) LIKE ? ESCAPE '!' OR LOWER(COALESCE(o.name,'')) LIKE ?"
                        + " ESCAPE '!')");
        args.add(Pages.like(search));
        args.add(Pages.like(search));
        return Pages.sqlPage(
                db,
                sql.toString(),
                args,
                cursor,
                limit,
                a.enterpriseId() + ":" + a.id() + ":recipients:" + search,
                List.of("display_name", "id"),
                false,
                row -> {
                    var v = view(row);
                    v.put(
                            "relationship",
                            a.id().equals(row.get("supervisor_user_id")) ? "DIRECT" : "DESCENDANT");
                    v.remove("supervisorUserId");
                    return v;
                });
    }

    /**
     * 任务时间：ALL_DAY 不带时刻（跨天由日期区间表达）；TIMED 带严格 HH:mm。
     * 时间区间的起点是 startDate+startTime、终点是 endDate+endTime，因此**允许跨天**；
     * 同日时结束时刻必须晚于开始时刻，跨天则由日期递增保证区间为正。
     */
    private int[] time(
            TimeKind kind, LocalDate start, LocalDate end, String startTime, String endTime) {
        if (end.isBefore(start))
            throw BusinessException.invalid("endDate must not precede startDate.");
        if (kind == TimeKind.ALL_DAY) {
            if (startTime != null || endTime != null)
                throw BusinessException.invalid("ALL_DAY cannot contain times.");
            return null;
        }
        if (startTime == null
                || endTime == null
                || !startTime.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")
                || !endTime.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]"))
            throw BusinessException.invalid("TIMED requires strict HH:mm times without seconds.");
        int
                from =
                        LocalTime.parse(startTime).getHour() * 60
                                + LocalTime.parse(startTime).getMinute(),
                to = LocalTime.parse(endTime).getHour() * 60 + LocalTime.parse(endTime).getMinute();
        if (start.equals(end) && to <= from)
            throw BusinessException.invalid("TIMED end must be after start on the same day.");
        return new int[] {from, to};
    }

    @Transactional
    public Map<String, Object> create(IdentityService.Actor a, Contracts.TaskCreate c) {
        if (c.recurrence() == null) return createOne(a, c, null, null, true);
        a.business();
        var dates = RecurrenceDates.generate(c.startDate(), c.endDate(), c.recurrence());
        var minutes = time(c.timeKind(), c.startDate(), c.startDate(), c.startTime(), c.endTime());
        var zone = identity.zone(a);
        for (var date : dates) {
            var due = date.atTime(minutes == null ? LocalTime.MAX : LocalTime.of(minutes[1] / 60, minutes[1] % 60)).atZone(zone).toInstant();
            if (!due.isAfter(Instant.now())) throw BusinessException.invalid("重复任务中存在已过截止时间的日期，请调整开始日期。");
        }
        String series = id(), label = RecurrenceDates.label(c.startDate(), c.endDate(), c.recurrence());
        Map<String, Object> first = null;
        for (var date : dates) {
            var occurrence = new Contracts.TaskCreate(c.kind(), c.title(), c.description(), c.priorityLevel(),
                    c.timeKind(), date, date, c.startTime(), c.endTime(), c.recipientIds(), c.tagIds(), c.primaryTagId(), null, c.companyWide());
            var result = createOne(a, occurrence, series, label, first == null);
            if (first == null) first = result;
        }
        first.put("occurrenceCount", dates.size());
        return first;
    }

    private Map<String, Object> createOne(IdentityService.Actor a, Contracts.TaskCreate c, String series, String recurrenceLabel, boolean submitReview) {
        a.business();
        identity.zone(a);
        if (Boolean.TRUE.equals(c.companyWide()) && (!a.has("FOUNDER") || c.kind() != TaskKind.DELEGATED))
            throw BusinessException.forbidden();
        var minutes = time(c.timeKind(), c.startDate(), c.endDate(), c.startTime(), c.endTime());
        var tagIds = tags.validate(a, c.tagIds(), c.primaryTagId());
        List<String> recipients =
                c.recipientIds() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(new LinkedHashSet<>(c.recipientIds()));
        if (c.kind() == TaskKind.PERSONAL) {
            if (recipients.stream().anyMatch(id -> !a.id().equals(id)))
                throw new BusinessException(
                        422,
                        "PERSONAL_RECIPIENT_INVALID",
                        "A personal task can only belong to its creator.");
            recipients = List.of(a.id());
        } else {
            var managed = scope.taskRecipients(a, recipients);
            if (recipients.isEmpty())
                throw BusinessException.invalid("A delegated task requires recipients.");
            if (!managed.containsAll(recipients))
                throw new BusinessException(
                        403,
                        "RECIPIENT_OUT_OF_SCOPE",
                        "All recipients must be authorized subordinate business users.");
        }
        boolean founder = a.has("FOUNDER");
        String supervisor = null;
        if (!founder) {
            var user = db.entity("app_user", a.enterpriseId(), a.id());
            supervisor = str(user, "supervisor_user_id");
            if (supervisor == null || supervisor.equals(a.id()))
                throw BusinessException.policy(
                        "A direct-supervisor approval policy is required; self approval is not"
                                + " supported.");
            var reviewer = db.entity("app_user", a.enterpriseId(), supervisor);
            if (!"ACTIVE".equals(reviewer.get("status"))
                    || identity.actor(supervisor, a.enterpriseId(), str(reviewer, "org_unit_id"), null)
                                    .businessRank()
                            >= 3)
                throw BusinessException.policy("An active business reviewer must be configured.");
        }
        String task = id(), review = id();
        db.update(
                "INSERT INTO"
                    + " task(id,enterprise_id,creator_user_id,task_kind,title,description,priority_level,start_date,end_date,time_kind,start_minute,end_minute,primary_tag_id)"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                task,
                a.enterpriseId(),
                a.id(),
                c.kind().name(),
                c.title(),
                c.description(),
                c.priorityLevel(),
                c.startDate(),
                c.endDate(),
                c.timeKind().name(),
                minutes == null ? null : minutes[0],
                minutes == null ? null : minutes[1],
                c.primaryTagId());
        db.update("UPDATE task SET company_wide=? WHERE enterprise_id=? AND id=?", Boolean.TRUE.equals(c.companyWide()), a.enterpriseId(), task);
        tags.replace(a, "task_tag", "task_id", task, tagIds);
        if (series != null) db.update("UPDATE task SET series_id=?,recurrence_label=?,occurrence_date=? WHERE enterprise_id=? AND id=?",
                series, recurrenceLabel, c.startDate(), a.enterpriseId(), task);
        for (String recipient : recipients)
            db.update(
                    "INSERT INTO task_assignment(id,enterprise_id,task_id,recipient_user_id)"
                            + " VALUES(?,?,?,?)",
                    id(),
                    a.enterpriseId(),
                    task,
                    recipient);
        if (founder) {
            db.update(
                    "UPDATE task SET review_status='APPROVED' WHERE enterprise_id=? AND id=?",
                    a.enterpriseId(),
                    task);
            db.update(
                    "UPDATE task_assignment SET execution_status='ACTIVE',activated_at=CURRENT_TIMESTAMP"
                            + " WHERE enterprise_id=? AND task_id=?",
                    a.enterpriseId(),
                    task);
            notifications.taskApproved(a.enterpriseId(), task, c.title());
        }
        var snapshot =
                fields(
                        "kind",
                        c.kind(),
                        "title",
                        c.title(),
                        "description",
                        c.description(),
                        "priorityLevel",
                        c.priorityLevel(),
                        "timeKind",
                        c.timeKind(),
                        "startDate",
                        c.startDate(),
                        "endDate",
                        c.endDate(),
                        "startTime",
                        c.startTime(),
                        "endTime",
                        c.endTime(),
                        "recipientIds",
                        recipients,
                        "tagIds",
                        tagIds,
                        "primaryTagId",
                        c.primaryTagId());
        snapshot.put("companyWide", Boolean.TRUE.equals(c.companyWide()));
        snapshot.put("seriesId", series);
        snapshot.put("recurrenceLabel", recurrenceLabel);
        if (!founder && submitReview)
            db.update(
                    "INSERT INTO"
                        + " task_review(id,enterprise_id,task_id,reviewer_user_id,submitted_by_user_id,task_version,submitted_snapshot)"
                        + " VALUES(?,?,?,?,?,0,?)",
                    review,
                    a.enterpriseId(),
                    task,
                    supervisor,
                    a.id(),
                    json.writeValueAsString(snapshot));
        if (!founder && submitReview)
            notifications.reviewRequested(a.enterpriseId(), review, task, a.id(), supervisor, c.title());
        audit.record(a,task,"TASK","TASK","CREATE","新增任务",null,audit.task(a,task));
        var result = detail(a, task);
        result.put("taskId", task);
        if (!founder && submitReview) result.put("reviewId", review);
        return result;
    }

    private List<Map<String, Object>> assignments(IdentityService.Actor a, String task) {
        return db.rows(
                "SELECT * FROM task_assignment WHERE enterprise_id=? AND task_id=? AND current_member=TRUE ORDER BY id",
                a.enterpriseId(),
                task);
    }

    private Set<String> managedOrEmpty(IdentityService.Actor a) {
        return a.businessRank() < 3 ? scope.managed(a) : Set.of();
    }

    private boolean visible(
            IdentityService.Actor a,
            Map<String, Object> task,
            List<Map<String, Object>> assignments,
            Set<String> managed) {
        if (Boolean.TRUE.equals(task.get("company_wide")) && "APPROVED".equals(task.get("review_status"))) return true;
        if (a.id().equals(task.get("creator_user_id"))
                || managed.contains(str(task, "creator_user_id"))) return true;
        return assignments.stream()
                .anyMatch(
                        r ->
                                managed.contains(str(r, "recipient_user_id"))
                                        || ("APPROVED".equals(task.get("review_status"))
                                                && a.id().equals(r.get("recipient_user_id"))));
    }

    private String minute(Object value) {
        if (value == null) return null;
        int n = ((Number) value).intValue();
        return String.format(Locale.ROOT, "%02d:%02d", n / 60, n % 60);
    }

    private Map<String, Object> taskView(
            IdentityService.Actor a,
            Map<String, Object> task,
            List<Map<String, Object>> assignments,
            Set<String> managed,
            boolean full) {
        var v = view(task);
        v.put("taskId", v.get("id"));
        v.put("kind", v.remove("taskKind"));
        v.put("startTime", minute(task.get("start_minute")));
        v.put("endTime", minute(task.get("end_minute")));
        v.remove("startMinute");
        v.remove("endMinute");
        var tagList = tags.linked(a, "task_tag", "task_id", str(task, "id"));
        v.put("tags", tagList);
        v.put("tagIds", tagList.stream().map(t -> t.get("id")).toList());
        v.put(
                "displayColor",
                tagList.stream()
                        .filter(t -> Objects.equals(t.get("id"), task.get("primary_tag_id")))
                        .map(t -> t.get("colorHex"))
                        .findFirst()
                        .orElse(null));
        var allowed =
                assignments.stream()
                        .filter(
                                r ->
                                        a.id().equals(task.get("creator_user_id"))
                                                || a.id().equals(r.get("recipient_user_id"))
                                                || managed.contains(str(r, "recipient_user_id")))
                        .map(
                                r -> {
                                    var assignment = view(r);
                                    assignment.put("allowedActions", assignmentActions(a, task, r));
                                    assignment.put("workStatus","COMPLETED".equals(r.get("execution_status"))?"COMPLETED":r.get("work_status"));
                                    assignment.put("allowedWorkStatuses",a.id().equals(r.get("recipient_user_id"))&&"APPROVED".equals(task.get("review_status"))&&"ACTIVE".equals(r.get("execution_status"))?List.of("NOT_STARTED","IN_PROGRESS","COMPLETED"):List.of());
                                    return assignment;
                                })
                        .toList();
        v.put("authorizedAssignments", allowed);
        Set<String> actions = new LinkedHashSet<>();
        if (a.id().equals(task.get("creator_user_id"))
                && task.get("series_id") == null && "PENDING".equals(task.get("review_status"))) actions.add("EDIT_PENDING");
        if (a.id().equals(task.get("creator_user_id")) && "APPROVED".equals(task.get("review_status"))) actions.add("EDIT_TASK");
        for (var r : assignments) actions.addAll(assignmentActions(a, task, r));
        v.put("allowedActions", List.copyOf(actions));
        v.put("pendingEditReviewId",db.optional("SELECT id FROM task_review WHERE enterprise_id=? AND task_id=? AND review_kind='EDIT' AND decision='PENDING' AND task_version=? AND attempt_no=(SELECT MAX(attempt_no) FROM task_review WHERE enterprise_id=? AND task_id=?)",a.enterpriseId(),task.get("id"),task.get("version"),a.enterpriseId(),task.get("id")).map(row->row.get("id")).orElse(null));
        if (full) {
            v.put(
                    "reviewId",
                    db.optional(
                                    "SELECT id FROM task_review WHERE enterprise_id=? AND task_id=?"
                                            + " ORDER BY attempt_no DESC",
                                    a.enterpriseId(),
                                    task.get("id"))
                            .map(r -> r.get("id"))
                            .orElse(null));
            List<Map<String, Object>> history = new ArrayList<>(), requests = new ArrayList<>();
            for (var assignment : allowed) {
                history.addAll(
                        db
                                .rows(
                                        "SELECT * FROM task_progress_record WHERE enterprise_id=?"
                                                + " AND assignment_id=? ORDER BY recorded_at,id",
                                        a.enterpriseId(),
                                        assignment.get("id"))
                                .stream()
                                .map(BusinessDb::view)
                                .toList());
                requests.addAll(
                        db
                                .rows(
                                        "SELECT * FROM task_completion_request WHERE"
                                                + " enterprise_id=? AND assignment_id=? ORDER BY"
                                                + " requested_at,id",
                                        a.enterpriseId(),
                                        assignment.get("id"))
                                .stream()
                                .map(BusinessDb::view)
                                .toList());
            }
            v.put("progressRecords", history);
            v.put("completionRequests", requests);
        } else v.remove("description");
        return v;
    }

    private List<String> assignmentActions(
            IdentityService.Actor a, Map<String, Object> task, Map<String, Object> assignment) {
        if (Boolean.FALSE.equals(assignment.get("current_member")) || !"APPROVED".equals(task.get("review_status"))) return List.of();
        if (a.id().equals(assignment.get("recipient_user_id"))
                && "ACTIVE".equals(assignment.get("execution_status")))
            return List.of(
                    "UPDATE_PROGRESS",
                    "PERSONAL".equals(task.get("task_kind"))
                            ? "COMPLETE_PERSONAL"
                            : "REQUEST_COMPLETION");
        if (a.id().equals(task.get("creator_user_id"))
                && "AWAITING_ACCEPTANCE".equals(assignment.get("execution_status")))
            return List.of("DECIDE_COMPLETION");
        return List.of();
    }

    public Map<String, Object> detail(IdentityService.Actor a, String id) {
        a.business();
        var task = db.entity("task", a.enterpriseId(), id);
        var assignments = assignments(a, id);
        var managed = managedOrEmpty(a);
        if (!visible(a, task, assignments, managed)) throw BusinessException.missing();
        return taskView(a, task, assignments, managed, true);
    }

    private record TaskQuery(String sql, List<Object> args, Set<String> managed) {}

    private TaskQuery query(
            IdentityService.Actor a,
            LocalDate from,
            LocalDate to,
            String requestedScope,
            List<String> tagIds,
            boolean approvedOnly,
            String kind) {
        a.business();
        if (!Set.of("mine", "managed", "created").contains(requestedScope))
            throw BusinessException.invalid("Unknown task scope.");
        if ("managed".equals(requestedScope)) a.manager();
        if (from != null && to != null && (to.isBefore(from) || to.isAfter(from.plusDays(366))))
            throw BusinessException.invalid(
                    "Query one date window of at most 367 days; older windows remain available.");
        var managed = managedOrEmpty(a);
        StringBuilder sql = new StringBuilder("SELECT t.* FROM task t WHERE t.enterprise_id=?");
        List<Object> args = new ArrayList<>(List.of(a.enterpriseId()));
        if (from != null) {
            sql.append(" AND t.end_date>=?");
            args.add(from);
        }
        if (to != null) {
            sql.append(" AND t.start_date<=?");
            args.add(to);
        }
        if (approvedOnly) sql.append(" AND t.review_status='APPROVED'");
        if (kind != null) {
            sql.append(" AND t.task_kind=?");
            args.add(kind);
        }
        switch (requestedScope) {
            case "created" -> {
                sql.append(" AND t.creator_user_id=?");
                args.add(a.id());
            }
            case "managed" -> {
                if (managed.isEmpty()) sql.append(" AND 1=0");
                else {
                    String placeholders = Pages.placeholders(managed);
                    sql.append(" AND (t.creator_user_id IN (")
                            .append(placeholders)
                            .append(
                                    ") OR EXISTS (SELECT 1 FROM task_assignment x WHERE"
                                        + " x.enterprise_id=t.enterprise_id AND x.task_id=t.id AND x.current_member=TRUE AND"
                                        + " x.recipient_user_id IN (")
                            .append(placeholders)
                            .append(")))");
                    args.addAll(managed);
                    args.addAll(managed);
                }
            }
            default -> {
                sql.append(
                        " AND t.review_status='APPROVED' AND EXISTS (SELECT 1 FROM task_assignment"
                                + " x WHERE x.enterprise_id=t.enterprise_id AND x.task_id=t.id AND x.current_member=TRUE AND"
                                + " x.recipient_user_id=?)");
                args.add(a.id());
            }
        }
        for (String tag : new TreeSet<>(tagIds == null ? List.of() : tagIds)) {
            sql.append(
                    " AND EXISTS (SELECT 1 FROM task_tag x WHERE x.enterprise_id=t.enterprise_id"
                            + " AND x.task_id=t.id AND x.tag_id=?)");
            args.add(tag);
        }
        return new TaskQuery(sql.toString(), args, managed);
    }

    private Map<String, Object> queryPage(
            IdentityService.Actor a,
            TaskQuery query,
            String cursor,
            int limit,
            String fingerprint) {
        return Pages.sqlPage(
                db,
                query.sql(),
                query.args(),
                cursor,
                limit,
                fingerprint,
                List.of("start_date", "created_at", "id"),
                false,
                row -> taskView(a, row, assignments(a, str(row, "id")), query.managed(), false));
    }

    public Map<String, Object> list(
            IdentityService.Actor a,
            LocalDate from,
            LocalDate to,
            String requestedScope,
            List<String> tagIds,
            String cursor,
            int limit) {
        var query = query(a, from, to, requestedScope, tagIds, false, null);
        return queryPage(
                a,
                query,
                cursor,
                limit,
                a.enterpriseId()
                        + ":"
                        + a.id()
                        + ":tasks:"
                        + requestedScope
                        + ":"
                        + from
                        + ":"
                        + to
                        + ":"
                        + new TreeSet<>(tagIds == null ? List.of() : tagIds));
    }

    public Map<String, Object> delegatedPage(IdentityService.Actor a, String cursor, int limit) {
        a.business();
        var query = new TaskQuery("SELECT t.* FROM task t WHERE t.enterprise_id=? AND t.company_wide=TRUE AND t.review_status='APPROVED' AND t.task_kind='DELEGATED'", List.of(a.enterpriseId()), managedOrEmpty(a));
        return queryPage(
                a, query, cursor, limit, a.enterpriseId() + ":" + a.id() + ":dashboard-tasks");
    }

    @Transactional
    public Map<String, Object> patch(IdentityService.Actor a, String id, Contracts.TaskPatch c) {
        return patchContent(a,id,c,false);
    }

    private Map<String,Object> patchContent(IdentityService.Actor a,String id,Contracts.TaskPatch c,boolean seriesEdit) {
        a.business();
        var task=db.lock("task",a.enterpriseId(),id);
        if (task.get("series_id") != null && !seriesEdit) throw BusinessException.invalid("重复任务请使用范围编辑。");
        if(!a.id().equals(task.get("creator_user_id"))) throw BusinessException.missing();
        if(number(task,"version")!=c.expectedVersion()) checkVersion(0);
        boolean effective="APPROVED".equals(task.get("review_status"));
        if(!effective&&!"PENDING".equals(task.get("review_status")))
            throw BusinessException.policy("Only pending or approved tasks can be edited.");
        var recipients=c.recipientIds()==null?assignments(a,id).stream().map(r->str(r,"recipient_user_id")).toList():List.copyOf(new LinkedHashSet<>(c.recipientIds()));
        var proposedTask=new HashMap<String,Object>(task);
        if(c.kind()!=null) proposedTask.put("task_kind",c.kind().name());
        boolean companyWide=c.companyWide()==null?Boolean.TRUE.equals(task.get("company_wide")):c.companyWide();
        if(companyWide && (!a.has("FOUNDER") || !"DELEGATED".equals(proposedTask.get("task_kind")))) throw BusinessException.forbidden();
        validateRecipients(a,proposedTask,recipients);
        if("DELEGATED".equals(proposedTask.get("task_kind"))&&!scope.taskRecipients(a,recipients).containsAll(recipients))
            throw new BusinessException(403,"RECIPIENT_OUT_OF_SCOPE","Recipients must remain active authorized users.");
        time(c.timeKind(),c.startDate(),c.endDate(),c.startTime(),c.endTime());
        var tagIds=tags.validate(a,c.tagIds(),c.primaryTagId());
        var snapshot=fields("kind",proposedTask.get("task_kind"),"title",c.title(),"description",c.description(),"priorityLevel",c.priorityLevel(),
                "timeKind",c.timeKind(),"startDate",c.startDate(),"endDate",c.endDate(),"startTime",c.startTime(),"endTime",c.endTime(),
                "recipientIds",recipients,"tagIds",tagIds,"primaryTagId",c.primaryTagId());
        snapshot.put("companyWide",companyWide);
        snapshot.put("seriesId", task.get("series_id"));
        snapshot.put("recurrenceLabel", task.get("recurrence_label"));
        var auditBefore=audit.task(a,id);
        if(a.has("FOUNDER")) {
            applyContent(a,id,snapshot,c.expectedVersion());
            if(!effective) {
                db.update("UPDATE task SET review_status='APPROVED' WHERE enterprise_id=? AND id=?",a.enterpriseId(),id);
                db.update("UPDATE task_assignment SET execution_status='ACTIVE',activated_at=CURRENT_TIMESTAMP,version=version+1 WHERE enterprise_id=? AND task_id=? AND execution_status='PENDING_APPROVAL'",a.enterpriseId(),id);
                notifications.taskApproved(a.enterpriseId(),id,c.title());
            }
            audit.record(a,id,"TASK","TASK","UPDATE","创始人修改任务",auditBefore,audit.task(a,id));
            return detail(a,id);
        }
        String reviewerId=str(db.entity("app_user",a.enterpriseId(),a.id()),"supervisor_user_id");
        if(reviewerId==null||reviewerId.equals(a.id())) throw BusinessException.policy("A direct-supervisor approval policy is required.");
        var reviewer=db.entity("app_user",a.enterpriseId(),reviewerId);
        if(!"ACTIVE".equals(reviewer.get("status"))||identity.actor(reviewerId,a.enterpriseId(),str(reviewer,"org_unit_id"),null).businessRank()>=3)
            throw BusinessException.policy("An active business reviewer must be configured.");
        if(effective) checkVersion(db.update("UPDATE task SET version=version+1 WHERE enterprise_id=? AND id=? AND version=?",a.enterpriseId(),id,c.expectedVersion()));
        else applyContent(a,id,snapshot,c.expectedVersion());
        long attempt=number(db.one("SELECT COALESCE(MAX(attempt_no),0)+1 AS next_attempt FROM task_review WHERE enterprise_id=? AND task_id=?",a.enterpriseId(),id),"next_attempt");
        String reviewId=id();
        db.update("INSERT INTO task_review(id,enterprise_id,task_id,reviewer_user_id,submitted_by_user_id,task_version,submitted_snapshot,attempt_no,review_kind) VALUES(?,?,?,?,?,?,?,?,?)",
                reviewId,a.enterpriseId(),id,reviewerId,a.id(),c.expectedVersion()+1,json.writeValueAsString(snapshot),attempt,effective?"EDIT":"CREATE");
        if(effective) audit.record(a,reviewId,"TASK_REVIEW","TASK","UPDATE","提交任务修改申请",null,fields("version",0,"taskId",id,"title",c.title(),"description",c.description()));
        else audit.record(a,id,"TASK","TASK","UPDATE","修改待审批任务",auditBefore,audit.task(a,id));
        notifications.reviewRequested(a.enterpriseId(),reviewId,id,a.id(),reviewerId,c.title());
        return detail(a,id);
    }

    void validateRecipients(IdentityService.Actor creator, Map<String,Object> task, List<String> recipients) {
        if (recipients.isEmpty()) throw BusinessException.invalid("A task requires recipients.");
        if ("PERSONAL".equals(task.get("task_kind"))) {
            if (!recipients.equals(List.of(creator.id()))) throw BusinessException.invalid("A personal task belongs to its creator.");
        } else if (!scope.taskRecipients(creator, recipients).containsAll(recipients)) {
            throw BusinessException.forbidden();
        }
    }

    @SuppressWarnings("unchecked")
    private void applyRecipients(IdentityService.Actor actor, String taskId, Map<String,Object> content) {
        if (!(content.get("recipientIds") instanceof List<?>)) return;
        var task=db.entity("task",actor.enterpriseId(),taskId);
        var owner=db.entity("app_user",actor.enterpriseId(),str(task,"creator_user_id"));
        var creator=identity.actor(str(owner,"id"),actor.enterpriseId(),str(owner,"org_unit_id"),null);
        var recipients=List.copyOf(new LinkedHashSet<>((List<String>)content.get("recipientIds")));
        validateRecipients(creator,task,recipients);
        for (var old: assignments(actor,taskId)) {
            if (!recipients.contains(str(old,"recipient_user_id")))
                db.update("UPDATE task_assignment SET current_member=FALSE,version=version+1 WHERE enterprise_id=? AND id=?",actor.enterpriseId(),old.get("id"));
        }
        for (String recipient:recipients) {
            var old=db.optional("SELECT id,current_member FROM task_assignment WHERE enterprise_id=? AND task_id=? AND recipient_user_id=?",actor.enterpriseId(),taskId,recipient);
            if (old.isEmpty()) {
                db.update("INSERT INTO task_assignment(id,enterprise_id,task_id,recipient_user_id,execution_status,activated_at) VALUES(?,?,?,?,?,CURRENT_TIMESTAMP)",id(),actor.enterpriseId(),taskId,recipient,"APPROVED".equals(task.get("review_status"))?"ACTIVE":"PENDING_APPROVAL");
            } else if (Boolean.FALSE.equals(old.get().get("current_member"))) {
                db.update("UPDATE task_assignment SET current_member=TRUE,version=version+1 WHERE enterprise_id=? AND id=?",actor.enterpriseId(),old.get().get("id"));
            }
        }
    }

    /** Apply a validated immutable proposal within the caller's task-locked transaction. */
    @SuppressWarnings("unchecked")
    void applyContent(IdentityService.Actor a,String id,Map<String,Object> content,long expectedVersion) {
        var kind=TimeKind.valueOf(String.valueOf(content.get("timeKind")));
        var start=LocalDate.parse(String.valueOf(content.get("startDate")));
        var end=LocalDate.parse(String.valueOf(content.get("endDate")));
        var minutes=time(kind,start,end,(String)content.get("startTime"),(String)content.get("endTime"));
        var tagIds=tags.validate(a,(List<String>)content.get("tagIds"),(String)content.get("primaryTagId"));
        checkVersion(db.update("UPDATE task SET title=?,description=?,priority_level=?,start_date=?,end_date=?,time_kind=?,start_minute=?,end_minute=?,primary_tag_id=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",
                content.get("title"),content.get("description"),content.get("priorityLevel"),start,end,kind.name(),minutes==null?null:minutes[0],minutes==null?null:minutes[1],content.get("primaryTagId"),a.enterpriseId(),id,expectedVersion));
        if(content.get("kind")!=null) db.update("UPDATE task SET task_kind=? WHERE enterprise_id=? AND id=?",String.valueOf(content.get("kind")),a.enterpriseId(),id);
        if(content.containsKey("companyWide")) db.update("UPDATE task SET company_wide=? WHERE enterprise_id=? AND id=?",Boolean.TRUE.equals(content.get("companyWide")),a.enterpriseId(),id);
        tags.replace(a,"task_tag","task_id",id,tagIds);
        applyRecipients(a,id,content);
    }

    private List<Map<String,Object>> scopeTargets(IdentityService.Actor a,String id,String scope,boolean lock) {
        detail(a,id);
        var selected=db.entity("task",a.enterpriseId(),id);
        if("SINGLE".equals(scope)) return List.of(lock?db.lock("task",a.enterpriseId(),id):selected);
        if(!"THIS_AND_FUTURE".equals(scope)||selected.get("series_id")==null) throw BusinessException.invalid("无有效重复任务范围。");
        return db.rows("SELECT * FROM task WHERE enterprise_id=? AND series_id=? AND occurrence_date>=? ORDER BY id"+(lock?" FOR UPDATE":""),
                a.enterpriseId(),selected.get("series_id"),selected.get("occurrence_date"));
    }

    public Map<String,Object> scopeSnapshot(IdentityService.Actor a,String id) {
        var selected=db.entity("task",a.enterpriseId(),id);
        return fields("items",scopeTargets(a,id,selected.get("series_id")==null?"SINGLE":"THIS_AND_FUTURE",false).stream()
                .map(t->detail(a,str(t,"id"))).toList());
    }

    private Contracts.ScopeVersion scopeVersion(List<Contracts.ScopeVersion> versions,Map<String,Object> task) {
        var matches=versions.stream().filter(v->Objects.equals(v.taskId(),task.get("id"))).toList();
        if(matches.size()!=1 || !Objects.equals(matches.getFirst().taskVersion(),number(task,"version"))) checkVersion(0);
        return matches.getFirst();
    }

    @Transactional
    public Map<String,Object> scopedEdit(IdentityService.Actor a,String id,Contracts.ScopedEdit c) {
        a.business();
        var targets=scopeTargets(a,id,c.scope(),true);
        var selected=targets.stream().filter(t->id.equals(t.get("id"))).findFirst().orElseThrow();
        if(!Objects.equals(c.task().expectedVersion(),number(selected,"version"))) checkVersion(0);
        if(selected.get("series_id")==null) return patch(a,id,c.task());
        long shift=java.time.temporal.ChronoUnit.DAYS.between(LocalDate.parse(selected.get("start_date").toString()),c.task().startDate());
        long length=java.time.temporal.ChronoUnit.DAYS.between(c.task().startDate(),c.task().endDate());
        for(var task:targets) {
            scopeVersion(c.versions(),task);
            if(!a.id().equals(task.get("creator_user_id"))) throw BusinessException.missing();
            if(!"APPROVED".equals(task.get("review_status"))) throw BusinessException.policy("重复任务须先完成创建审批。");
            var start=LocalDate.parse(task.get("start_date").toString()).plusDays(shift);
            var proposed = new Contracts.TaskPatch(number(task,"version"), c.task().title(), c.task().description(),
                    c.task().priorityLevel(), c.task().timeKind(), start, start.plusDays(length), c.task().startTime(),
                    c.task().endTime(), c.task().tagIds(), c.task().primaryTagId(), c.task().recipientIds(), c.task().kind(), c.task().companyWide());
            patchContent(a,str(task,"id"),proposed,true);

        }
        return detail(a,id);
    }

    @Transactional
    public Map<String,Object> scopedStatus(IdentityService.Actor a,String id,Contracts.ScopedStatus c) {
        a.business();
        var targets=scopeTargets(a,id,c.scope(),true);
        for(var task:targets) {
            var version=scopeVersion(c.versions(),task);
            var assignment=db.optional("SELECT * FROM task_assignment WHERE enterprise_id=? AND task_id=? AND recipient_user_id=? FOR UPDATE",a.enterpriseId(),task.get("id"),a.id())
                    .orElseThrow(BusinessException::missing);
            if(!Objects.equals(version.assignmentId(),assignment.get("id")) || !Objects.equals(version.assignmentVersion(),number(assignment,"version"))) checkVersion(0);
            if(!"APPROVED".equals(task.get("review_status"))) throw BusinessException.policy("An approved task is required.");
            String state="COMPLETED".equals(assignment.get("execution_status"))?"COMPLETED":str(assignment,"work_status");
            if(c.workStatus().equals(state)) continue;
            if("COMPLETED".equals(assignment.get("execution_status")) && task.get("series_id")!=null && "PERSONAL".equals(task.get("task_kind"))) {
                var old=audit.assignment(a,str(assignment,"id"));
                checkVersion(db.update("UPDATE task_assignment SET execution_status='ACTIVE',work_status=?,progress_percent=0,completion_source=NULL,completed_by_user_id=NULL,completed_at=NULL,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",
                        c.workStatus(),a.enterpriseId(),assignment.get("id"),assignment.get("version")));
                audit.record(a,str(assignment,"id"),"TASK_ASSIGNMENT","TASK","PROGRESS","更新重复任务状态",old,audit.assignment(a,str(assignment,"id")));
            } else setWorkStatus(a,str(assignment,"id"),new Contracts.AssignmentStatus(number(assignment,"version"),c.workStatus(),c.note()));
        }
        return detail(a,id);
    }

    private void active(Map<String, Object> task, Map<String, Object> assignment) {
        if (Boolean.FALSE.equals(assignment.get("current_member"))) throw BusinessException.missing();
        if (!"APPROVED".equals(task.get("review_status"))
                || !"ACTIVE".equals(assignment.get("execution_status")))
            throw new BusinessException(
                    409, "INVALID_WORKFLOW_STATE", "An approved active assignment is required.");
    }

    @Transactional
    public Map<String, Object> progress(IdentityService.Actor a, String id, Contracts.Progress c) {
        a.business();
        var assignment = db.lock("task_assignment", a.enterpriseId(), id);
        if (Boolean.FALSE.equals(assignment.get("current_member"))) throw BusinessException.missing();
        if (!a.id().equals(assignment.get("recipient_user_id"))) throw BusinessException.missing();
        var task = db.entity("task", a.enterpriseId(), str(assignment, "task_id"));
        active(task, assignment);
        var auditBefore=audit.assignment(a,id);
        if(c.progressPercent()==null&&(c.note()==null||c.note().isBlank())) throw BusinessException.invalid("A text note or legacy progress percentage is required.");
        int percent=c.progressPercent()==null?(int)number(assignment,"progress_percent"):c.progressPercent();
        checkVersion(
                db.update(
                        "UPDATE task_assignment SET progress_percent=?,version=version+1 WHERE"
                                + " enterprise_id=? AND id=? AND version=?",
                        percent,
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        String record = id();
        db.update(
                "INSERT INTO"
                    + " task_progress_record(id,enterprise_id,assignment_id,actor_user_id,progress_percent,note)"
                    + " VALUES(?,?,?,?,?,?)",
                record,
                a.enterpriseId(),
                id,
                a.id(),
                percent,
                c.note());
        // Consecutive identical progress is quiet; a later return to that percentage is a new event.
        if (number(assignment, "progress_percent") != percent) notifications.statusChanged(
                a.enterpriseId(),
                str(task, "creator_user_id"),
                str(task, "id"),
                id,
                str(task, "title"),
                "的进度更新为 " + percent + "%",
                record + "|STATUS|PROGRESS");
        var auditAfter=audit.assignment(a,id);
        auditAfter.put("note",c.note());
        audit.record(a,id,"TASK_ASSIGNMENT","TASK","PROGRESS","更新任务进度",auditBefore,auditAfter);
        return fields(
                "record",
                view(db.entity("task_progress_record", a.enterpriseId(), record)),
                "assignment",
                view(db.entity("task_assignment", a.enterpriseId(), id)));
    }

    @Transactional
    public Map<String,Object> setWorkStatus(IdentityService.Actor a,String id,Contracts.AssignmentStatus c) {
        a.business();
        var assignment=db.lock("task_assignment",a.enterpriseId(),id);
        if (Boolean.FALSE.equals(assignment.get("current_member"))) throw BusinessException.missing();
        if(!a.id().equals(assignment.get("recipient_user_id"))) throw BusinessException.missing();
        if(number(assignment,"version")!=c.expectedVersion()) checkVersion(0);
        var task=db.entity("task",a.enterpriseId(),str(assignment,"task_id"));
        active(task,assignment);
        if("COMPLETED".equals(c.workStatus())) {
            if("DELEGATED".equals(task.get("task_kind"))) {
                var result=requestCompletion(a,id,new Contracts.CompletionRequest(c.expectedVersion(),c.note()));
                return fields("assignment",result.get("assignment"),"completionRequest",result.get("request"));
            }
            var completed=complete(a,str(task,"id"),new Contracts.Complete(c.expectedVersion()));
            return fields("assignment",completed,"completionRequest",null);
        }
        var before=audit.assignment(a,id);
        checkVersion(db.update("UPDATE task_assignment SET work_status=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",c.workStatus(),a.enterpriseId(),id,c.expectedVersion()));
        if(c.note()!=null&&!c.note().isBlank()) db.update("INSERT INTO task_progress_record(id,enterprise_id,assignment_id,actor_user_id,progress_percent,note) VALUES(?,?,?,?,?,?)",id(),a.enterpriseId(),id,a.id(),assignment.get("progress_percent"),c.note());
        var after=audit.assignment(a,id);
        after.put("note",c.note());
        audit.record(a,id,"TASK_ASSIGNMENT","TASK","PROGRESS","更新任务工作状态",before,after);
        return fields("assignment",view(db.entity("task_assignment",a.enterpriseId(),id)),"completionRequest",null);
    }

    @Transactional
    public Map<String, Object> complete(
            IdentityService.Actor a, String taskId, Contracts.Complete c) {
        a.business();
        var task = db.entity("task", a.enterpriseId(), taskId);
        if (!a.id().equals(task.get("creator_user_id"))) throw BusinessException.missing();
        if (!"PERSONAL".equals(task.get("task_kind")))
            throw new BusinessException(
                    409,
                    "INVALID_WORKFLOW_STATE",
                    "Delegated assignments require creator acceptance.");
        var assignment =
                db.one(
                        "SELECT * FROM task_assignment WHERE enterprise_id=? AND task_id=? AND"
                                + " recipient_user_id=? FOR UPDATE",
                        a.enterpriseId(),
                        taskId,
                        a.id());
        active(task, assignment);
        var auditBefore=audit.assignment(a,str(assignment,"id"));
        checkVersion(
                db.update(
                        "UPDATE task_assignment SET"
                            + " execution_status='COMPLETED',work_status='COMPLETED',completion_source='SELF',completed_by_user_id=?,completed_at=CURRENT_TIMESTAMP,version=version+1"
                            + " WHERE enterprise_id=? AND id=? AND version=?",
                        a.id(),
                        a.enterpriseId(),
                        assignment.get("id"),
                        c.expectedAssignmentVersion()));
        notifications.statusChanged(
                a.enterpriseId(),
                a.id(),
                taskId,
                str(assignment, "id"),
                str(task, "title"),
                "已由本人标记完成",
                str(assignment, "id") + "|STATUS|COMPLETED");
        audit.record(a,str(assignment,"id"),"TASK_ASSIGNMENT","TASK","UPDATE","本人完成任务",auditBefore,audit.assignment(a,str(assignment,"id")));
        return view(db.entity("task_assignment", a.enterpriseId(), str(assignment, "id")));
    }

    @Transactional
    public Map<String, Object> requestCompletion(
            IdentityService.Actor a, String id, Contracts.CompletionRequest c) {
        a.business();
        var assignment = db.lock("task_assignment", a.enterpriseId(), id);
        if (Boolean.FALSE.equals(assignment.get("current_member"))) throw BusinessException.missing();
        if (!a.id().equals(assignment.get("recipient_user_id"))) throw BusinessException.missing();
        var task = db.entity("task", a.enterpriseId(), str(assignment, "task_id"));
        if (!"DELEGATED".equals(task.get("task_kind")))
            throw new BusinessException(
                    409, "INVALID_WORKFLOW_STATE", "Personal tasks use self completion.");
        active(task, assignment);
        var auditBefore=audit.assignment(a,id);
        checkVersion(
                db.update(
                        "UPDATE task_assignment SET"
                                + " execution_status='AWAITING_ACCEPTANCE',work_status='IN_PROGRESS',version=version+1 WHERE"
                                + " enterprise_id=? AND id=? AND version=?",
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        String request = id();
        db.update(
                "INSERT INTO"
                    + " task_completion_request(id,enterprise_id,assignment_id,requested_by_user_id,submitted_note)"
                    + " VALUES(?,?,?,?,?)",
                request,
                a.enterpriseId(),
                id,
                a.id(),
                c.note());
        notifications.statusChanged(
                a.enterpriseId(),
                str(task, "creator_user_id"),
                str(task, "id"),
                id,
                str(task, "title"),
                "已提交完成申请，等待验收",
                request + "|STATUS|REQUESTED");
        var auditAfter=audit.assignment(a,id);
        auditAfter.put("note",c.note());
        audit.record(a,id,"TASK_ASSIGNMENT","TASK","SUBMIT","提交任务完成申请",auditBefore,auditAfter);
        return fields(
                "request",
                view(db.entity("task_completion_request", a.enterpriseId(), request)),
                "assignment",
                view(db.entity("task_assignment", a.enterpriseId(), id)));
    }

    @Transactional
    public Map<String, Object> decide(IdentityService.Actor a, String id, Contracts.Decision c) {
        a.business();
        var request = db.lock("task_completion_request", a.enterpriseId(), id);
        String assignmentId = str(request, "assignment_id");
        var assignment = db.lock("task_assignment", a.enterpriseId(), assignmentId);
        if (Boolean.FALSE.equals(assignment.get("current_member"))) throw BusinessException.missing();
        var auditBefore=audit.assignment(a,assignmentId);
        var task = db.entity("task", a.enterpriseId(), str(assignment, "task_id"));
        if (!a.id().equals(task.get("creator_user_id"))) throw BusinessException.missing();
        if (!"DELEGATED".equals(task.get("task_kind"))
                || !"APPROVED".equals(task.get("review_status"))
                || !"PENDING".equals(request.get("decision"))
                || !"AWAITING_ACCEPTANCE".equals(assignment.get("execution_status")))
            throw new BusinessException(
                    409,
                    "INVALID_WORKFLOW_STATE",
                    "A pending delegated completion request is required.");
        boolean accept = "ACCEPT".equals(c.decision());
        checkVersion(
                db.update(
                        "UPDATE task_completion_request SET"
                            + " decision=?,decided_by_user_id=?,decision_note=?,decided_at=CURRENT_TIMESTAMP,version=version+1"
                            + " WHERE enterprise_id=? AND id=? AND version=?",
                        accept ? "ACCEPTED" : "REJECTED",
                        a.id(),
                        c.comment(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        if (accept)
            db.update(
                    "UPDATE task_assignment SET"
                        + " execution_status='COMPLETED',work_status='COMPLETED',completion_source='CREATOR_ACCEPTANCE',completed_by_user_id=?,completed_at=CURRENT_TIMESTAMP,version=version+1"
                        + " WHERE enterprise_id=? AND id=?",
                    a.id(),
                    a.enterpriseId(),
                    assignmentId);
        else
            db.update(
                    "UPDATE task_assignment SET execution_status='ACTIVE',work_status='IN_PROGRESS',version=version+1 WHERE"
                            + " enterprise_id=? AND id=?",
                    a.enterpriseId(),
                    assignmentId);
        String statusText = accept ? "的完成申请已验收通过" : "的完成申请被驳回，请继续处理";
        notifications.statusChanged(
                a.enterpriseId(),
                str(assignment, "recipient_user_id"),
                str(task, "id"),
                assignmentId,
                str(task, "title"),
                statusText,
                id + "|STATUS|" + (accept ? "ACCEPTED" : "REJECTED"));
        var auditAfter=audit.assignment(a,assignmentId);
        auditAfter.put("note",c.comment());
        audit.record(a,assignmentId,"TASK_ASSIGNMENT","TASK",accept?"ACCEPT":"REJECT","验收任务完成结果",auditBefore,auditAfter);
        return fields(
                "request",
                view(db.entity("task_completion_request", a.enterpriseId(), id)),
                "assignment",
                view(db.entity("task_assignment", a.enterpriseId(), assignmentId)));
    }

    public Map<String, Object> calendar(
            IdentityService.Actor a,
            String mode,
            LocalDate anchor,
            List<String> tagIds,
            String cursor,
            int limit) {
        a.business();
        LocalDate start, end;
        switch (mode) {
            case "day" -> {
                start = anchor;
                end = anchor;
            }
            case "week" -> {
                start = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end = start.plusDays(6);
            }
            case "month" -> {
                start =
                        anchor.withDayOfMonth(1)
                                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                end =
                        anchor.with(TemporalAdjusters.lastDayOfMonth())
                                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            }
            default -> throw BusinessException.invalid("Calendar mode must be day, week or month.");
        }
        var query = query(a, start, end, "mine", tagIds, true, null);
        var page =
                queryPage(
                        a,
                        query,
                        cursor,
                        limit,
                        a.enterpriseId()
                                + ":"
                                + a.id()
                                + ":calendar:"
                                + mode
                                + ":"
                                + anchor
                                + ":"
                                + new TreeSet<>(tagIds == null ? List.of() : tagIds));
        long total =
                number(
                        db.one(
                                "SELECT COUNT(*) AS total FROM (" + query.sql() + ") q",
                                query.args().toArray()),
                        "total");
        return fields(
                "mode",
                mode,
                "anchorDate",
                anchor.toString(),
                "rangeStart",
                start.toString(),
                "rangeEnd",
                end.toString(),
                "today",
                identity.today(a).toString(),
                "businessTimezone",
                identity.zone(a).getId(),
                "tasks",
                page.get("items"),
                "nextCursor",
                page.get("nextCursor"),
                "totalCount",
                total);
    }
}
