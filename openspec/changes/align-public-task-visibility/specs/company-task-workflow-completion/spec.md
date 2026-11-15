## MODIFIED Requirements

### Requirement: Explicit company-wide tasks
The system SHALL allow only FOUNDER business users to publish explicitly company-wide delegated tasks. Approved company-wide tasks SHALL appear in the company panel, the personal task list and the calendar of business readers in the same enterprise. The publisher SHALL explicitly select the executors. Legacy and ordinary delegated tasks SHALL remain private. Public reading SHALL NOT grant write, approval or execution rights.

#### Scenario: Private delegation
- **WHEN** a manager delegates a normal task to an employee
- **THEN** it is absent from company panels, while remaining visible in authorized personal/task views

#### Scenario: Public task isolation
- **WHEN** a founder publishes a company-wide task
- **THEN** same-enterprise business readers can list it in the company panel, personal task list and calendar and read its details without becoming executors, and other enterprises cannot access it

### Requirement: Complete editable information panel
Task detail SHALL place task information in one white segmented panel with an authorized edit action and execution in a second panel. Creators SHALL edit all user-supplied task information through a complete form. Non-founder changes SHALL require the configured superior's approval before replacing effective data. Founder behavior SHALL follow the established top-level approval exception. Android SHALL label founder creation "发布任务" and render the creation time and optional latest effective edit time in the enterprise time zone. Android SHALL label the task's acceptance-pending state "待验收".

#### Scenario: Awaiting edit approval
- **WHEN** a creator submits changed content, dates, priority, tags or executors
- **THEN** the superior reviews that proposed version and the current effective task stays unchanged until approval

#### Scenario: Read latest edit time
- **WHEN** a reader opens a task after an effective edit
- **THEN** the detail shows creation time and the latest effective edit time in the enterprise time zone

## ADDED Requirements

### Requirement: Complete execution information for readable tasks
Task views SHALL include all current execution relationships for a task the reader is authorized to see. The latest nonblank progress note and detail history SHALL use that collection. The service SHALL determine progress, completion and acceptance actions for each relationship from the reader's authorization and the task state.

#### Scenario: Read execution as a non-executor
- **WHEN** an employee who is neither an executor nor an approver reads an approved company-wide task
- **THEN** the employee sees all current executors and their execution information and receives no actions for those relationships
