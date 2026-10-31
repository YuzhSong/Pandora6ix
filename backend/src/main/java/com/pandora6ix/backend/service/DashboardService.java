package com.pandora6ix.backend.service;

import static com.pandora6ix.backend.repository.BusinessDb.*;

import com.pandora6ix.backend.api.*;
import com.pandora6ix.backend.repository.BusinessDb;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class DashboardService
        implements com.pandora6ix.backend.application.dashboard.DashboardUseCases {
    private final BusinessDb db;
    private final JournalService journals;
    private final TaskService tasks;

    public DashboardService(BusinessDb db, JournalService journals, TaskService tasks) {
        this.db = db;
        this.journals = journals;
        this.tasks = tasks;
    }

    public Map<String, Object> items(IdentityService.Actor a, String cursor, int limit) {
        return Pages.sqlPage(
                db,
                "SELECT * FROM company_important_item WHERE enterprise_id=?",
                List.of(a.enterpriseId()),
                cursor,
                limit,
                a.enterpriseId() + ":company-items",
                List.of("display_order", "created_at", "id"),
                false,
                row -> {
                    var v = view(row);
                    v.put("itemId", v.get("id"));
                    return v;
                });
    }

    public Map<String, Object> item(IdentityService.Actor a, String id) {
        var result = view(db.entity("company_important_item", a.enterpriseId(), id));
        result.put("itemId", result.get("id"));
        return result;
    }

    private void publisher(IdentityService.Actor a) {
        if (a.has("ADMIN") || a.has("FOUNDER")) return;
        if (a.has("DEPARTMENT_MANAGER"))
            throw BusinessException.policy(
                    "The restricted department publication scope is not defined in the current"
                            + " company-item contract.");
        throw BusinessException.forbidden();
    }

    @Transactional
    public Map<String, Object> createItem(IdentityService.Actor a, Contracts.ItemCreate c) {
        publisher(a);
        String id = id();
        db.update(
                "INSERT INTO"
                    + " company_important_item(id,enterprise_id,publisher_user_id,title,content,display_order)"
                    + " VALUES(?,?,?,?,?,?)",
                id,
                a.enterpriseId(),
                a.id(),
                c.title(),
                c.content(),
                c.displayOrder());
        return item(a, id);
    }

    @Transactional
    public Map<String, Object> patchItem(
            IdentityService.Actor a, String id, Contracts.ItemPatch c) {
        publisher(a);
        db.entity("company_important_item", a.enterpriseId(), id);
        BusinessException.checkVersion(
                db.update(
                        "UPDATE company_important_item SET"
                                + " title=?,content=?,display_order=?,version=version+1 WHERE"
                                + " enterprise_id=? AND id=? AND version=?",
                        c.title(),
                        c.content(),
                        c.displayOrder(),
                        a.enterpriseId(),
                        id,
                        c.expectedVersion()));
        return item(a, id);
    }

    public Map<String, Object> statistics(IdentityService.Actor a) {
        a.business();
        var row =
                db.one(
                        "SELECT COUNT(*) AS total,COALESCE(SUM(CASE WHEN"
                            + " a.execution_status='COMPLETED' THEN 1 ELSE 0 END),0) AS completed"
                            + " FROM task_assignment a JOIN task t ON"
                            + " t.enterprise_id=a.enterprise_id AND t.id=a.task_id WHERE"
                            + " a.enterprise_id=? AND a.current_member=TRUE AND a.recipient_user_id=? AND"
                            + " t.review_status='APPROVED'",
                        a.enterpriseId(),
                        a.id());
        long total = number(row, "total"), completed = number(row, "completed");
        return fields(
                "total",
                total,
                "completed",
                completed,
                "uncompleted",
                total - completed,
                "basis",
                "OWN_APPROVED_ASSIGNMENTS");
    }

    public Map<String, Object> dashboard(IdentityService.Actor a) {
        a.business();
        var company = items(a, null, 20);
        company.put("moreUrl", "/api/v1/company-important-items");
        var taskPanel = tasks.delegatedPage(a, null, 20);
        taskPanel.put("moreUrl", "/api/v1/dashboard/task-items");

        var entries = journals.candidates(a, null, 20);
        entries.put("moreUrl", "/api/v1/me/important-entry-candidates");
        return fields(
                "companyImportantItems",
                company,
                "companyTasks",
                taskPanel,
                "personalImportantEntries",
                journals.selection(a),
                "workEntries",
                entries,
                "statistics",
                statistics(a));
    }

    public Map<String, Object> dashboardTasks(IdentityService.Actor a, String cursor, int limit) {
        return tasks.delegatedPage(a, cursor, limit);
    }
}
