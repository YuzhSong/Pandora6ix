## MODIFIED Requirements

### Requirement: Daily unique journals and stable work entries

The backend SHALL publish at most one journal per author/date, return existingJournalId on duplicate, and provide authorized lists/details and entry detail. Only the author SHALL edit; patches SHALL preserve entry IDs, keep omitted entries, and atomically update entries/tags with expectedVersion.

The edit/delete window SHALL be decided by the journal's creation time (`published_at`) converted to the company business timezone, SHALL NOT be decided by `work_date`: a journal created during the current business day SHALL remain editable and deletable for that whole day, including backfilled journals whose `work_date` is yesterday or the day before; once the creation day has passed it SHALL become read-only. `allowedActions` SHALL expose `EDIT`/`DELETE` under exactly this rule.

The backend SHALL expose `GET /work-logs/dates` returning every business date that has a journal for the caller, together with `businessToday`. It SHALL NOT be limited by the 367-day window applied to `GET /work-logs`.

#### Scenario: Partial patch preserves other work
- **WHEN** an author modifies one entry and adds another
- **THEN** the original entry ID remains, omitted entries survive, and one aggregate version advances.

#### Scenario: Backfilled journal stays editable on its creation day
- **WHEN** an author submits a journal for yesterday and then patches it in the same business day
- **THEN** `allowedActions` contains `EDIT`/`DELETE` and the patch succeeds

#### Scenario: Journal created on an earlier day is read-only
- **WHEN** a journal's creation time falls on a previous business day
- **THEN** `allowedActions` is empty and both patch and delete return `409 JOURNAL_EDIT_WINDOW_EXPIRED`

#### Scenario: Journal dates for the date picker
- **WHEN** an author calls `GET /work-logs/dates`
- **THEN** the response lists that author's journal dates (all history, other authors excluded) and `businessToday`
