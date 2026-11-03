package com.pandora6ix.backend.service.ai;

import static com.pandora6ix.backend.repository.BusinessDb.fields;
import static com.pandora6ix.backend.repository.BusinessDb.number;
import static com.pandora6ix.backend.repository.BusinessDb.str;

import com.pandora6ix.backend.repository.BusinessDb;
import com.pandora6ix.backend.service.IdentityService;
import com.pandora6ix.backend.service.Pages;
import com.pandora6ix.backend.service.ScopeService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 职业星图 / 公司下一站的最小化摘要：只读本人或现有可见范围的数据，
 * 只输出统计、标签频次与脱敏截断的摘录，不含人名与原始全文。
 */
@Component
public class AiDigestBuilder {
    static final int CAREER_DAYS = 30;
    static final int COMPANY_DAYS = 90;
    static final int MAX_ENTRY_EXCERPTS = 40;
    static final int ENTRY_EXCERPT_LENGTH = 60;
    static final int MAX_TASK_TITLES = 20;
    static final int TASK_TITLE_LENGTH = 30;
    static final int MAX_COMPANY_ITEMS = 20;
    static final int COMPANY_ITEM_LENGTH = 40;
    private static final Map<String, String> EXECUTION = Map.of(
            "PENDING_APPROVAL", "待审批", "ACTIVE", "进行中", "AWAITING_ACCEPTANCE", "待验收", "COMPLETED", "已完成");
    private static final Map<Integer, String> PRIORITY = Map.of(1, "很低", 2, "一般", 3, "普通", 4, "重要", 5, "紧急");
    private final BusinessDb db;
    private final ScopeService scope;

    public AiDigestBuilder(BusinessDb db, ScopeService scope) {
        this.db = db;
        this.scope = scope;
    }

    record Digest(Map<String, Object> input, Map<String, Object> stats, List<Map<String, Object>> topTags, boolean empty) {}

    AiRedactor redactor(IdentityService.Actor a) {
        List<String> names = new ArrayList<>();
        for (var row : db.rows("SELECT display_name,login_name FROM app_user WHERE enterprise_id=?", a.enterpriseId())) {
            names.add(str(row, "display_name"));
            names.add(str(row, "login_name"));
        }
        return new AiRedactor(names);
    }

    Digest career(IdentityService.Actor a, LocalDate today) {
        LocalDate from = today.minusDays(CAREER_DAYS - 1);
        var redactor = redactor(a);
        long journals = number(db.one("SELECT COUNT(*) AS c FROM work_log WHERE enterprise_id=? AND author_user_id=? AND work_date BETWEEN ? AND ?",
                a.enterpriseId(), a.id(), from, today), "c");
        var entries = db.rows("SELECT e.content FROM work_log_entry e JOIN work_log l ON l.enterprise_id=e.enterprise_id AND l.id=e.work_log_id"
                        + " WHERE e.enterprise_id=? AND e.author_user_id=? AND l.work_date BETWEEN ? AND ?"
                        + " ORDER BY l.work_date DESC,e.display_order,e.id",
                a.enterpriseId(), a.id(), from, today);
        List<String> excerpts = new ArrayList<>();
        for (var row : entries) {
            if (excerpts.size() >= MAX_ENTRY_EXCERPTS) break;
            String text = redactor.excerpt(str(row, "content"), ENTRY_EXCERPT_LENGTH);
            if (!text.isBlank()) excerpts.add(text);
        }
        var journalTags = counts(db.rows("SELECT g.name AS label,COUNT(*) AS c FROM work_log_tag wt"
                        + " JOIN work_log l ON l.enterprise_id=wt.enterprise_id AND l.id=wt.work_log_id"
                        + " JOIN enterprise_tag g ON g.enterprise_id=wt.enterprise_id AND g.id=wt.tag_id"
                        + " WHERE wt.enterprise_id=? AND l.author_user_id=? AND l.work_date BETWEEN ? AND ?"
                        + " GROUP BY g.name ORDER BY c DESC,g.name LIMIT 10",
                a.enterpriseId(), a.id(), from, today), redactor);

        // 本人任务：当前成员、已通过审批、与统计窗口有交集
        String own = " FROM task_assignment x JOIN task t ON t.enterprise_id=x.enterprise_id AND t.id=x.task_id"
                + " WHERE x.enterprise_id=? AND x.recipient_user_id=? AND x.current_member=TRUE"
                + " AND t.review_status='APPROVED' AND t.end_date>=? AND t.start_date<=?";
        Object[] ownArgs = {a.enterpriseId(), a.id(), from, today.plusDays(CAREER_DAYS)};
        var byStatus = labeled(db.rows("SELECT x.execution_status AS k,COUNT(*) AS c" + own + " GROUP BY x.execution_status", ownArgs), EXECUTION);
        var byPriority = priorities(db.rows("SELECT t.priority_level AS k,COUNT(*) AS c" + own + " GROUP BY t.priority_level", ownArgs));
        var taskTags = counts(db.rows("SELECT g.name AS label,COUNT(*) AS c" + own.replace(" WHERE ",
                        " JOIN task_tag tt ON tt.enterprise_id=t.enterprise_id AND tt.task_id=t.id JOIN enterprise_tag g ON g.enterprise_id=tt.enterprise_id AND g.id=tt.tag_id WHERE ")
                        + " GROUP BY g.name ORDER BY c DESC,g.name LIMIT 10", ownArgs), redactor);
        List<String> titles = new ArrayList<>();
        for (var row : db.rows("SELECT t.title" + own + " ORDER BY t.end_date DESC,t.id LIMIT " + MAX_TASK_TITLES, ownArgs))
            titles.add(redactor.excerpt(str(row, "title"), TASK_TITLE_LENGTH));
        long tasks = sum(byStatus), completed = byStatus.getOrDefault("已完成", 0L);

        var stats = fields("windowDays", CAREER_DAYS, "journalCount", journals, "entryCount", (long) entries.size(),
                "taskCount", tasks, "completedTaskCount", completed);
        var input = fields("windowDays", CAREER_DAYS, "journalCount", journals, "entryCount", entries.size(),
                "journalTags", journalTags,
                "tasks", fields("total", tasks, "byStatus", byStatus, "byPriority", byPriority, "tags", taskTags),
                "taskTitles", titles, "entryExcerpts", excerpts);
        return new Digest(input, stats, journalTags, journals == 0 && tasks == 0);
    }

    Digest company(IdentityService.Actor a, LocalDate today) {
        var redactor = redactor(a);
        List<String> items = new ArrayList<>();
        for (var row : db.rows("SELECT title FROM company_important_item WHERE enterprise_id=? ORDER BY display_order,created_at DESC,id LIMIT " + MAX_COMPANY_ITEMS,
                a.enterpriseId()))
            items.add(redactor.excerpt(str(row, "title"), COMPANY_ITEM_LENGTH));

        // 与 TaskService.visible 一致：公司级已通过任务、本人或下属创建、下属参与、本人参与的已通过任务
        Set<String> managed = a.businessRank() < 3 ? scope.managed(a) : Set.of();
        List<Object> args = new ArrayList<>(List.of(a.enterpriseId(), today.minusDays(COMPANY_DAYS - 1), a.id(), a.id()));
        StringBuilder visible = new StringBuilder(" t.enterprise_id=? AND t.end_date>=? AND ((t.company_wide=TRUE AND t.review_status='APPROVED')"
                + " OR t.creator_user_id=? OR EXISTS (SELECT 1 FROM task_assignment v WHERE v.enterprise_id=t.enterprise_id AND v.task_id=t.id"
                + " AND ((t.review_status='APPROVED' AND v.recipient_user_id=?)");
        if (!managed.isEmpty()) {
            visible.append(" OR v.recipient_user_id IN (").append(Pages.placeholders(managed)).append(")");
            args.addAll(managed);
        }
        visible.append("))");
        if (!managed.isEmpty()) {
            visible.append(" OR t.creator_user_id IN (").append(Pages.placeholders(managed)).append(")");
            args.addAll(managed);
        }
        visible.append(")");
        Object[] params = args.toArray();
        String where = " WHERE" + visible;

        long tasks = number(db.one("SELECT COUNT(*) AS c FROM task t" + where, params), "c");
        String assignments = " FROM task_assignment x JOIN task t ON t.enterprise_id=x.enterprise_id AND t.id=x.task_id" + where
                + " AND x.current_member=TRUE AND t.review_status='APPROVED'";
        var byStatus = labeled(db.rows("SELECT x.execution_status AS k,COUNT(*) AS c" + assignments + " GROUP BY x.execution_status", params), EXECUTION);
        List<Object> overdueArgs = new ArrayList<>(args);
        overdueArgs.add(today);
        long overdue = number(db.one("SELECT COUNT(*) AS c" + assignments + " AND x.execution_status<>'COMPLETED' AND t.end_date<?", overdueArgs.toArray()), "c");
        var byPriority = priorities(db.rows("SELECT t.priority_level AS k,COUNT(*) AS c FROM task t" + where + " GROUP BY t.priority_level", params));
        var tags = counts(db.rows("SELECT g.name AS label,COUNT(*) AS c FROM task t JOIN task_tag tt ON tt.enterprise_id=t.enterprise_id AND tt.task_id=t.id"
                + " JOIN enterprise_tag g ON g.enterprise_id=tt.enterprise_id AND g.id=tt.tag_id" + where
                + " GROUP BY g.name ORDER BY c DESC,g.name LIMIT 10", params), redactor);
        long completed = byStatus.getOrDefault("已完成", 0L), active = byStatus.getOrDefault("进行中", 0L) + byStatus.getOrDefault("待验收", 0L);

        var stats = fields("windowDays", COMPANY_DAYS, "importantItemCount", (long) items.size(), "taskCount", tasks,
                "completedCount", completed, "inProgressCount", active, "overdueCount", overdue);
        var input = fields("windowDays", COMPANY_DAYS, "importantItems", items,
                "tasks", fields("total", tasks, "assignmentsByStatus", byStatus, "overdueAssignments", overdue,
                        "byPriority", byPriority, "tags", tags));
        return new Digest(input, stats, tags, items.isEmpty() && tasks == 0);
    }

    private static List<Map<String, Object>> counts(List<Map<String, Object>> rows, AiRedactor redactor) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var row : rows) result.add(fields("name", redactor.excerpt(str(row, "label"), 16), "count", number(row, "c")));
        return result;
    }

    private static Map<String, Long> labeled(List<Map<String, Object>> rows, Map<String, String> labels) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (var row : rows) result.merge(labels.getOrDefault(str(row, "k"), str(row, "k")), number(row, "c"), Long::sum);
        return result;
    }

    private static Map<String, Long> priorities(List<Map<String, Object>> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (var row : rows) {
            int level = (int) number(row, "k");
            result.merge(PRIORITY.getOrDefault(level, String.valueOf(level)), number(row, "c"), Long::sum);
        }
        return result;
    }

    private static long sum(Map<String, Long> values) {
        return values.values().stream().mapToLong(Long::longValue).sum();
    }
}
