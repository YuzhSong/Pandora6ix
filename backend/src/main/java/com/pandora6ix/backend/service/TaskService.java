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
    private final BusinessDb db;
    private final IdentityService identity;
    private final ScopeService scope;
    private final TagService tags;
    private final JsonMapper json;

    public TaskService(
            BusinessDb db,
            IdentityService identity,
            ScopeService scope,
            TagService tags,
            JsonMapper json) {
        this.db = db;
        this.identity = identity;
        this.scope = scope;
        this.tags = tags;
        this.json = json;
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
        if (!start.equals(end) || to <= from)
            throw BusinessException.policy(
                    "Cross-day and non-increasing TIMED endpoints await Q-04 confirmation.");
        return new int[] {from, to};
    }

    @Transactional
    public Map<String, Object> create(IdentityService.Actor a, Contracts.TaskCreate c) {
        a.business();
        identity.zone(a);
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
            var managed = scope.activeManaged(a);
            if (recipients.isEmpty())
                throw BusinessException.invalid("A delegated task requires recipients.");
            if (!managed.containsAll(recipients))
                throw new BusinessException(
                        403,
                        "RECIPIENT_OUT_OF_SCOPE",
                        "All recipients must be authorized subordinate business users.");
        }
        var user = db.entity("app_user", a.enterpriseId(), a.id());
        String supervisor = str(user, "supervisor_user_id");
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
        tags.replace(a, "task_tag", "task_id", task, tagIds);
        for (String recipient : recipients)
            db.update(
                    "INSERT INTO task_assignment(id,enterprise_id,task_id,recipient_user_id)"
                            + " VALUES(?,?,?,?)",
                    id(),
                    a.enterpriseId(),
                    task,
                    recipient);
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
        var result = detail(a, task);
        result.put("taskId", task);
        result.put("reviewId", review);
        return result;
    }

    private List<Map<String, Object>> assignments(IdentityService.Actor a, String task) {
        return db.rows(
                "SELECT * FROM task_assignment WHERE enterprise_id=? AND task_id=? ORDER BY id",
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
                                    return assignment;
                                })
                        .toList();
        v.put("authorizedAssignments", allowed);
        Set<String> actions = new LinkedHashSet<>();
        if (a.id().equals(task.get("creator_user_id"))
                && "PENDING".equals(task.get("review_status"))) actions.add("EDIT_PENDING");
        for (var r : assignments) actions.addAll(assignmentActions(a, task, r));
        v.put("allowedActions", List.copyOf(actions));
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
        if (!"APPROVED".equals(task.get("review_status"))) return List.of();
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
                                        + " x.enterprise_id=t.enterprise_id AND x.task_id=t.id AND"
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
                                + " x WHERE x.enterprise_id=t.enterprise_id AND x.task_id=t.id AND"
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
        var query = query(a, null, null, "mine", List.of(), true, "DELEGATED");
        return queryPage(
                a, query, cursor, limit, a.enterpriseId() + ":" + a.id() + ":dashboard-tasks");
    }

    @Transactional
    public Map<String, Object> patch(IdentityService.Actor a, String id, Contracts.TaskPatch c) {
        a.business();
        var task = db.lock("task", a.enterpriseId(), id);
        if (!a.id().equals(task.get("creator_user_id"))) throw BusinessException.missing();
        if (!"PENDING".equals(task.get("review_status")))
            throw BusinessException.policy(
                    "Editing approved/rejected tasks and resubmission await Q-02 confirmation.");
        if ("DELEGATED".equals(task.get("task_kind"))) {
            var managed = scope.activeManaged(a);
            if (assignments(a, id).stream()
                    .anyMatch(row -> !managed.contains(str(row, "recipient_user_id"))))
                throw new BusinessException(
                        403,
                        "RECIPIENT_OUT_OF_SCOPE",
                        "All recipients must remain authorized active subordinate users.");
        }
        String reviewerId =
                str(db.entity("app_user", a.enterpriseId(), a.id()), "supervisor_user_id");
        if (reviewerId == null || reviewerId.equals(a.id()))
            throw BusinessException.policy("A direct-supervisor approval policy is required.");
        var reviewer = db.entity("app_user", a.enterpriseId(), reviewerId);
        if (!"ACTIVE".equals(reviewer.get("status"))
                || identity.actor(reviewerId, a.enterpriseId(), str(reviewer, "org_unit_id"), null)
                                .businessRank()
                        >= 3)
            throw BusinessException.policy("An active business reviewer must be configured.");
        var minutes = time(c.timeKind(), c.startDate(), c.endDate(), c.startTime(), c.endTime());
        var tagIds = tags.validate(a, c.tagIds(), c.primaryTagId());
        checkVersion(
                db.update(
                        "UPDATE task SET"
                            + " title=?,description=?,priority_level=?,start_date=?,end_date=?,time_kind=?,start_minute=?,end_minute=?,primary_tag_id=?,version=version+1"
                            + " WHERE enterprise_id=? AND id=? AND version=?",
                        c.title(),
                        c.description(),
                        c.priorityLevel(),
                        c.startDate(),
                        c.endDate(),
                        c.timeKind().name(),
                        minutes == null ? null : minutes[0],
                        minutes == null ? null : minutes[1],
                        c.primaryTagId(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        tags.replace(a, "task_tag", "task_id", id, tagIds);
        // Keep the same submitted command shape as creation, without mutable read/action metadata.
        // Earlier snapshots remain immutable and can no longer approve this content version.
        var snapshot =
                fields(
                        "kind",
                        task.get("task_kind"),
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
                        assignments(a, id).stream().map(r -> r.get("recipient_user_id")).toList(),
                        "tagIds",
                        tagIds,
                        "primaryTagId",
                        c.primaryTagId());
        var previous =
                db.one(
                        "SELECT * FROM task_review WHERE enterprise_id=? AND task_id=? ORDER BY"
                                + " attempt_no DESC LIMIT 1",
                        a.enterpriseId(),
                        id);
        db.update(
                "INSERT INTO"
                    + " task_review(id,enterprise_id,task_id,reviewer_user_id,submitted_by_user_id,task_version,submitted_snapshot,attempt_no)"
                    + " VALUES(?,?,?,?,?,?,?,?)",
                id(),
                a.enterpriseId(),
                id,
                reviewerId,
                a.id(),
                c.expectedVersion() + 1,
                json.writeValueAsString(snapshot),
                number(previous, "attempt_no") + 1);
        return detail(a, id);
    }

    private void active(Map<String, Object> task, Map<String, Object> assignment) {
        if (!"APPROVED".equals(task.get("review_status"))
                || !"ACTIVE".equals(assignment.get("execution_status")))
            throw new BusinessException(
                    409, "INVALID_WORKFLOW_STATE", "An approved active assignment is required.");
    }

    @Transactional
    public Map<String, Object> progress(IdentityService.Actor a, String id, Contracts.Progress c) {
        a.business();
        var assignment = db.lock("task_assignment", a.enterpriseId(), id);
        if (!a.id().equals(assignment.get("recipient_user_id"))) throw BusinessException.missing();
        var task = db.entity("task", a.enterpriseId(), str(assignment, "task_id"));
        active(task, assignment);
        checkVersion(
                db.update(
                        "UPDATE task_assignment SET progress_percent=?,version=version+1 WHERE"
                                + " enterprise_id=? AND id=? AND version=?",
                        c.progressPercent(),
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
                c.progressPercent(),
                c.note());
        return fields(
                "record",
                view(db.entity("task_progress_record", a.enterpriseId(), record)),
                "assignment",
                view(db.entity("task_assignment", a.enterpriseId(), id)));
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
        checkVersion(
                db.update(
                        "UPDATE task_assignment SET"
                            + " execution_status='COMPLETED',completion_source='SELF',completed_by_user_id=?,completed_at=CURRENT_TIMESTAMP,version=version+1"
                            + " WHERE enterprise_id=? AND id=? AND version=?",
                        a.id(),
                        a.enterpriseId(),
                        assignment.get("id"),
                        c.expectedAssignmentVersion()));
        return view(db.entity("task_assignment", a.enterpriseId(), str(assignment, "id")));
    }

    @Transactional
    public Map<String, Object> requestCompletion(
            IdentityService.Actor a, String id, Contracts.CompletionRequest c) {
        a.business();
        var assignment = db.lock("task_assignment", a.enterpriseId(), id);
        if (!a.id().equals(assignment.get("recipient_user_id"))) throw BusinessException.missing();
        var task = db.entity("task", a.enterpriseId(), str(assignment, "task_id"));
        if (!"DELEGATED".equals(task.get("task_kind")))
            throw new BusinessException(
                    409, "INVALID_WORKFLOW_STATE", "Personal tasks use self completion.");
        active(task, assignment);
        checkVersion(
                db.update(
                        "UPDATE task_assignment SET"
                                + " execution_status='AWAITING_ACCEPTANCE',version=version+1 WHERE"
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
                        + " execution_status='COMPLETED',completion_source='CREATOR_ACCEPTANCE',completed_by_user_id=?,completed_at=CURRENT_TIMESTAMP,version=version+1"
                        + " WHERE enterprise_id=? AND id=?",
                    a.id(),
                    a.enterpriseId(),
                    assignmentId);
        else
            db.update(
                    "UPDATE task_assignment SET execution_status='ACTIVE',version=version+1 WHERE"
                            + " enterprise_id=? AND id=?",
                    a.enterpriseId(),
                    assignmentId);
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
