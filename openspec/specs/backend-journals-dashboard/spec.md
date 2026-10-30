# backend-journals-dashboard Specification

## Purpose
Define author-protected daily journals, stable work entries, explicit important-entry selections, enterprise company items and four-panel dashboard/basic statistics.
## Requirements
### Requirement: Daily unique journals and stable work entries
The backend SHALL publish at most one journal per author/date, return existingJournalId on duplicate, and provide authorized lists/details and entry detail. New journals SHALL use only today, yesterday or the day before yesterday in the enterprise business timezone. Only the author SHALL edit or delete a journal whose creation timestamp (published_at), converted to the enterprise business timezone, falls on the current business date. Backfills for yesterday or the day before yesterday SHALL remain editable and deletable throughout their creation day, independently of workDate; all journals SHALL become read-only after their creation day. Patches SHALL preserve entry IDs, keep omitted entries, and atomically update entries/tags with expectedVersion. Explicit entry or journal deletion SHALL atomically remove personal-important references and advance any affected manual selection version without selecting replacement entries. Journal responses SHALL expose businessToday and author-scoped EDIT/DELETE allowedActions.

#### Scenario: Partial patch preserves other work
- **WHEN** an author modifies one entry and adds another to today's journal
- **THEN** the original entry ID remains, omitted entries survive, and one aggregate version advances.

#### Scenario: Backfill remains editable on its creation day
- **WHEN** an author submits a missing journal for yesterday or the day before yesterday
- **THEN** POST succeeds once; repeated creation returns 409 DAILY_JOURNAL_EXISTS with existingJournalId, while the author can PATCH or DELETE during the creation business day and receives EDIT/DELETE allowedActions.

#### Scenario: Earlier creation day is read-only
- **WHEN** an author attempts to patch or delete a journal created on an earlier enterprise business date
- **THEN** the server returns 409 JOURNAL_EDIT_WINDOW_EXPIRED and exposes no edit/delete actions, regardless of workDate.

#### Scenario: Company midnight closes editing
- **WHEN** the company's current business date changes after a journal was submitted
- **THEN** that journal becomes read-only even if less than 24 hours elapsed, with no dependence on a client timezone or publishedAt duration.

#### Scenario: Invalid create date
- **WHEN** an author creates a journal outside the last three company business dates, including a future date
- **THEN** the server returns 422 JOURNAL_DATE_OUT_OF_RANGE without writing journal or entries.

#### Scenario: Historical read remains available
- **WHEN** an authorized user reads existing older journals or D through D-9 important-entry candidates
- **THEN** the create/edit restriction does not remove historical data or shorten read/selection ranges.

### Requirement: Complete author journal date index
The backend SHALL expose GET /work-logs/dates with every work date having a journal for the authenticated author in the same enterprise and businessToday. This date-only index SHALL NOT be restricted by the 367-day list query window and SHALL NOT expose other authors' dates.

#### Scenario: Historical date picker
- **WHEN** an author requests their journal date index
- **THEN** all existing dates are returned with the enterprise business date, including dates older than the list query window.

### Requirement: Important entry selection
The backend SHALL return the author's published entries from business today through D-9 as candidates, permit up to ten selected entries, persist an explicit manual empty set, and use initial version zero. Without a manual state, at most ten candidates SHALL be selected by default; more than ten SHALL require selection. Existing selections SHALL NOT be silently deleted/replaced when they expire.

#### Scenario: Empty manual selection persists
- **WHEN** a user saves an empty selection and reloads it
- **THEN** hasManualSelection remains true and the empty set is retained.

#### Scenario: Invalid new selection is atomic
- **WHEN** a save introduces an expired or other author's entry
- **THEN** the entire selection is rejected without changing the previous set or version.

### Requirement: Company items and four-panel dashboard
The backend SHALL expose enterprise company item lists/full detail and ADMIN/FOUNDER maintenance with versions. The dashboard SHALL provide four panels with itemId/taskId/entryId references and own approved-assignment total/completed/uncompleted statistics. Department publication without an explicit restricted scope SHALL return a policy error.

#### Scenario: Empty dashboard
- **WHEN** an authorized business user has no tasks or entries
- **THEN** four panels return empty data and basic statistics return zero without fabricated totals.
