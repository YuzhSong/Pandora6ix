## ADDED Requirements

### Requirement: Home uses a cream static dashboard
Home SHALL use the shared cream content background below its unchanged navigation colors, show date text and a circular mail button in the header, and keep four panel previews static without inner scrolling.

#### Scenario: Open Home
- **WHEN** Home is displayed
- **THEN** the header shows the current month/day and mail affordance, and all four panel surfaces use the cream background

### Requirement: View date controls stay with each date row
Day, week, and month views SHALL place their previous/next arrows alongside the displayed date range rather than in the mode selector row.

#### Scenario: Change a week
- **WHEN** a user taps a week date-row arrow once
- **THEN** the adjacent week appears without requiring repeated taps

### Requirement: Return-to-today is fixed above navigation
When the active cursor differs from the demo today, View SHALL show a fixed left-rounded bookmark-like “回到今天” control above the bottom navigation.

#### Scenario: Return to today
- **WHEN** the fixed control is tapped
- **THEN** the active day/week/month cursor returns to the demo today

### Requirement: Week/month content enters day view first
When a user taps a date or task content in week or month view, the app SHALL switch to day view for the selected date before allowing task detail navigation.

#### Scenario: Select calendar content
- **WHEN** a week or month cell/task is tapped
- **THEN** day view opens for that date and lists its tasks/logs
