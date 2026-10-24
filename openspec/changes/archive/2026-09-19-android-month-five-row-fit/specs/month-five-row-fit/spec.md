## ADDED Requirements

### Requirement: Month fits five equal rows without scrolling
Month view SHALL display five equal-height week rows in the available content area without vertical scrolling.

#### Scenario: Open Month view
- **WHEN** the user opens Month view
- **THEN** all five week rows are visible at once and each row has the same height

### Requirement: Month previews two tasks per week
Each Month week SHALL show at most two task bars directly and use a third equal slot for `+N条计划` when more tasks are present.

#### Scenario: Busy week
- **WHEN** a week contains more than two visible tasks
- **THEN** the first two tasks occupy the first two slots and the third slot displays the total visible task count minus two
