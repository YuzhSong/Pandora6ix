## ADDED Requirements

### Requirement: Week tasks render as continuous date-aligned bars
The week view SHALL render one multi-day task as one continuous colored bar spanning its inclusive start/end dates, with separate lanes for overlapping tasks.

#### Scenario: Multi-day task
- **WHEN** a task begins on one day and ends several days later in the selected week
- **THEN** its bar remains visually connected across those date columns and opens the same task detail when tapped

### Requirement: View navigation uses correct date ranges
The month view SHALL move exactly one calendar month per arrow, and the week heading SHALL display only its date range without a leading year/month label.

#### Scenario: Change month
- **WHEN** the user taps the month previous or next arrow
- **THEN** the displayed month changes by exactly one month

### Requirement: View can return to today
When the selected cursor is not the demo today, the active day/week/month view SHALL show a “回到今天” action that restores the corresponding range containing today.

#### Scenario: Return to today
- **WHEN** the user taps “回到今天”
- **THEN** the active view remains selected and its date cursor becomes 2026年9月19日
