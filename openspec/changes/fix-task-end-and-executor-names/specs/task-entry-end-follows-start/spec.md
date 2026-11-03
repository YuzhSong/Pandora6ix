## ADDED Requirements

### Requirement: End time follows a later start
When the user changes the start date or start time in the task entry sheet and the end is no longer after the start, the Android client SHALL move the end forward by the previous duration (one hour, or the previous day span for all-day tasks, when there was no positive duration). An end that is still after the new start SHALL NOT change. For repeating tasks the series end date SHALL be at least the start date and each occurrence's end time SHALL stay within the same day (capped at 23:59).

#### Scenario: Start moved past end
- **WHEN** a task is 09:00–10:30 and the user changes the start to 14:00
- **THEN** the end becomes 15:30 on the same day.

#### Scenario: Crossing midnight
- **WHEN** a one-hour task's start is changed to 23:30
- **THEN** the end becomes 00:30 on the next day.

#### Scenario: Later end kept
- **WHEN** the end is the next day at 18:00 and the start changes to 14:00 today
- **THEN** the end stays at the next day 18:00.
