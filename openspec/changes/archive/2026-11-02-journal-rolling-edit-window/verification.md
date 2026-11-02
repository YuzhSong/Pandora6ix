# PR #48 verification

## Initial failures and confirmed causes

The initial full backend run had 152 tests, with two failures. Android assembleDebug and 110 unit tests passed. All five PR OpenSpec changes and the 69-operation OpenAPI contract validated.

- `journalEditWindowAndEntryDeletion`: the fixture used `OffsetDateTime.now().minusDays(1)` at the exact expiry boundary. H2 2.5.252 stores the default TIMESTAMP WITH TIME ZONE at microsecond precision, which can round that value up by 500ns. Consecutive JVM clock reads can return the same instant. An isolated database probe invoking the actual PR service reproduced eligibility just before the rounded deadline; explicitly expired values were rejected. Fixed-instant service checks returned true/false/false at deadline minus 1ns, exact deadline and deadline plus 1ns.
- `backfilledJournalRemainsEditableAcrossMidnightOutsideCreationDateRange`: the shared `journal()` fixture uses a fixed Asia/Shanghai test date. It replaced the simulated enterprise's requested date with that fixed date. In the failed run, enterprise today was November 2, requested work date October 31, but the helper sent November 3, correctly rejected as a future date.

## Approved correction scope

Only these two integration fixtures change. The expired fixture uses 25 hours ago; exact boundary coverage remains in `JournalEditWindowTests`. The cross-timezone fixture creates its journal directly using the simulated enterprise date, leaving the shared historical-journal helper unchanged. No production implementation, business rule, permission, API or schema changes are introduced by the correction.

## Final validation

- Corrected full backend `./mvnw.cmd package`: 152 tests, 0 failures, 0 errors, 0 skipped; executable JAR built successfully.
- Android `:app:assembleDebug :app:testDebugUnitTest`: successful; 110 tests, 0 failures/errors/skipped. Runtime Android source was unchanged by the fixture correction.
- Strict validation passed for journal-rolling-edit-window, preserve-task-completion-records, notification-detail-return-list, journal-primary-tag-first and created-task-rejection-label.
- OpenAPI contract: 69 operations verified; git diff --check passed.
- Verification completed 2026-11-03. Test logs are retained outside the repository as pandora-pr48-fixed-backend.log and pandora-pr48-fixed-android.log in the local temporary directory. No device UI session or production data was changed during this merge verification.
