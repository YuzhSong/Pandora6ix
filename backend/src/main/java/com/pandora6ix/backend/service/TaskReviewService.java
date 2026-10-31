package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.api.BusinessException.checkVersion;
import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.json.JsonMapper;

import java.util.*;

@Service
public class TaskReviewService {
    private final MutationAudit audit;
    private final BusinessDb db;
    private final IdentityService identity;
    private final ScopeService scope;
    private final TaskService tasks;
    private final JsonMapper json;
    private final NotificationService notifications;

    public TaskReviewService(
            BusinessDb db,
            MutationAudit audit,
            IdentityService identity,
            ScopeService scope,
            TaskService tasks,
            JsonMapper json,
            NotificationService notifications) {
        this.audit = audit;
        this.db = db;
        this.identity = identity;
        this.scope = scope;
        this.tasks = tasks;
        this.json = json;
        this.notifications = notifications;
    }

    private void authorize(IdentityService.Actor a, Map<String, Object> review) {
        if (a.businessRank() == 99
                || (!a.id().equals(review.get("reviewer_user_id"))
                        && !a.id().equals(review.get("submitted_by_user_id"))))
            throw BusinessException.missing();
    }

    private boolean current(Map<String, Object> review) {
        return number(review, "attempt_no")
                == number(
                        db.one(
                                "SELECT MAX(attempt_no) AS latest FROM task_review WHERE"
                                        + " enterprise_id=? AND task_id=?",
                                review.get("enterprise_id"),
                                review.get("task_id")),
                        "latest");
    }

    private boolean pending(Map<String, Object> review, Map<String, Object> task) {
        return "PENDING".equals(review.get("decision"))
                && ("EDIT".equals(review.get("review_kind")) ? "APPROVED".equals(task.get("review_status")) : "PENDING".equals(task.get("review_status")))
                && number(review, "task_version") == number(task, "version")
                && current(review);
    }

    private boolean validContext(
            IdentityService.Actor a, Map<String, Object> review, Map<String, Object> task) {
        if (a.businessRank() >= 3 || a.id().equals(task.get("creator_user_id"))) return false;
        var reviewerUser = db.entity("app_user", a.enterpriseId(), a.id());
        var reviewer =
                identity.actor(a.id(), a.enterpriseId(), str(reviewerUser, "org_unit_id"), null);
        if (!"ACTIVE".equals(reviewerUser.get("status")) || reviewer.businessRank() >= 3)
            return false;
        var submitter = db.entity("app_user", a.enterpriseId(), str(task, "creator_user_id"));
        if (!"ACTIVE".equals(submitter.get("status"))
                || !a.id().equals(submitter.get("supervisor_user_id"))
                || !a.id().equals(review.get("reviewer_user_id"))
                || !scope.managed(reviewer).contains(str(submitter, "id"))) return false;
        var creator =
                identity.actor(
                        str(submitter, "id"),
                        a.enterpriseId(),
                        str(submitter, "org_unit_id"),
                        null);
        if ("EDIT".equals(review.get("review_kind"))) {
            @SuppressWarnings("unchecked") var snapshot=(Map<String,Object>)json.readValue(str(review,"submitted_snapshot"),Map.class);
            try {
                var proposed=new HashMap<String,Object>(task);
                if(snapshot.get("kind")!=null) proposed.put("task_kind",String.valueOf(snapshot.get("kind")));
                tasks.validateRecipients(creator,proposed,snapshotRecipientIds(snapshot));
                return true;
            } catch (BusinessException invalid) { return false; }
        }
        var assignments =
                db.rows(
                        "SELECT x.*,u.status AS user_status FROM task_assignment x JOIN app_user u"
                                + " ON u.enterprise_id=x.enterprise_id AND u.id=x.recipient_user_id"
                                + " WHERE x.enterprise_id=? AND x.task_id=? AND x.current_member=TRUE",
                        a.enterpriseId(),
                        task.get("id"));
        if (assignments.isEmpty()
                || assignments.stream()
                        .anyMatch(
                                x ->
                                        !"ACTIVE".equals(x.get("user_status"))
                                                || (!"EDIT".equals(review.get("review_kind")) && !"PENDING_APPROVAL".equals(x.get("execution_status")))))
            return false;
        if ("PERSONAL".equals(task.get("task_kind")))
            return assignments.size() == 1
                    && creator.id().equals(assignments.getFirst().get("recipient_user_id"));
        if (creator.businessRank() >= 3) return false;
        var managed = scope.taskRecipients(creator, assignments.stream()
                .map(row -> str(row, "recipient_user_id")).toList());
        return assignments.stream().allMatch(x -> managed.contains(str(x, "recipient_user_id")));
    }

    /** 本企业 userId → 人员档案（展示名 + 职位）：一次查全量，避免逐条审批再查用户表 */
    private Map<String, Map<String, Object>> userIndex(IdentityService.Actor a) {
        Map<String, Map<String, Object>> users = new HashMap<>();
        for (var row :
                db.rows(
                        "SELECT id,display_name,job_title FROM app_user WHERE enterprise_id=?",
                        a.enterpriseId()))
            users.put(str(row, "id"), row);
        return users;
    }

    private static String text(Map<String, Object> row, String column) {
        return row == null ? null : str(row, column);
    }

    /** 快照里申请的执行人 ID：审批详情展示的是不可变快照，执行人必须取自提交当时 */
    private static List<String> snapshotRecipientIds(Map<String, Object> snapshot) {
        var ids = snapshot == null ? null : snapshot.get("recipientIds");
        if (!(ids instanceof List<?> list)) return List.of();
        return list.stream().filter(String.class::isInstance).map(String.class::cast).toList();
    }

    /**
     * 执行人（展示名 + 职位）：取自本条申请提交的快照，快照缺失（历史数据）时回退任务当前执行关系。
     * 不用当前执行关系代替快照：任务被改派后，旧审批详情显示当时的执行人才与快照内容一致。
     */
    private List<Map<String, Object>> recipients(
            IdentityService.Actor a, String taskId, Map<String, Map<String, Object>> users, Map<String, Object> snapshot) {
        List<String> ids = snapshotRecipientIds(snapshot);
        if (ids.isEmpty())
            ids =
                    db.rows(
                                    "SELECT recipient_user_id FROM task_assignment WHERE"
                                            + " enterprise_id=? AND task_id=?",
                                    a.enterpriseId(),
                                    taskId)
                            .stream()
                            .map(row -> str(row, "recipient_user_id"))
                            .toList();
        List<Map<String, Object>> out = new ArrayList<>();
        for (String id : ids) {
            var u = users.get(id);
            out.add(
                    fields(
                            "id", id,
                            "displayName", text(u, "display_name"),
                            "jobTitle", text(u, "job_title")));
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> reviewView(
            IdentityService.Actor a, Map<String, Object> review, Map<String, Map<String, Object>> users) {
        var task = db.entity("task", a.enterpriseId(), str(review, "task_id"));
        var v = view(review);
        v.put("reviewId", v.get("id"));
        var snapshot = (Map<String, Object>) json.readValue(str(review, "submitted_snapshot"), Map.class);
        v.put("submittedSnapshot", snapshot);
        v.put("currentTaskVersion", task.get("version"));
        v.put("effectiveTaskReviewStatus", task.get("review_status"));
        v.put("reviewStatus", "EDIT".equals(review.get("review_kind"))&&"PENDING".equals(review.get("decision"))?"PENDING":task.get("review_status"));
        v.put("isCurrent", current(review));
        // 审批详情要显示"谁申请的、谁执行"，只有 ID 无法呈现，故附带展示名与职位
        var submitter = users.get(str(review, "submitted_by_user_id"));
        var reviewer = users.get(str(review, "reviewer_user_id"));
        v.put("submittedByName", text(submitter, "display_name"));
        v.put("submittedByJobTitle", text(submitter, "job_title"));
        v.put("reviewerName", text(reviewer, "display_name"));
        v.put("reviewerJobTitle", text(reviewer, "job_title"));
        v.put("recipients", recipients(a, str(review, "task_id"), users, snapshot));
        boolean actionable =
                pending(review, task)
                        && a.id().equals(review.get("reviewer_user_id"))
                        && validContext(a, review, task);
        v.put("allowedActions", actionable ? List.of("APPROVE", "REJECT") : List.of());
        return v;
    }

    public Map<String, Object> list(
            IdentityService.Actor a, String requestedScope, String cursor, int limit) {
        a.business();
        var users = userIndex(a);
        if (!Set.of("pending", "created").contains(requestedScope))
            throw BusinessException.invalid("Review scope must be pending or created.");
        String query =
                "SELECT r.* FROM task_review r JOIN task t ON t.enterprise_id=r.enterprise_id AND"
                        + " t.id=r.task_id WHERE r.enterprise_id=?";
        if ("pending".equals(requestedScope)) {
            a.manager();
            query +=
                    " AND r.reviewer_user_id=? AND r.decision='PENDING' AND"
                            + " ((r.review_kind='CREATE' AND t.review_status='PENDING') OR (r.review_kind='EDIT' AND t.review_status='APPROVED')) AND r.task_version=t.version AND"
                            + " r.attempt_no=(SELECT MAX(x.attempt_no) FROM task_review x WHERE"
                            + " x.enterprise_id=r.enterprise_id AND x.task_id=r.task_id)";
        } else query += " AND r.submitted_by_user_id=?";
        return Pages.sqlPage(
                db,
                query,
                List.of(a.enterpriseId(), a.id()),
                cursor,
                limit,
                a.enterpriseId() + ":" + a.id() + ":reviews:" + requestedScope,
                List.of("submitted_at", "id"),
                true,
                row -> reviewView(a, row, users));
    }

    public Map<String, Object> pendingCount(IdentityService.Actor a) {
        a.manager();
        var result = db.one(
                "SELECT COUNT(*) AS review_count FROM task_review r JOIN task t"
                        + " ON t.enterprise_id=r.enterprise_id AND t.id=r.task_id"
                        + " WHERE r.enterprise_id=? AND r.reviewer_user_id=?"
                        + " AND r.decision='PENDING' AND ((r.review_kind='CREATE' AND t.review_status='PENDING') OR (r.review_kind='EDIT' AND t.review_status='APPROVED'))"
                        + " AND r.task_version=t.version"
                        + " AND r.attempt_no=(SELECT MAX(x.attempt_no) FROM task_review x"
                        + " WHERE x.enterprise_id=r.enterprise_id AND x.task_id=r.task_id)",
                a.enterpriseId(), a.id());
        return fields("count", number(result, "review_count"));
    }

    public Map<String, Object> detail(IdentityService.Actor a, String id) {
        var review = db.entity("task_review", a.enterpriseId(), id);
        authorize(a, review);
        return reviewView(a, review, userIndex(a));
    }

    @Transactional
    public Map<String, Object> decide(
            IdentityService.Actor a, String id, Contracts.ReviewDecision c) {
        var before = db.entity("task_review", a.enterpriseId(), id);
        authorize(a, before);
        if (!a.id().equals(before.get("reviewer_user_id")) || a.businessRank() >= 3)
            throw BusinessException.missing();
        // Task edits use the same first lock, preventing an old snapshot from winning a race.
        var previewTask = db.entity("task", a.enterpriseId(), str(before, "task_id"));
        if (previewTask.get("series_id") != null && "CREATE".equals(before.get("review_kind"))) {
            return decideSeries(a, before, c);
        }
        var task = db.lock("task", a.enterpriseId(), str(before, "task_id"));
        var auditBefore=audit.task(a,str(task,"id"));
        var review = db.lock("task_review", a.enterpriseId(), id);
        if (number(review, "version") != c.expectedVersion()) checkVersion(0);
        if (!pending(review, task))
            throw new BusinessException(
                    409, "REVIEW_NOT_PENDING", "Read the current pending review before deciding.");
        if (!validContext(a, review, task))
            throw new BusinessException(
                    409,
                    "REVIEW_CONTEXT_CHANGED",
                    "The direct supervisor, business roles or authorized active recipients"
                            + " changed.");
        boolean approve = "APPROVE".equals(c.decision());
        checkVersion(
                db.update(
                        "UPDATE task_review SET"
                            + " decision=?,decided_by_user_id=?,decision_note=?,decided_at=CURRENT_TIMESTAMP,version=version+1"
                            + " WHERE enterprise_id=? AND id=? AND version=?",
                        approve ? "APPROVED" : "REJECTED",
                        a.id(),
                        c.comment(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        boolean edit="EDIT".equals(review.get("review_kind"));
        if(edit&&approve) {
            @SuppressWarnings("unchecked") var content=(Map<String,Object>)json.readValue(str(review,"submitted_snapshot"),Map.class);
            tasks.applyContent(a,str(task,"id"),content,number(review,"task_version"));
        } else if(edit) {
            checkVersion(db.update("UPDATE task SET version=version+1 WHERE enterprise_id=? AND id=? AND version=?",a.enterpriseId(),task.get("id"),review.get("task_version")));
        } else {
            checkVersion(db.update("UPDATE task SET review_status=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",approve?"APPROVED":"REJECTED",a.enterpriseId(),task.get("id"),review.get("task_version")));
        }
        notifications.reviewDecided(a.enterpriseId(), id, str(task, "id"),
                str(review, "submitted_by_user_id"), approve ? "APPROVED" : "REJECTED");
        if (approve && !edit) {
            db.update(
                    "UPDATE task_assignment SET"
                        + " execution_status='ACTIVE',activated_at=CURRENT_TIMESTAMP,version=version+1"
                        + " WHERE enterprise_id=? AND task_id=? AND"
                        + " execution_status='PENDING_APPROVAL'",
                    a.enterpriseId(),
                    task.get("id"));
            // 提醒与激活在同一事务：任务未生效则不产生新任务提醒
            notifications.taskApproved(a.enterpriseId(), str(task, "id"), str(task, "title"));
        }
        audit.record(a,str(task,"id"),"TASK","TASK",approve?"APPROVE":"REJECT","审批任务",auditBefore,audit.task(a,str(task,"id")));
        return fields("review", detail(a, id), "task", tasks.detail(a, str(task, "id")));
    }

    private Map<String,Object> decideSeries(IdentityService.Actor a,Map<String,Object> before,Contracts.ReviewDecision c) {
        var head = db.entity("task",a.enterpriseId(),str(before,"task_id"));
        var group = db.rows("SELECT * FROM task WHERE enterprise_id=? AND series_id=? ORDER BY id FOR UPDATE",a.enterpriseId(),head.get("series_id"));
        var review = db.lock("task_review",a.enterpriseId(),str(before,"id"));
        if(number(review,"version")!=c.expectedVersion()) checkVersion(0);
        for(var task:group) {
            if(!"PENDING".equals(task.get("review_status")) || !validContext(a,review,task))
                throw new BusinessException(409,"REVIEW_CONTEXT_CHANGED","The series approval context changed.");
        }
        if(!pending(review,head)) throw new BusinessException(409,"REVIEW_NOT_PENDING","Review is no longer pending.");
        boolean approve="APPROVE".equals(c.decision());
        for(var task:group) {
            var old=audit.task(a,str(task,"id"));
            checkVersion(db.update("UPDATE task SET review_status=?,version=version+1 WHERE enterprise_id=? AND id=? AND version=?",
                    approve?"APPROVED":"REJECTED",a.enterpriseId(),task.get("id"),task.get("version")));
            if(approve) {
                db.update("UPDATE task_assignment SET execution_status='ACTIVE',activated_at=CURRENT_TIMESTAMP,version=version+1 WHERE enterprise_id=? AND task_id=? AND execution_status='PENDING_APPROVAL'",a.enterpriseId(),task.get("id"));
                notifications.taskApproved(a.enterpriseId(),str(task,"id"),str(task,"title"));
            }
            db.update("UPDATE task_review SET decision=?,decided_by_user_id=?,decision_note=?,decided_at=CURRENT_TIMESTAMP,version=version+1 WHERE enterprise_id=? AND task_id=? AND review_kind='CREATE' AND decision='PENDING'",
                    approve?"APPROVED":"REJECTED",a.id(),c.comment(),a.enterpriseId(),task.get("id"));
            audit.record(a,str(task,"id"),"TASK","TASK",approve?"APPROVE":"REJECT","审批重复任务",old,audit.task(a,str(task,"id")));
        }
        notifications.reviewDecided(a.enterpriseId(),str(review,"id"),str(head,"id"),str(review,"submitted_by_user_id"),approve?"APPROVED":"REJECTED");
        return fields("review",detail(a,str(review,"id")),"task",tasks.detail(a,str(head,"id")));
    }
}
