## 1. Baseline and contracts

- [x] 1.1 Merge latest dev into backend-syz and compare current Android branches, request modules and implemented interfaces.
- [x] 1.2 Record user-confirmed approval scope, API behavior, concurrency and migration design before implementation.

## 2. Backend implementation

- [x] 2.1 Add additive review audit migration and authorized paginated review list/detail endpoints.
- [x] 2.2 Implement current-version direct-supervisor decisions, transactional activation/rejection and idempotent retries.
- [x] 2.3 Preserve latest review identity after approval and expose per-assignment actions and profile task capabilities.

## 3. Verification and handoff

- [x] 3.1 Cover authorization, superseded snapshots, context changes, concurrency, idempotency and both task completion branches in integration tests.
- [x] 3.2 Exercise all ten modules via actual HTTP and file database restart without SQL approval fixtures; verify V1 database upgrade.
- [x] 3.3 Update backend Android integration guidance, API/OpenAPI contract and validation evidence, retaining unresolved policy boundaries.
- [x] 3.4 Run backend package/tests, OpenAPI check, OpenSpec strict validation and diff checks; leave changes on backend-syz without PR.
