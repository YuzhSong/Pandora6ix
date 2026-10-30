## Context

Cloud logs show POST /api/v1/tasks returning 409 with SQLState/vendorCode 23514. H2 distinguishes invalid CHECK evaluation (23514) from a violated CHECK (23513). A plain JDBC fixture reproduces 23514/90098 on 2.4.240 after the constraint-creating connection closes. The runtime has used the affected H2 since the original Spring Boot 4 backend, rather than introducing it in the latest Android merge.

## Goals / Non-Goals

Goals: keep valid task writes usable after physical connection replacement; retain checks that reject invalid values; provide safe useful constraint names. Non-goals: remove database constraints, change task authorization, deploy new services, or change live account data during diagnosis.

## Decisions

Pin H2 2.5.252, a released upstream version containing the IN/CHECK session fix. Avoid ad hoc constraint rewrites and periodic service restarts. Add plain JDBC tests that keep a second connection alive, close the creator, and verify valid and invalid writes. Test full-schema task creation after Hikari soft eviction with PERSONAL/DELEGATED, ALL_DAY/TIMED and cross-day dates. Also cover reopening a file database and loading a file produced by the old engine.

Extract quoted constraint identifiers before generic fallback, never log exception bodies/SQL/values. Keep the client error envelope compatible.

## Risks / Trade-offs

[Engine upgrade compatibility] -> Test a disposable 2.4.240 file with the new engine; retain the normal stopped-service pre-deployment backup and restore matching backup for rollback if needed.
[In-memory tests hide session turnover] -> Add explicit pool eviction and independent connections, not a long sleep.
[Production test messages] -> Use isolated local fixtures; no production tasks or provider sends.

## Migration Plan

Build and verify the dependency in the release package, run all backend tests and isolated API task matrix, submit a focused PR. Deployment uses the existing receiver backup and health checks. No business schema migration or credentials are added.
