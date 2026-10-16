package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.service.*;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TaskController {
    private final IdentityService identity;
    private final TaskService tasks;
    private final TagService tags;
    private final Idempotency idempotency;

    public TaskController(
            IdentityService identity, TaskService tasks, TagService tags, Idempotency idempotency) {
        this.identity = identity;
        this.tasks = tasks;
        this.tags = tags;
        this.idempotency = idempotency;
    }

    @GetMapping("/tags")
    public Object tags(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return tags.list(identity.current(), name, cursor, limit);
    }

    @PostMapping("/tags")
    @ResponseStatus(HttpStatus.CREATED)
    public Object tag(@Valid @RequestBody Contracts.TagCreate c) {
        return tags.create(identity.current(), c);
    }

    @PatchMapping("/tags/{id}")
    public Object tagPatch(@PathVariable String id, @Valid @RequestBody Contracts.TagPatch c) {
        return tags.patch(identity.current(), id, c);
    }

    @GetMapping("/tasks/recipient-candidates")
    public Object candidates(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return tasks.candidates(identity.current(), q, cursor, limit);
    }

    @GetMapping("/tasks")
    public Object tasks(
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(defaultValue = "mine") String scope,
            @RequestParam(required = false) List<String> tagIds,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return tasks.list(identity.current(), from, to, scope, tagIds, cursor, limit);
    }

    @GetMapping("/tasks/{id}")
    public Object detail(@PathVariable String id) {
        return tasks.detail(identity.current(), id);
    }

    @PostMapping("/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public Object create(
            @Valid @RequestBody Contracts.TaskCreate c,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var a = identity.current();
        return idempotency.execute(a, "POST /tasks", key, c, () -> tasks.create(a, c));
    }

    @PatchMapping("/tasks/{id}")
    public Object patch(@PathVariable String id, @Valid @RequestBody Contracts.TaskPatch c) {
        return tasks.patch(identity.current(), id, c);
    }

    @PostMapping("/task-assignments/{id}/progress-records")
    @ResponseStatus(HttpStatus.CREATED)
    public Object progress(
            @PathVariable String id,
            @Valid @RequestBody Contracts.Progress c,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var a = identity.current();
        return idempotency.execute(
                a, "POST /assignments/" + id + "/progress", key, c, () -> tasks.progress(a, id, c));
    }

    @PostMapping("/tasks/{id}/complete")
    public Object complete(
            @PathVariable String id,
            @Valid @RequestBody Contracts.Complete c,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var a = identity.current();
        return idempotency.execute(
                a, "POST /tasks/" + id + "/complete", key, c, () -> tasks.complete(a, id, c));
    }

    @PostMapping("/task-assignments/{id}/completion-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Object request(
            @PathVariable String id,
            @Valid @RequestBody Contracts.CompletionRequest c,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var a = identity.current();
        return idempotency.execute(
                a,
                "POST /assignments/" + id + "/request",
                key,
                c,
                () -> tasks.requestCompletion(a, id, c));
    }

    @PostMapping("/task-completion-requests/{id}/decision")
    public Object decision(
            @PathVariable String id,
            @Valid @RequestBody Contracts.Decision c,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var a = identity.current();
        return idempotency.execute(
                a, "POST /requests/" + id + "/decision", key, c, () -> tasks.decide(a, id, c));
    }

    @GetMapping("/calendar")
    public Object calendar(
            @RequestParam String mode,
            @RequestParam LocalDate anchorDate,
            @RequestParam(required = false) List<String> tagIds,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return tasks.calendar(identity.current(), mode, anchorDate, tagIds, cursor, limit);
    }
}
