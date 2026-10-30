## Why

The confirmed teacher workflow is only partially represented by current task APIs: effective tasks cannot be revised, textual progress still requires a percentage, status selection is incomplete, and operation history covers personnel but not task/journal mutations. Complete these backend gaps while preserving existing clients and tenant boundaries.

## What Changes

- Add reviewable edits of effective tasks, keeping old effective content until approval and storing immutable proposals.
- Allow text-only progress without mandatory percentages and add versioned execution-status updates.
- Persist successful task/journal changes in their business transaction and sanitized authenticated failures separately after rollback; identify Web and Android consistently.
- Audit existing journal deletion; do not add task deletion in this round.
- Document matching client requests and verify isolation, rollback, concurrency, idempotency and older clients.

## Capabilities

### New Capabilities
- `task-workflow-completion`: effective-task revisions, text progress and explicit execution status.

### Modified Capabilities
- `backend-operation-history`: extend auditable operations to tasks and journals, including safe failure records.

## Impact

Backend task/review/journal services, controller DTOs, additive Flyway migration and OpenAPI. Shared client source headers only; client UI integration remains documented for Android developers. No push credentials or provider activation.
