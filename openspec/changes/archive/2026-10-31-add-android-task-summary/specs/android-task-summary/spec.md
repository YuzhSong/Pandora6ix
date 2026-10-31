## ADDED Requirements

### Requirement: Compact calendar entry and preserved navigation
The client SHALL place a compact task-summary entry at the right of the existing calendar top control row, keep exactly the five existing bottom tabs, and preserve calendar date, mode, filters and reasonable scroll position when returning.

#### Scenario: Return to calendar
- **WHEN** a user opens summary from a filtered calendar and returns
- **THEN** the prior selection and scroll state are restored without adding a title row or navigation tab

### Requirement: Three scoped panels
The summary SHALL contain only task list, direct reports and recent journals, use the existing theme, and default to mine. Its three task tabs SHALL map to mine, created and managed, preserving the latter's subordinate creator-or-executor semantics. It SHALL provide loading, empty, error/retry and pagination states independently and SHALL show no metrics or charts.

#### Scenario: Switch task scope
- **WHEN** a user selects another task tab
- **THEN** only its task collection changes and the authenticated scoped API provides the data

#### Scenario: Account cannot manage
- **WHEN** a business user without manage capability opens summary
- **THEN** the subordinate tab explains the unavailable permission, no managed or candidate request is made, and the other two task tabs and own journals remain available

### Requirement: Real task content and existing details
Task rows SHALL show title, server tags, real priority, review/execution state, deadline and actual authorized executor avatars; opening a row SHALL load the existing full detail by stable ID with existing action authorization.

#### Scenario: Multiple executors
- **WHEN** a task has multiple authorized assignments
- **THEN** its row uses recipientUserId for executor avatars and does not present the creator as executor

### Requirement: Authorized people and organization
The direct-report panel SHALL be a single horizontal row of candidate records marked DIRECT, with more linking to an expandable organization page constructed only from authorized server department/team IDs. Members SHALL open a detail with available identity fields, responsible tasks and recent journals. Unsupported contact/rank/reporting data SHALL be disclosed without fabrication or admin API access.

#### Scenario: Partial candidate page
- **WHEN** more candidate pages exist but no DIRECT person has been loaded
- **THEN** the UI discloses the partial result and permits loading more rather than claiming the user has no direct reports

#### Scenario: Member responsibility
- **WHEN** a member detail filters authorized tasks
- **THEN** only tasks with that member in authorizedAssignments are shown, with further-page controls when the collection is partial

### Requirement: Recent journals and safe state
Recent journals SHALL display actual author avatar/name or explicit ID fallback, business time and one-line summary, open existing journal detail, and offer a full paginated list retaining scope. Member and detail navigation SHALL preserve parent page state, and account changes SHALL clear feature data.

#### Scenario: Member without loaded journals
- **WHEN** the loaded scoped pages contain no journal from the selected member and nextCursor exists
- **THEN** the UI offers continued paging and does not claim no historical journals exist

#### Scenario: Request failure
- **WHEN** a request fails or server access is denied
- **THEN** the existing ApiClient session/permission processing applies, the affected panel has retry, and no mock records or broader-scope fallback are used
