## ADDED Requirements

### Requirement: Home provides refresh and personal filtering affordances
Home SHALL show a small refresh button aligned to the right of 工作总览 and an edit button on 个人十大重要事项.

#### Scenario: Use Home affordances
- **WHEN** the user opens Home
- **THEN** refresh is visible beside 工作总览 and edit is visible on 个人十大重要事项

### Requirement: Today log action follows entries
Logs SHALL render today’s entries first, then the 记录今天的工作 action; newly created demo entries SHALL appear above that action.

#### Scenario: Add today log
- **WHEN** the user publishes a new today log
- **THEN** it appears above the record button in 今日工作日志

### Requirement: Task management entries are neutral
Task dispatch and pending-review entry cards SHALL use white backgrounds.

#### Scenario: Browse task management
- **WHEN** the user opens 任务管理
- **THEN** the dispatch and pending-review cards use white backgrounds
