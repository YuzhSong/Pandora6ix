# PR 17 review validation (2026-10-18)

Review fixes restrict deadline scanning to APPROVED tasks and ACTIVE / AWAITING_ACCEPTANCE assignments, notify both creator and recipient for execution events, retain real progress transitions back to an earlier percentage, and absorb concurrent duplicate notification inserts without hiding unrelated errors.

- Java 21 `backend/mvnw.cmd -B -ntp package`: 65 tests passed, no failures/errors/skips; packaged artifact produced.
- Regression coverage includes pending/rejected task privacy, completed-task exclusion, both-party status delivery, repeated equal progress versus 50→75→50, personal-task deduplication and eight concurrent duplicate inserts.
- `python backend/scripts/generate-openapi.py --check`: 49 operations verified.
- All five PR 17 OpenSpec changes passed strict validation before merge.

Android still derives foreground reminders from task snapshots; consuming `/notifications` is deferred. Manual device acceptance remains unchecked in the respective tasks files; no physical-device result is claimed.
