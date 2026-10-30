## ADDED Requirements

### Requirement: Explicit schedule meaning
Creation SHALL distinguish continuous intervals and repeating schedules, independently of all-day timing, and display a readable preview. Legacy tasks without recurrence SHALL retain continuous semantics.

#### Scenario: Continuous example
- **WHEN** a user selects no repetition from Jan 1 09:00 to Sep 10 10:00
- **THEN** the summary describes one uninterrupted interval

#### Scenario: Repeating example
- **WHEN** a user selects daily repetition over the same dates with times 09:00–10:00
- **THEN** each occurrence covers only its date's 09:00–10:00 and has an independent completion state

### Requirement: Bounded deterministic recurrence
The server SHALL generate dates inclusively for DAILY, WEEKLY and MONTHLY frequencies with intervals 1..365. Weekly rules SHALL require ISO weekdays and anchor to the starting week's Monday. Monthly rules SHALL skip months lacking the anchor day. At most 366 occurrences over at most ten years SHALL be accepted, and timed occurrences SHALL end later on the same day.

#### Scenario: Month ends
- **WHEN** monthly repetition starts Jan 31
- **THEN** February is skipped and March occurs on March 31

#### Scenario: Week interval
- **WHEN** every two weeks on Monday and Friday is selected
- **THEN** only those weekdays in alternating weeks inside the date range are generated

### Requirement: Atomic independent task lifecycle
Recurrence creation SHALL create independent task/assignment/review records in one transaction under the existing authorization, tenant, idempotency and approval rules. Occurrences SHALL retain a common series ID and readable rule label. Edits and completion SHALL affect only the selected occurrence, and UI SHALL disclose separate approvals and edits.

#### Scenario: Completion isolation
- **WHEN** today's occurrence is completed
- **THEN** tomorrow's assignment remains unchanged and calendar/reminders use each occurrence's own dates

#### Scenario: Failure or retry
- **WHEN** any occurrence fails validation or a creation request is retried
- **THEN** no partial batch is persisted and the same idempotency key does not create duplicate occurrences
