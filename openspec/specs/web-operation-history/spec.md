# web-operation-history Specification

## Purpose
Define company-scoped server operation-history filtering, readable immutable snapshots, real historical versions and complete Excel exports.
## Requirements
### Requirement: Filter server-owned operation history
The Web client SHALL query operation records by keyword, date range, module, action, source and result using fixed 20-row pages. Missing endpoints and failures SHALL show errors without sample fallback. A separately selected loopback development preview SHALL allow labelled sample logs in the full application, never in production. Filter changes SHALL reset pagination and superseded responses SHALL NOT replace current results.

#### Scenario: Backend unavailable
- **WHEN** the operation-history endpoint is missing or fails
- **THEN** the page reports the load failure and offers retry without showing sample records

#### Scenario: Change filters during loading
- **WHEN** an earlier request finishes after the administrator changes filters
- **THEN** only the current filter's response can populate the list

#### Scenario: Explicit local preview
- **WHEN** the user opens the full application in loopback development mode with the explicit sample-log flag
- **THEN** labelled sample logs support filtering, details and pagination while other modules use the real local backend, and production builds contain no sample data

### Requirement: Inspect readable historical snapshots
The client SHALL show compact summary rows and a keyboard-accessible detail dialog opening on complete before/after snapshots with colored text differences and no field-visibility toggle. Secondary metadata SHALL be collapsed and historical versions SHALL use a separate tab. Deleted-object history SHALL remain readable, sensitive fields SHALL be excluded and business mutation controls SHALL NOT be offered.

#### Scenario: Inspect a changed or deleted object
- **WHEN** an administrator opens a successful modification or deletion record
- **THEN** historical snapshots remain readable independently from the current business object, and changed fields are highlighted

#### Scenario: Open a modification detail
- **WHEN** an administrator opens a modification record
- **THEN** both full versions appear immediately, unchanged fields remain visible and changed text is distinguished without choosing a display mode

#### Scenario: Inspect unsuccessful operation
- **WHEN** an operation failed before any version was committed
- **THEN** its failure reason is shown without inventing a saved historical version

### Requirement: Export matching operation records
The client SHALL export the complete filtered summary result as an Excel workbook using a stable server snapshot, not just the visible page. It SHALL preserve human-readable values as text and reject repeated cursors, mismatched snapshots or incomplete responses rather than download partial results.

#### Scenario: Multiple pages of matching records
- **WHEN** the administrator exports a filtered result with several pages
- **THEN** every matching summary record from the same snapshot appears once in the workbook

### Requirement: Preserve administrator UI conventions
The history page SHALL use the current theme, shared selection controls, responsive tables and labelled dialogs. It SHALL provide loading, empty and error feedback without unsolicited explanatory product prose. Closing a dialog SHALL restore focus and session loss SHALL discard in-flight history data.

#### Scenario: Open detail with keyboard
- **WHEN** an administrator opens details and closes them with Escape
- **THEN** focus returns to the invoking control and the background remains scroll-locked while the dialog is open


### Requirement: Display real historical versions and captured names
The Web SHALL display real non-null snapshot versions returned by the backend, including valid version zero, and SHALL label missing historical versions as unrecorded rather than manufacture a version. History navigation SHALL use the server-provided same-enterprise, same-object successful history; failed operations SHALL NOT become committed versions. Captured historical organization/person names SHALL take precedence over current directory names. Uncaptured legacy organization or supervisor names SHALL be labelled as historically unrecorded rather than substituted from mutable directory data; other current-reference formatting SHALL NOT rewrite the snapshot.

#### Scenario: Historical version was not captured
- **WHEN** an old snapshot has a null version
- **THEN** the dialog shows an unrecorded-version label without converting null into zero

#### Scenario: Organization was renamed after an operation
- **WHEN** a snapshot includes the organization name captured during that operation
- **THEN** the dialog preserves that name even if the current directory name differs
