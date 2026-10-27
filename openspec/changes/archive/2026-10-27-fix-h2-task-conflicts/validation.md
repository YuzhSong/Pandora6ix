# Validation, 2026-10-27

## Evidence and cause

Cloud release 2f2c751 logs contain repeated POST /api/v1/tasks DATA_CONFLICT responses with SQLState/vendorCode 23514. Existing constraint definitions match checked-in schema. H2 documents 23514 as invalid CHECK evaluation, distinct from 23513 for invalid input. Plain JDBC reproduction on 2.4.240 closes a constraint-creator connection while another remains open; a valid PERSONAL/DELEGATED insert then fails with 23514 and nested 90098 (closed originating session). The same fixture passes on 2.5.252. This matches upstream h2database/h2database#4302 and its fix #4311.

The affected dependency was inherited from the original Spring Boot 4 foundation; it was not introduced by the latest Android merge. Prior automated checks and short local file-backed integration passed but did not explicitly retire physical pool connections. The missing lifecycle coverage is now added. Production diagnosis used metadata SELECTs and authenticated reads; no production test task/account was created. Exact prior failing Android payloads were unavailable, so the test matrix covers all supplied combinations.

## Automated checks

- Old-version regression: `mvn -Dh2.version=2.4.240 -Dtest=H2ConnectionLifecycleTests test` fails on a valid write with 23514/90098.
- Fixed-version `mvn verify`: 118 tests, zero failures/errors/skips. Includes 15 task combinations after Hikari pool eviction: employee/manager/founder personal tasks, manager mixed self/subordinate delegation, founder delegation; all-day, same-day timed and cross-day timed; supervisor approval and founder auto-approval.
- Old-engine full-schema disposable file: created with all existing migrations under 2.4.240, then read/written by 2.5.252 after closing the opening connection; committed row count preserved. This is a synthetic compatibility fixture, not an export of production business data.
- Existing operations console source/tests compiled and passed against 2.5.252: file reads, private projection, write/DDL denial, viewer refresh, HTTP serving and reopen.
- Real Spring + H2 + Vite repository integration passed using the new backend jar.
- Packaged runtime smoke check passed: component scan, migrations, health, protected profile. Packaged jar contains exactly h2-2.5.252.jar. Web code is unchanged; packaging reused the previously verified matching Web artifact.
- Generated OpenAPI unchanged and verified at 63 operations; strict OpenSpec validation passed.
- Flyway emits its existing advisory that H2 is newer than its verified support range; all ten migrations and package startup were exercised successfully.

## Deployment and remaining verification

This PR is prepared for review; no merge, production database-engine replacement, service restart or production push send was performed in this diagnostic task. Use the existing stopped-service pre-release backup before deployment. Do not assume an old engine can reopen a database already written by a newer engine; rollback must preserve and use the matching backup if needed. Confirm creation from the reporting Android app after release.

The Web backend contract work already exists in PR #32. A live accountIdentifier login succeeded and real same-object history lists were returned; legacy versions and historical labels remain null when not originally captured. Android still uses the legacy enterprise-domain/login-name form. Zhao Ke currently belongs to the Web team, with a successful JOB_CHANGE record from 2026-10-24; no retained structured error identified his earlier failed request, so its precise cause is not retroactively asserted.

JPush AppKey and Master Secret are both absent from the running server environment. Remote push requires configuration by an operations account, a rotated Master Secret, matching Android AppKey and physical-device acceptance. Credentials are not required for ordinary task writes; no credentials are included here.

Sources: https://h2database.com/javadoc/org/h2/api/ErrorCode.html ; https://github.com/h2database/h2database/pull/4311 ; https://github.com/h2database/h2database/releases/tag/version-2.5.252
