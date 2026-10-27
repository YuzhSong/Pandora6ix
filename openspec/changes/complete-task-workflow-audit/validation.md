# Validation — 2026-10-27

Base: remote `dev` at `2b5c961` (includes PR #34). Implementation is isolated in `codex/complete-task-workflow-audit`.

## Automated verification

- Backend: Java 21, `backend/mvnw.cmd -q -f backend/pom.xml verify` — **128 tests, 0 failures/errors/skips** on final source, including 103 API integration tests.
- New regressions cover effective edit approval/rejection, superseding proposals, founder bypass, execution while an edit is pending, proposal/approval races, changed supervisor/recipient context, text-only progress, completion acceptance, recipient and tenant isolation, idempotent status replay, safe failure history and rollback when either business or success-audit writes fail.
- File-backed upgrade: real Flyway migration V1–V10, retained review/active/completed fixture data, close/reopen, then V11. Old review snapshots and task/assignment versions survive; completed assignments retain completion state and receive the correct work status.
- Web: `npm ci`, `npm test` — **135 passed**; `npm run lint` and `npm run build` passed. Existing React effect and chunk-size warnings remain.
- Android: `testDebugUnitTest assembleDebug compileReleaseKotlin --no-daemon` — **36 unit tests passed**, debug APK and release Kotlin compile passed. Existing icon deprecation warnings remain.
- Real packaged backend with a fresh file-backed H2 database, Vite proxy and Web HTTP repository: `npm run test:integration` passed personnel persistence, tenant/role controls, organization changes, credential lifecycle, CSV creation, important-item versions and session lifecycle.
- `python deploy/check-package.py` passed the thin runtime package: real health controller, component scan, database migration and protected profile endpoint.
- `python backend/scripts/generate-openapi.py --check` — **64 operations**, synchronized. Pending-review count schema now correctly describes `count`.
- `openspec validate complete-task-workflow-audit --strict` and `git diff --check` passed.

The first backend run found a missing bootstrap token in the new foreign-enterprise fixture and a clock-sensitive existing push fixture. The bootstrap fixture now uses the provisioning token, and the fake push delivery is explicitly already due instead of relying on database/JVM submillisecond clock ordering. Provider calls remain mocked in these unit tests; production push configuration is untouched.

## Delivery boundary

See `backend/TASK_WORKFLOW_HANDOFF.md` for versioned requests, edit proposals, status/acceptance mapping and operation-history behavior.

This change adds no task deletion endpoint, credentials, tables or infrastructure services. V11 adds review-kind and work-status columns. New history begins at deployment; missing past snapshots are not invented. Anonymous failures have no tenant audit record.

Shared Web/Android clients now forward their source header. Android task-edit/status/text-progress screens are not wired in this backend change; the existing percentage-based completion display needs client adaptation to distinguish pending acceptance. No Android device acceptance, cloud deployment, production-data mutation or live push-provider test was performed.

[PR #35](https://github.com/YuzhSong/Pandora6ix/pull/35) targets `dev` and awaits non-author review. Keep the OpenSpec change active until merge and validation; no separate archive PR is created here.
