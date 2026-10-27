## Context

Existing task review snapshots and operation_log tables can support the missing workflow without replacing the task model or exposing credentials. Current clients use percentage and completion-request APIs, so compatibility is required.

## Goals / Non-Goals

Goals: auditable task/journal writes, text progress, explicit work status and reviewable effective-task edits with tenant and version checks. Non-goals: push activation, frontend redesign, removal of personal-task compatibility or unrelated personnel changes.

## Decisions

1. Reuse task_review with a CREATE/EDIT discriminator. An edit proposal stores immutable new content; ordinary effective-task changes remain separate from effective content until approval. Superseding versions and task-first locking prevent outdated approval. Confirmed: creator edits of effective tasks keep the old content until supervisor approval; founder edits apply immediately. Delegated completion keeps creator/superior acceptance; selecting COMPLETED submits a completion request, not a completed state.
2. Keep legacy execution_status for compatibility; expose a separate three-value workStatus to distinguish NOT_STARTED, IN_PROGRESS and COMPLETED. Text progress accepts omitted progressPercent and preserves its legacy value. Status mutations require the actual recipient and expected assignment version.
3. Record successes from service transactions, using whitelisted task/journal snapshots with actual versions and captured actor/related-person names. Idempotent replays do not re-enter mutations. Append authenticated HTTP failures after rollback in a separate transaction; store only object identifier, action, request ID, status and safe error code, never raw bodies or exception SQL. Anonymous failures cannot safely be attributed to a tenant and are excluded.
4. Use X-Client-Source with bounded WEB/ANDROID values on shared clients. This is display metadata, never authorization. Missing/unknown sources use SYSTEM. Backend determines identity and enterprise only from authenticated sessions.
5. User explicitly excluded adding task deletion this round. Journal deletion is audited; existing unsupported task deletion requests can be logged as failures. Completed work remains terminal; pending delegated acceptance must be decided by the creator before further progress/state changes.

## Risks / Trade-offs

[Race between proposal and approval] -> Lock task before review/assignment consistently and verify versions/current proposal.
[Audit failure] -> Successful business writes roll back with failed success-audit persistence; failure-audit errors are safely logged without changing an already resolved response.
[Legacy clients] -> Keep old fields and endpoints, add optional fields/routes and document new client integration.

## Migration Plan

Use an additive migration for review kind and work status. Retain current data and back up via the existing deployment receiver. Validate all three modules for shared header changes.

## Open Questions

None. Decisions confirmed on 2026-10-27. Personal-task compatibility and its existing self-completion remain unchanged; this confirmation specifically concerns delegated tasks.
