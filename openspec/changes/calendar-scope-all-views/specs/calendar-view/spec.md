## ADDED Requirements

### Requirement: Task scope selection in every calendar mode
The Android calendar SHALL show 我的任务 and 我创建的 in day, week, month, and year views, and SHALL show 我管理的 only when the user has review capability. All modes SHALL load tasks matching the selected scope and period, retain scope across mode changes, and apply existing tag filters to the resulting tasks. Managed scope permission failures SHALL fall back to mine through the existing permission handling.

#### Scenario: Select created tasks
- **WHEN** the user selects 我创建的 in any mode
- **THEN** tasks are loaded with created scope for that mode's date range

#### Scenario: Permission-aware managed tasks
- **WHEN** a user without review capability opens any calendar mode
- **THEN** 我管理的 is hidden and a restored managed selection returns to 我的任务

#### Scenario: Change mode
- **WHEN** the user switches modes after selecting a scope
- **THEN** the selected scope remains active and the new mode loads that scope for its period

#### Scenario: Narrow display
- **WHEN** scope controls exceed the available horizontal space
- **THEN** their labels remain on one line and can be reached by horizontal scrolling
