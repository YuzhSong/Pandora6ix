# Validation

## Backend

- First full Maven run executed 136 tests: 135 passed, with one migration-count assertion failure (new V12 makes two upgrades from V10). The assertion and legacy-null metadata checks were updated.
- New coverage: deterministic daily/weekly/monthly intervals, month-end/leap-day skipping, invalid ranges, caps, independent completion, separate approval snapshots, cross-user visibility and idempotent retry.
- A later-occurrence database failure test checks complete transactional rollback. Its temporary constraint is scoped to the fixture enterprise so earlier test fixtures do not interfere.
- Final focused Maven run: PASS, 10 tests (4 recurrence integration, 5 generation, 1 migration). This includes the repaired migration assertion and enterprise-scoped late-failure rollback test. Full suite was not repeated after these fixes; the previous 135 passing cases remain the baseline.

## Android

- Added RecurrenceRuleTest for daily intervals, alternate weeks, missing month days and invalid/unbounded rules.
- Build request was rejected by the user. No alternative compilation was attempted; Android compilation, test execution and visual checks remain pending.

## Contracts

- OpenAPI generator check: PASS (65 operations).
- Strict OpenSpec validation: PASS.
- git diff --check: PASS.
- Backend deployment and V12 migration must precede Android release. No deployment performed.


## Follow-up verification
During refine-repeat-and-full-day-display, Android compileDebugUnitTestKotlin and assembleDebug passed. RecurrenceRuleTest passed as part of 33 JUnitCore tests. Previous Android build restriction is now resolved for compilation and unit tests; device visual verification remains pending.
