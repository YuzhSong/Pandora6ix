## Context

backend-syz has fast-forwarded to dev a8adf5d. Spring Boot 4 / Java 21 currently has health and framework error contracts only. Authoritative inputs are docs requirements V1.3, API0.1, DB0.2 and the 2026-10-13 gap audit. The user authorizes implementation, backend only, without a PR.

## Goals / Non-Goals

**Goals:** Complete the confirmed Planned API surface using real persistence and enforce enterprise/object/role boundaries, workflow state, optimistic concurrency and journal/selection invariants. Keep the backend independently buildable.

**Non-Goals:** Reserved approval/inbox services, unconfirmed Q-01–09 semantics, frontend work, production deployment, a PR or automatic dev merge. P1 team rates/distribution and P2 AI remain future work.

## Decisions

- JDBC transactions and Flyway migrations; local file H2, isolated H2 integration tests. MySQL 8.4 is only a candidate in DB0.2, so do not pretend a MySQL deployment was validated. DataSource is configurable; migration portability requires separate deployment validation. Generated UUID string IDs finalize the candidate ID scheme and avoid JavaScript precision issues. Same-enterprise composite foreign keys protect references, with owner keys for journal entries/selections. Service validation handles tree cycles and aggregate counts.
- Bearer-only opaque 256-bit random sessions; persist SHA-256 token digests and expiration/revocation, BCrypt passwords. A MVC interceptor guards business controllers; services derive current identity from it and recheck tenant/object scope. No ambient cookie authentication, default credentials or hardcoded enterprise. Bootstrap enterprise creation requires an externally configured secret header; existing ADMIN can create a new enterprise with a separate initial ADMIN. Current enterprise read/update is an explicit FR-USER-04 route extension. Password change invalidates the current session only; role/status changes are read on each request.
- Explicit per-enterprise IANA timezone; absence is an actionable error for business-day reads, not an implicit Asia/Shanghai default. Technical request size, string length, page size 1–100 and single date window bounds are documented, not presented as customer limits.
- Successful business responses have data/requestId; nested errors plus legacy flat fields preserve existing tests and health compatibility. Private unavailable objects use uniform 404. Strict DTOs reject forged identity/unknown fields. Typed command records use bean validation; read DTOs have explicit named fields.
- Optimistic WHERE version updates and transactional row locks for completion/selection/idempotency. Idempotency-Key is scoped by enterprise, actor, route and request digest; bounded keys replay the original payload, mismatches return 409. Serialization is JSON; no new queue.
- Managers use organization descendants plus lower business role, independently of the creator's direct supervisor approval link. ADMIN alone has no diary/task role. Founder covers lower business roles across the enterprise; department/team roles require matching organization kind. Company items are enterprise-wide, maintained by ADMIN/FOUNDER; department publication is closed pending the unspecified restricted publication scope.
- Task creation writes a PENDING review snapshot and PENDING_APPROVAL assignments; no supervisor yields WORKFLOW_POLICY_UNCONFIRMED. Approval APIs remain absent. Integration tests explicitly seed approved database fixtures; production has no bypass. Personal SELF completion and delegated creator acceptance are separate transactional branches. Pending creator edits alone are permitted and append a new immutable review snapshot; earlier attempts retain their content/version and cannot approve a newer task version. Approved/rejected editing and resubmission remain closed.
- Journal entries are stable children; omitted patch entries survive. One journal per author/date, with existingJournalId on duplicate. Important entry selection preserves an explicit empty manual selection; new expired entries are rejected while existing selections are not silently removed. DTO eligibility is computed; no replacement/deletion policy is invented. Statistics count approved assignments belonging to the current user and expose this declared basis, without an invented overall task/parent denominator.

## Risks / Trade-offs

- Reserved approval prevents newly created tasks executing until future approval work → honest PENDING states and executable approved fixtures in tests.
- Local H2 does not establish production MySQL readiness → document dialect/configuration boundary and use migrations for local persistence.
- Unspecified company publication scope and date endpoint semantics → explicit policy errors rather than speculative behavior.
- UUID ordering is stable but not chronological → sort business dates / event timestamps with id as final tie breaker, bind opaque cursors to filters.

## Migration Plan

Flyway initializes only new backend tables; preserve existing health behavior. Start with an explicitly configured bootstrap secret and enterprise timezone, then create accounts through ADMIN. Back up a persisted database before migration/deployment; do not roll back by deleting business data. Keep changes and validation on backend-syz with the OpenSpec change active until a later integration request.

## Open Questions

Q-01–09 remain governed by docs. The user confirmed there is no fixed team database/timezone configuration and authorized local persistence with explicit enterprise timezone configuration. Approval/inbox, parent-task links, deletion, drafts, tag retirement and team statistical denominators are not claimed complete.
