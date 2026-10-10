## ADDED Requirements

### Requirement: Month weeks have a fixed rhythm
Every rendered Month week SHALL use the same fixed height and SHALL display no more than three task bars directly.

#### Scenario: Inspect a busy month
- **WHEN** a week has more than three visible tasks
- **THEN** its row remains the same height as other weeks and shows a `+N条计划` overflow summary for the additional tasks

### Requirement: Month drills into Week
Selecting any date or task content in Month view SHALL switch to Week view with that date as the cursor.

#### Scenario: Select a Month week
- **WHEN** the user taps a Month date or task bar
- **THEN** the app opens Week view for the containing week

### Requirement: Week keeps Day drill-down
Selecting a date or task content in Week view SHALL continue to switch to Day view.

#### Scenario: Select a Week date
- **WHEN** the user taps a Week date or task bar
- **THEN** the app opens Day view for the selected date
