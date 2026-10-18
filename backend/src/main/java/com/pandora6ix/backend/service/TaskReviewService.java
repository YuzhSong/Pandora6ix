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
    private final BusinessDb db;
    private final IdentityService identity;
    private final ScopeService scope;
    private final TaskService tasks;
    private final JsonMapper json;
    private final NotificationService notifications;

    public TaskReviewService(
            BusinessDb db,
            IdentityService identity,
            ScopeService scope,
            TaskService tasks,
            JsonMapper json,
            NotificationService notifications) {
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
                && "PENDING".equals(task.get("review_status"))
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
        var assignments =
                db.rows(
                        "SELECT x.*,u.status AS user_status FROM task_assignment x JOIN app_user u"
                                + " ON u.enterprise_id=x.enterprise_id AND u.id=x.recipient_user_id"
                                + " WHERE x.enterprise_id=? AND x.task_id=?",
                        a.enterpriseId(),
                        task.get("id"));
        if (assignments.isEmpty()
                || assignments.stream()
                        .anyMatch(
                                x ->
                                        !"ACTIVE".equals(x.get("user_status"))
                                                || !"PENDING_APPROVAL"
                                                        .equals(x.get("execution_status"))))
            return false;
        if ("PERSONAL".equals(task.get("task_kind")))
            return assignments.size() == 1
                    && creator.id().equals(assignments.getFirst().get("recipient_user_id"));
        if (creator.businessRank() >= 3) return false;
        var managed = scope.activeManaged(creator);
        return assignments.stream().allMatch(x -> managed.contains(str(x, "recipient_user_id")));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> reviewView(IdentityService.Actor a, Map<String, Object> review) {
        var task = db.entity("task", a.enterpriseId(), str(review, "task_id"));
        var v = view(review);
        v.put("reviewId", v.get("id"));
        v.put("submittedSnapshot", json.readValue(str(review, "submitted_snapshot"), Map.class));
        v.put("currentTaskVersion", task.get("version"));
        v.put("reviewStatus", task.get("review_status"));
        v.put("isCurrent", current(review));
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
        if (!Set.of("pending", "created").contains(requestedScope))
            throw BusinessException.invalid("Review scope must be pending or created.");
        String query =
                "SELECT r.* FROM task_review r JOIN task t ON t.enterprise_id=r.enterprise_id AND"
                        + " t.id=r.task_id WHERE r.enterprise_id=?";
        if ("pending".equals(requestedScope)) {
            a.manager();
            query +=
                    " AND r.reviewer_user_id=? AND r.decision='PENDING' AND"
                            + " t.review_status='PENDING' AND r.task_version=t.version AND"
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
                row -> reviewView(a, row));
    }

    public Map<String, Object> detail(IdentityService.Actor a, String id) {
        var review = db.entity("task_review", a.enterpriseId(), id);
        authorize(a, review);
        return reviewView(a, review);
    }

    @Transactional
    public Map<String, Object> decide(
            IdentityService.Actor a, String id, Contracts.ReviewDecision c) {
        var before = db.entity("task_review", a.enterpriseId(), id);
        authorize(a, before);
        if (!a.id().equals(before.get("reviewer_user_id")) || a.businessRank() >= 3)
            throw BusinessException.missing();
        // Task edits use the same first lock, preventing an old snapshot from winning a race.
        var task = db.lock("task", a.enterpriseId(), str(before, "task_id"));
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
        checkVersion(
                db.update(
                        "UPDATE task SET review_status=?,version=version+1"
                                + " WHERE enterprise_id=? AND id=? AND version=?",
                        approve ? "APPROVED" : "REJECTED",
                        a.enterpriseId(),
                        task.get("id"),
                        review.get("task_version")));
        if (approve) {
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
        return fields("review", detail(a, id), "task", tasks.detail(a, str(task, "id")));
    }
}
