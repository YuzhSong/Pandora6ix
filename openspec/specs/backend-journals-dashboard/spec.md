# backend-journals-dashboard Specification

## Purpose
Define author-protected daily journals, stable work entries, explicit important-entry selections, enterprise company items and four-panel dashboard/basic statistics.
## Requirements
### Requirement: Daily unique journals and stable work entries
The backend SHALL publish at most one journal per author/date, return existingJournalId on duplicate, and provide authorized lists/details and entry detail. Only the author SHALL edit; patches SHALL preserve entry IDs, keep omitted entries, and atomically update entries/tags with expectedVersion.

#### Scenario: Partial patch preserves other work
- **WHEN** an author modifies one entry and adds another
- **THEN** the original entry ID remains, omitted entries survive, and one aggregate version advances.

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
