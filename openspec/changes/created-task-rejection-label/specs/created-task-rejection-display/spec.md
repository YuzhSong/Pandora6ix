## ADDED Requirements

### Requirement: Rejected created tasks have explicit labels
Android SHALL visibly label tasks whose reviewStatus is REJECTED as “被驳回” in the created-task calendar and task summary. Calendar views and overlapping-task selection SHALL display the rejection label before the title. Other review statuses SHALL NOT be labeled rejected merely because an execution assignment is cancelled.

#### Scenario: Rejected task in created scope
- **WHEN** the user views a rejected task in “我创建的”
- **THEN** the task visibly displays “被驳回” without opening its detail.

#### Scenario: Other review states
- **WHEN** a task is pending or approved
- **THEN** no rejection label is added.
