package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.service.*;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
public class JournalController {
    private final IdentityService identity;
    private final JournalService journals;
    private final DashboardService dashboard;
    private final Idempotency idempotency;

    public JournalController(
            IdentityService identity,
            JournalService journals,
            DashboardService dashboard,
            Idempotency idempotency) {
        this.identity = identity;
        this.journals = journals;
        this.dashboard = dashboard;
        this.idempotency = idempotency;
    }

    @GetMapping("/work-logs")
    public Object logs(
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "mine") String scope,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return journals.list(identity.current(), date, from, to, scope, cursor, limit);
    }

    /** 有日志的日期集合（全部历史）：供客户端日期选择器着色与月份跳转 */
    @GetMapping("/work-logs/dates")
    public Object dates() {
        return journals.dates(identity.current());
    }

    @GetMapping("/work-logs/{id}")
    public Object detail(@PathVariable String id) {
        return journals.detail(identity.current(), id);
    }

    @GetMapping("/work-log-entries/{id}")
    public Object entry(@PathVariable String id) {
        return journals.entry(identity.current(), id);
    }

    @PostMapping("/work-logs")
    @ResponseStatus(HttpStatus.CREATED)
    public Object create(
            @Valid @RequestBody Contracts.JournalCreate c,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var a = identity.current();
        return idempotency.execute(a, "POST /work-logs", key, c, () -> journals.create(a, c));
    }

    @PatchMapping("/work-logs/{id}")
    public Object patch(@PathVariable String id, @Valid @RequestBody Contracts.JournalPatch c) {
        return journals.patch(identity.current(), id, c);
    }

    @DeleteMapping("/work-logs/{id}")
    public Object delete(@PathVariable String id) {
        return journals.delete(identity.current(), id);
    }

    @GetMapping("/me/important-entry-candidates")
    public Object candidates(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return journals.candidates(identity.current(), cursor, limit);
    }

    @GetMapping("/me/important-entries")
    public Object selection() {
        return journals.selection(identity.current());
    }

    @PutMapping("/me/important-entries")
    public Object selection(@Valid @RequestBody Contracts.Selection c) {
        return journals.saveSelection(identity.current(), c);
    }

    @GetMapping("/company-important-items")
    public Object items(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return dashboard.items(identity.current(), cursor, limit);
    }

    @GetMapping("/company-important-items/{id}")
    public Object item(@PathVariable String id) {
        return dashboard.item(identity.current(), id);
    }

    @PostMapping("/company-important-items")
    @ResponseStatus(HttpStatus.CREATED)
    public Object createItem(
            @Valid @RequestBody Contracts.ItemCreate c,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var a = identity.current();
        return idempotency.execute(
                a, "POST /company-important-items", key, c, () -> dashboard.createItem(a, c));
    }

    @PatchMapping("/company-important-items/{id}")
    public Object patchItem(@PathVariable String id, @Valid @RequestBody Contracts.ItemPatch c) {
        return dashboard.patchItem(identity.current(), id, c);
    }

    @GetMapping("/dashboard")
    public Object dashboard() {
        return dashboard.dashboard(identity.current());
    }

    @GetMapping("/dashboard/task-items")
    public Object dashboardTasks(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return dashboard.dashboardTasks(identity.current(), cursor, limit);
    }

    @GetMapping("/statistics")
    public Object statistics() {
        return dashboard.statistics(identity.current());
    }
}
