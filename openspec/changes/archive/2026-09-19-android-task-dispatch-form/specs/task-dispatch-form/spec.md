## ADDED Requirements

### Requirement: Dispatch captures priority and notes
The task dispatch overlay SHALL provide a task name, a one-to-five-star priority selector, and a notes/description field.

#### Scenario: Set dispatch details
- **WHEN** a manager opens 任务派发
- **THEN** the priority control appears directly below the task name and the notes field provides enough height for multi-line guidance

### Requirement: Dispatch recipients are multi-select and scrollable
The task dispatch overlay SHALL show managed direct and lower-level members in a vertically scrollable multi-select list.

#### Scenario: Select multiple recipients
- **WHEN** the manager taps recipient rows
- **THEN** each row toggles independently, at least three rows are visible without scrolling, and additional rows can be reached by scrolling

### Requirement: Form fields use neutral colors
Editable fields in task dispatch and today-log overlays SHALL use gray borders, labels, and cursor colors rather than the coral accent.

#### Scenario: Review form styling
- **WHEN** the user opens either overlay
- **THEN** the input controls use the neutral gray palette while primary actions remain visually distinct
