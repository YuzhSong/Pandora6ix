## Context

backend-syz has merged dev ec288d4. The latest Android-lj and Android-swj commits are already ancestors of dev; their network-free prototypes are evidence for screens, not API DTO definitions. Existing backend APIs implement the ten requested modules. The missing production transition is PENDING to APPROVED: only test fixtures currently activate assignments. The user explicitly approved implementing direct-supervisor review on 2026-10-15, superseding the older document's Reserved designation for these three review routes only.

## Goals / Non-Goals

**Goals:** Complete actual task lifecycle APIs, safe review audit/history, useful Android permissions, and reproducible HTTP acceptance for the ten requested modules.

**Non-Goals:** Android/Web edits, inbox/read notifications, self-approval, no-supervisor policy, resubmission, approved edits, timed-endpoint policy changes, new services or dependencies, deployment or PR creation.

## Decisions

- Keep the existing `/api/v1` envelopes, opaque query-bound cursors, Bearer authentication and idempotency implementation. Add `GET /task-reviews?scope=pending|created`, `GET /task-reviews/{id}`, `POST /task-reviews/{id}/decision` using APPROVE/REJECT and expectedVersion. Reuse task_review instead of a second workflow store.
- Review history is visible only to its designated reviewer and submitter within the enterprise, with uniform 404 otherwise. Pending lists include current-version reviews only; created lists include submitted attempts for audit. Detail exposes the immutable submittedSnapshot, taskVersion, currentTaskVersion, isCurrent, allowedActions and persisted decision audit. Superseded attempts remain readable with no actions.
- A decision requires a current pending task/review and current direct-supervisor relationship, an active business submitter and authorized active recipients. Recheck management scope for delegation after directory changes; stale context is 409, unrelated actors are 404. No retargeting or autoapproval occurs.
- Serialize decisions and task edits by locking task first, then review. Use review expectedVersion and atomic transaction updates for audit, task reviewStatus/version and all assignment activations (activatedAt/version). Reject leaves pending assignments inactive. Return the decided review and current task; exact retries with the same Idempotency-Key replay the response.
- Add nullable decided_by_user_id, decision_note and decided_at plus review-list indexes in V2 migration. Preserve V1 checksum and old stored records. Existing legacy decided rows can lack audit fields; new decisions populate them.
- Keep reviewId on task detail pointing to the latest attempt after approval advances the task version. Compute per-assignment allowedActions using the same authorization rules as task-level union; preserve existing fields. Add role-level createPersonalTasks/createDelegatedTasks/reviewTasks capabilities; these are not an override for configured-supervisor or workflow checks.
- Normalize newly edited pending snapshots to the same submission fields as creation, including recipientIds, without runtime action metadata. Preserve historical stored snapshots exactly; their older task projection shape remains readable.
- Extend current MockMvc coverage with approval, stale edits, tenant/role isolation, context changes, concurrency and idempotency. Add an independent real-HTTP smoke script using random enterprise credentials and file H2, restart to verify approved state and journal/selection persistence. Do not use SQL fixtures to approve smoke-test tasks.

## Risks / Trade-offs

- Simultaneous directory edits can invalidate queued reviews → recheck current configured relationship and recipients; decline invalid context with no partial activation. No new hierarchy policy is invented.
- Clients must use the returned assignment version after approval → document and test version advancement and allowedActions per relationship.
- Old docs state Reserved → document this user-confirmed override in backend integration/API guidance and keep inbox/Q-01–09 unresolved.
- H2 is the currently configured local persistent datasource → verify additive upgrade/restart locally; no claim of MySQL or Android application acceptance.

## Migration Plan

Run module tests/package and the isolated HTTP smoke with V1 then V2 migration. Export/check OpenAPI and strict-validate this change. Keep implementation uncommitted on backend-syz for user review; no PR. V2 is additive and previous binaries ignore its nullable columns; do not delete published business data for rollback.
