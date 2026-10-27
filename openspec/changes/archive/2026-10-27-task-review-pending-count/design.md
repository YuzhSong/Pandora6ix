# Design

## Context

The existing pending review list already enforces reviewer identity, business rank, task version, and latest attempt. The mobile navigation currently fetches a page of review details to derive its badge count.

## Goals / Non-Goals

**Goals:** Return the badge count using a focused, cheap query with exactly the queue's authorization and filtering semantics.

**Non-Goals:** Push delivery/WebSocket infrastructure, client polling changes, notification counts, or database schema changes.

## Decisions

- Add `GET /api/v1/task-reviews/pending-count` alongside the existing review routes. Return `{count: number}` through the standard API envelope.
- Require the same manager capability as `scope=pending`; do not give ADMIN-only accounts implicit access.
- Use a database `COUNT(*)` query scoped by enterprise and reviewer, requiring pending task/review statuses, matching task version, and latest attempt. Recompute on every request rather than cache, so a client refresh observes approvals and resubmissions promptly.
- Do not derive the count by fetching/paging reviews: the endpoint avoids materializing snapshots and user detail for a badge.

## Risks / Trade-offs

- Frequent client requests still execute a count query. Existing pending-review indexes and enterprise/reviewer predicates bound the work; real-time refresh cadence remains a client decision.
- Count and list can differ if state changes between separate requests. Both are live views; the count is advisory and the decision endpoint remains authoritative.

## Migration Plan

No schema migration is needed. Deploy the additive endpoint before clients switch to it; rollback only removes the endpoint and clients can continue using the existing paginated queue.
