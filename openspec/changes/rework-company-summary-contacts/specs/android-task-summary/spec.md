## MODIFIED Requirements

### Requirement: Three scoped panels
The summary SHALL contain only tasks: it SHALL NOT show direct reports or journals, use the existing theme, and default to mine. Its three task tabs SHALL map to mine, created and managed, preserving the latter's subordinate creator-or-executor semantics. It SHALL provide loading, empty, error/retry and pagination states and SHALL show no metrics or charts.

#### Scenario: Switch task scope
- **WHEN** a user selects another task tab
- **THEN** only the task table changes and the authenticated scoped API provides the data

#### Scenario: Account cannot manage
- **WHEN** a business user without manage capability opens summary
- **THEN** the subordinate tab explains the unavailable permission, no managed request is made, and the other two task tabs remain available

### Requirement: Real task content and existing details
Tasks SHALL be displayed as a table with the columns 任务名称, 截止日期, 紧急程度, 完成情况, 进度备注 and 负责人, one row per task, using server title, deadline, real priority, review/execution state, latest progress note and actual authorized executor names. The 任务名称 column and the header row SHALL stay fixed while the remaining columns scroll horizontally together for all rows. Rows SHALL be grouped under primary-tag group rows. Opening a row SHALL load the existing full detail by stable ID with existing action authorization. A landscape action SHALL let the user view all columns at once and SHALL restore portrait when leaving the summary.

#### Scenario: Scroll columns on a phone
- **WHEN** a user swipes the table horizontally in portrait
- **THEN** every row and the header scroll together while task names stay visible on the left

#### Scenario: Multiple executors
- **WHEN** a task has multiple authorized assignments
- **THEN** its 负责人 cell lists the recipients' names and does not present the creator as executor

#### Scenario: Progress note
- **WHEN** an authorized assignment of a task has a progress record with a note
- **THEN** the 进度备注 cell shows the most recent such note, otherwise the average visible progress or a dash

### Requirement: Authorized people and organization
People and organization content SHALL be provided by the contacts destination, not by the task summary.

#### Scenario: Summary without people
- **WHEN** a manager opens the task summary
- **THEN** no direct-report panel or organization link is shown and contacts remains reachable from the company page

### Requirement: Recent journals and safe state
Journal content SHALL be provided by the contacts destination, not by the task summary. Task detail navigation SHALL preserve the summary table's tab and scroll state, and account changes SHALL clear feature data.

#### Scenario: Return from task detail
- **WHEN** a user opens a task from the table and returns
- **THEN** the same tab, loaded pages and scroll position are shown

#### Scenario: Request failure
- **WHEN** a request fails or server access is denied
- **THEN** the existing ApiClient session/permission processing applies, the table offers retry, and no mock records or broader-scope fallback are used
