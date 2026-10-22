package com.pandora6ix.backend.controller;

import com.pandora6ix.backend.api.Contracts;
import com.pandora6ix.backend.service.Idempotency;
import com.pandora6ix.backend.service.IdentityService;
import com.pandora6ix.backend.service.TaskReviewService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TaskReviewController {
    private final IdentityService identity;
    private final TaskReviewService reviews;
    private final Idempotency idempotency;

    public TaskReviewController(
            IdentityService identity, TaskReviewService reviews, Idempotency idempotency) {
        this.identity = identity;
        this.reviews = reviews;
        this.idempotency = idempotency;
    }

    @GetMapping("/task-reviews")
    public Object list(
            @RequestParam(defaultValue = "pending") String scope,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int limit) {
        return reviews.list(identity.current(), scope, cursor, limit);
    }

    @GetMapping("/task-reviews/pending-count")
    public Object pendingCount() {
        return reviews.pendingCount(identity.current());
    }

    @GetMapping("/task-reviews/{id}")
    public Object detail(@PathVariable String id) {
        return reviews.detail(identity.current(), id);
    }

    @PostMapping("/task-reviews/{id}/decision")
    public Object decision(
            @PathVariable String id,
            @Valid @RequestBody Contracts.ReviewDecision c,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        var a = identity.current();
        // Authorize even idempotent replays after a role change.
        reviews.detail(a, id);
        return idempotency.execute(
                a,
                "POST /task-reviews/" + id + "/decision",
                key,
                c,
                () -> reviews.decide(a, id, c));
    }
}
