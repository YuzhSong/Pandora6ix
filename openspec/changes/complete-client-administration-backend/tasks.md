## 1. Contracts and persistence

- [x] 1.1 Add nullable V3 profile columns and required create/optional preserve-on-patch contracts; restrict sensitive projections and update OpenAPI.
- [x] 1.2 Implement atomic directory persistence and profile validation, preserving legacy and role-only updates.

## 2. Authorization and organization

- [x] 2.1 Implement versioned department/team conversion with root, parent, child, manager-role and enterprise checks under the enterprise lock.
- [x] 2.2 Require deployment bootstrap for company initialization; restrict ADMIN-only tag/business routes and align capabilities.

## 3. Android recipient integration

- [x] 3.1 Return stable department metadata; group Android candidates by department ID and retrieve complete pagination.
- [x] 3.2 Inspect the cloud test account and apply only the agreed management role through administrator APIs; verify live capabilities/candidates.

- [x] 3.3 Enforce today-only edit/delete and one-time yesterday/day-before backfill; align Android company timezone, read-only UI and boundary tests.

## 4. Verification and handoff

- [x] 4.1 Add meaningful regressions for creation/login/profile persistence, legacy/role-only edits, rollback, conflict, privacy, tenant isolation, unsafe organization changes and concurrent directory edits.
- [x] 4.2 Update HTTP/Web smoke fixtures and verify old-schema migration, restart persistence and existing workflows.
- [x] 4.3 Run backend package/OpenAPI, Android build/unit tests, Web tests/lint/build and strict OpenSpec validation; record precise limitations.
- [x] 4.4 Verify merged PR #12/#13 deployment, commit backend-syz and open a review PR linked to this OpenSpec change. Keep audit storage undecided.
