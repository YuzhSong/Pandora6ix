## ADDED Requirements

### Requirement: Company page shows only own tasks
The company page SHALL NOT show the 我的任务 / 我管理的 / 我创建的 scope selector. All calendar modes SHALL request and display only the current user's own tasks (scope mine). Subordinate and created tasks SHALL remain available in the task summary tabs.

#### Scenario: Manager opens the company page
- **WHEN** a manager opens the company page
- **THEN** no scope selector is shown and the calendar displays only that manager's own tasks
