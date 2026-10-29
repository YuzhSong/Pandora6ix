package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.BusinessException;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 提醒记录：任务写路径埋点生成新任务/状态通知，定时扫描生成截止时间通知。
 *
 * <p>去重由 (enterprise_id,user_id,dedupe_key) 唯一索引与写入前的存在性检查共同保证：重复触发只
 * 保留一条，不影响外层任务事务。截止时间只按服务端最大档位（24 小时）生成一次并附带 dueAt，客户端
 * 按自己的提前量阈值决定是否展示，避免任意阈值导致去重键爆炸。
 */
@Service
public class NotificationService {

    public static final String NEW_TASK = "NEW_TASK";
    public static final String DEADLINE = "DEADLINE";
    public static final String STATUS = "STATUS";

    /** 服务端生成截止提醒的最大提前量（小时） */
    public static final int DEADLINE_LEAD_HOURS = 24;

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);

    private final BusinessDb db;

    public NotificationService(BusinessDb db) {
        this.db = db;
    }

    /** 去重写入一条提醒；同一用户同一 dedupeKey 只保留第一条 */
    @Transactional
    public void emit(
            String enterpriseId,
            String userId,
            String kind,
            String taskId,
            String assignmentId,
            String title,
            String body,
            String dedupeKey,
            Instant dueAt) {
        emit(enterpriseId, userId, kind, taskId, assignmentId, title, body, dedupeKey, dueAt, kind);
    }

    @Transactional
    private void emit(
            String enterpriseId,
            String userId,
            String kind,
            String taskId,
            String assignmentId,
            String title,
            String body,
            String dedupeKey,
            Instant dueAt,
            String eventType) {
        if (userId == null || enterpriseId == null) return;
        var existing =
                db.optional(
                        "SELECT id FROM notification WHERE enterprise_id=? AND user_id=? AND"
                                + " dedupe_key=?",
                        enterpriseId,
                        userId,
                        dedupeKey);
        if (existing.isPresent()) return;
        String notificationId = id();
        try {
            db.update(
                "INSERT INTO"
                    + " notification(id,enterprise_id,user_id,kind,event_type,task_id,assignment_id,title,body,dedupe_key,due_at)"
                    + " VALUES(?,?,?,?,?,?,?,?,?,?,?)",
                notificationId,
                enterpriseId,
                userId,
                kind,
                eventType,
                taskId,
                assignmentId,
                title,
                body,
                dedupeKey,
                dueAt == null ? null : Timestamp.from(dueAt));
        } catch (DuplicateKeyException ex) {
            // A competing scan may insert after the existence check. Only ignore that event.
            if (count(enterpriseId, dedupeKey, userId) == 0) throw ex;
            return;
        }
        for (var device : db.rows("SELECT id FROM push_device WHERE enterprise_id=? AND user_id=? AND active=TRUE",
                enterpriseId, userId))
            db.update("INSERT INTO notification_push_delivery(id,enterprise_id,notification_id,push_device_id) VALUES(?,?,?,?)",
                    id(), enterpriseId, notificationId, str(device, "id"));
    }

    public void reviewRequested(String enterpriseId, String reviewId, String taskId, String submitterId,
                                String reviewerId, String taskTitle) {
        // 正文带上提交人姓名：审批人一眼看到是谁申请审批（查不到姓名时退回原文案）
        String submitter = displayName(enterpriseId, submitterId);
        emit(enterpriseId, reviewerId, NEW_TASK, taskId, null, "审批申请",
                (submitter == null || submitter.isBlank() ? "" : submitter) + "有一项任务等待你审批。打开应用查看详情。",
                reviewId + "|REVIEW_REQUEST|" + reviewerId, null, "REVIEW_REQUEST");
    }

    private String displayName(String enterpriseId, String userId) {
        if (enterpriseId == null || userId == null) return null;
        return db.optional(
                        "SELECT display_name FROM app_user WHERE enterprise_id=? AND id=?",
                        enterpriseId,
                        userId)
                .map(row -> str(row, "display_name"))
                .orElse(null);
    }

    public void reviewDecided(String enterpriseId, String reviewId, String taskId, String submitterId,
                              String decision) {
        String text = "APPROVED".equals(decision) ? "任务审批已通过。打开应用查看详情。" : "任务审批未通过。打开应用查看详情。";
        emit(enterpriseId, submitterId, STATUS, taskId, null, "审批结果", text,
                reviewId + "|REVIEW_RESULT|" + submitterId, null, "REVIEW_RESULT");
    }

    /** 审批批准并激活执行关系：为每位被激活的接收人生成新任务提醒 */
    public void taskApproved(String enterpriseId, String taskId, String title) {
        var assignments =
                db.rows(
                        "SELECT id,recipient_user_id FROM task_assignment WHERE enterprise_id=? AND"
                                + " task_id=? AND execution_status='ACTIVE'",
                        enterpriseId,
                        taskId);
        for (var row : assignments) {
            String assignmentId = str(row, "id");
            String recipient = str(row, "recipient_user_id");
            emit(
                    enterpriseId,
                    recipient,
                    NEW_TASK,
                    taskId,
                    assignmentId,
                    "新任务提醒",
                    "《" + title + "》已批准生效，请及时处理。",
                    taskId + "|" + assignmentId + "|" + NEW_TASK,
                    null);
        }
    }

    /** 执行状态变化：通知创建人与执行人；个人任务只产生一条。 */
    public void statusChanged(
            String enterpriseId,
            String userId,
            String taskId,
            String assignmentId,
            String title,
            String statusText,
            String dedupeKey) {
        var recipients = new LinkedHashSet<String>();
        recipients.add(userId);
        recipients.add(str(db.entity("task", enterpriseId, taskId), "creator_user_id"));
        recipients.add(str(db.entity("task_assignment", enterpriseId, assignmentId), "recipient_user_id"));
        for (String recipient : recipients) emit(
                enterpriseId,
                recipient,
                STATUS,
                taskId,
                assignmentId,
                "任务状态变化",
                "《" + title + "》" + statusText + "。",
                dedupeKey,
                null);
    }

    public Map<String, Object> list(IdentityService.Actor a, String cursor, int limit) {
        a.business();
        return Pages.sqlPage(
                db,
                "SELECT * FROM notification WHERE enterprise_id=? AND user_id=?",
                List.of(a.enterpriseId(), a.id()),
                cursor,
                limit,
                a.enterpriseId() + ":" + a.id() + ":notifications",
                List.of("created_at", "id"),
                true,
                row -> notificationView(row));
    }

    public Map<String, Object> unreadCount(IdentityService.Actor a) {
        a.business();
        var row =
                db.one(
                        "SELECT COUNT(*) AS total FROM notification WHERE enterprise_id=? AND"
                                + " user_id=? AND read_at IS NULL",
                        a.enterpriseId(),
                        a.id());
        return fields("unreadCount", number(row, "total"));
    }

    /** 幂等标记已读；非接收人与其他企业返回统一 404 */
    @Transactional
    public Map<String, Object> read(IdentityService.Actor a, String id) {
        a.business();
        var row =
                db.optional(
                        "SELECT * FROM notification WHERE enterprise_id=? AND id=?",
                        a.enterpriseId(),
                        id);
        if (row.isEmpty() || !a.id().equals(str(row.get(), "user_id"))) throw BusinessException.missing();
        db.update(
                "UPDATE notification SET read_at=CURRENT_TIMESTAMP WHERE enterprise_id=? AND id=?"
                        + " AND read_at IS NULL",
                a.enterpriseId(),
                id);
        return notificationView(db.entity("notification", a.enterpriseId(), id));
    }

    /** 提醒视图：审批申请额外带 resolved，客户端据此隐藏「查看待审批」并标记为「已审批」 */
    private Map<String, Object> notificationView(Map<String, Object> row) {
        Map<String, Object> v = view(row);
        if (!"REVIEW_REQUEST".equals(str(row, "event_type"))) return v;
        String key = str(row, "dedupe_key");
        String reviewId = key == null || key.indexOf('|') < 0 ? key : key.substring(0, key.indexOf('|'));
        v.put("resolved", !reviewPending(str(row, "enterprise_id"), reviewId));
        return v;
    }

    /** 审批申请对应的审批记录仍为 PENDING 才算「待我审批」；记录缺失视为已处理 */
    private boolean reviewPending(String enterpriseId, String reviewId) {
        if (enterpriseId == null || reviewId == null || reviewId.isBlank()) return false;
        return db.optional(
                        "SELECT decision FROM task_review WHERE enterprise_id=? AND id=?",
                        enterpriseId,
                        reviewId)
                .map(row -> "PENDING".equals(str(row, "decision")))
                .orElse(false);
    }

    /**
     * 截止时间扫描：按企业业务时区判断任务截止时刻是否进入未来 24 小时窗口，为每条未完成的执行关系生成
     * 一条截止提醒。已完成、已驳回与已过期的任务不产生提醒（US29）。
     */
    @Scheduled(fixedDelayString = "${pandora.notification-scan-delay-ms:900000}")
    @Transactional
    public int scanDeadlines() {
        Instant now = Instant.now();
        int created = 0;
        for (var enterprise : db.rows("SELECT id,business_timezone FROM enterprise")) {
            String enterpriseId = str(enterprise, "id");
            ZoneId zone = zoneOf(str(enterprise, "business_timezone"));
            if (enterpriseId == null || zone == null) continue;
            LocalDate businessToday = LocalDate.now(zone);
            var rows =
                    db.rows(
                            "SELECT t.id,t.title,t.end_date,t.end_minute,t.time_kind,a.id AS"
                                + " assignment_id,a.recipient_user_id FROM task t JOIN"
                                + " task_assignment a ON a.enterprise_id=t.enterprise_id AND"
                                + " a.task_id=t.id WHERE t.enterprise_id=? AND"
                                + " t.review_status='APPROVED' AND t.end_date BETWEEN ? AND ? AND"
                                + " a.execution_status IN ('ACTIVE','AWAITING_ACCEPTANCE')",
                            enterpriseId,
                            businessToday.minusDays(1),
                            businessToday.plusDays(2));
            for (var row : rows) {
                Instant due = dueInstant(row, zone);
                if (due == null) continue;
                long remaining = Duration.between(now, due).toMillis();
                if (remaining <= 0 || remaining > DEADLINE_LEAD_HOURS * 60L * 60L * 1000L) continue;
                String taskId = str(row, "task_id") == null ? str(row, "id") : str(row, "task_id");
                String title = str(row, "title");
                String assignmentId = str(row, "assignment_id");
                String recipient = str(row, "recipient_user_id");
                String when = LocalDateTime.ofInstant(due, zone).format(STAMP);
                int before = count(enterpriseId, taskId + "|" + DEADLINE, recipient);
                emit(
                        enterpriseId,
                        recipient,
                        DEADLINE,
                        taskId,
                        assignmentId,
                        "截止时间临近",
                        "《" + title + "》将于 " + when + " 截止，请尽快处理。",
                        taskId + "|" + DEADLINE,
                        due);
                created += count(enterpriseId, taskId + "|" + DEADLINE, recipient) - before;
            }
        }
        return created;
    }

    private int count(String enterpriseId, String dedupeKey, String userId) {
        return db.rows(
                        "SELECT id FROM notification WHERE enterprise_id=? AND user_id=? AND"
                                + " dedupe_key=?",
                        enterpriseId,
                        userId,
                        dedupeKey)
                .size();
    }

    private ZoneId zoneOf(String zone) {
        if (zone == null || zone.isBlank()) return null;
        try {
            return ZoneId.of(zone);
        } catch (DateTimeException ex) {
            return null;
        }
    }

    /** 任务截止时刻：ALL_DAY 按当日 23:59，TIMED 按 end_minute，均在公司业务时区 */
    private Instant dueInstant(Map<String, Object> row, ZoneId zone) {
        Object date = row.get("end_date");
        if (date == null) return null;
        LocalDate day =
                date instanceof LocalDate local
                        ? local
                        : LocalDate.parse(date.toString(), DateTimeFormatter.ISO_LOCAL_DATE);
        int minute = 23 * 60 + 59;
        Object value = row.get("end_minute");
        if (value != null) minute = ((Number) value).intValue();
        minute = Math.max(0, Math.min(minute, 23 * 60 + 59));
        return LocalDateTime.of(day, LocalTime.of(minute / 60, minute % 60))
                .atZone(zone)
                .toInstant();
    }
}
