## MODIFIED Requirements

### Requirement: Daily unique journals and stable work entries
The backend SHALL publish at most one journal per author/date, return existingJournalId on duplicate, and provide authorized lists/details and entry detail. New journals SHALL use only today, yesterday or the day before yesterday in the enterprise business timezone. Only the author SHALL edit or delete a journal whose workDate equals the current enterprise business date; yesterday/day-before backfills SHALL become immutable immediately after submission. Patches SHALL preserve entry IDs, keep omitted entries, and atomically update entries/tags with expectedVersion. Explicit entry or journal deletion SHALL atomically remove personal-important references and advance any affected manual selection version without selecting replacement entries. Journal responses SHALL expose businessToday and author-scoped EDIT/DELETE allowedActions.

#### Scenario: Partial patch preserves other work
- **WHEN** an author modifies one entry and adds another to today's journal
- **THEN** the original entry ID remains, omitted entries survive, and one aggregate version advances.

#### Scenario: Backfill is one submission only
- **WHEN** an author submits a missing journal for yesterday or the day before yesterday
- **THEN** POST succeeds once; repeated creation returns 409 DAILY_JOURNAL_EXISTS with existingJournalId, while PATCH and DELETE return 409 JOURNAL_EDIT_WINDOW_EXPIRED immediately.

#### Scenario: Company midnight closes editing
- **WHEN** the company's current business date changes after a journal was submitted
- **THEN** that journal becomes read-only even if less than 24 hours elapsed, with no dependence on a client timezone or publishedAt duration.

#### Scenario: Invalid create date
- **WHEN** an author creates a journal outside the last three company business dates, including a future date
- **THEN** the server returns 422 JOURNAL_DATE_OUT_OF_RANGE without writing journal or entries.

#### Scenario: Historical read remains available
- **WHEN** an authorized user reads existing older journals or D through D-9 important-entry candidates
- **THEN** the create/edit restriction does not remove historical data or shorten read/selection ranges.
