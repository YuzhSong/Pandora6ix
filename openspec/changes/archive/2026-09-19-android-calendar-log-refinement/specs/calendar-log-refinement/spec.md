## ADDED Requirements

### Requirement: Dates include weekday context
Home and Day view date labels SHALL include the corresponding Chinese weekday.

#### Scenario: Read a date label
- **WHEN** Home or Day view is displayed
- **THEN** the date label includes both month/day and a `周一` through `周日` weekday suffix

### Requirement: Week view guides task spans
Week view SHALL show light dashed day-column guides behind continuous task bars and use the same readable date typography as the other calendar modes.

#### Scenario: Inspect a week
- **WHEN** Week view is displayed
- **THEN** each day column has a light dashed guide and multi-day tasks remain one continuous bar across their covered columns

### Requirement: Month view preserves task continuity
Month view SHALL render a multi-day task as a continuous bar within each week segment and SHALL show a `+N条计划` label for a day with more than two overlapping tasks.

#### Scenario: Inspect a busy month
- **WHEN** a month contains a task spanning multiple days or more than two tasks on one day
- **THEN** the task is shown as a weekly continuous bar and the day shows a `+N条计划` overflow label

### Requirement: Logs provides a first-pass workflow
The Logs page SHALL provide switchable Work Logs and Task Management sections using shared mock data, with clickable cards and a mock record action.

#### Scenario: Use the Logs page
- **WHEN** the user opens 日志 and switches between the two sections
- **THEN** mock logs or tasks are listed, cards open the existing detail route, and the record action opens a mock entry route
