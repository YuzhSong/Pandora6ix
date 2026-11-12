## ADDED Requirements

### Requirement: Overdue message

The backend deadline scan SHALL emit one OVERDUE message per task deadline to each current recipient of an unfinished assignment whose deadline instant has passed, using the enterprise business timezone to derive the deadline date. The dedupe key SHALL include the task id and the deadline date so that each deadline produces at most one message per recipient, and extending the deadline re-arms the message for the new date. Tasks that are awaiting acceptance, completed, or rejected SHALL NOT produce overdue messages. The scan window SHALL be bounded to deadlines that passed within the last 24 hours so that a first deployment does not flood users with historic overdue messages.

#### Scenario: Deadline passes without completion

- **WHEN** the scheduled scan runs after a task's deadline instant has passed and its current assignments are still not completed
- **THEN** each current recipient receives one overdue message for that deadline, and a repeated scan does not create a second message

#### Scenario: Deadline extended

- **WHEN** the creator moves the deadline to a future date via the effective revision flow and the new deadline later passes
- **THEN** a new overdue message is emitted for the new deadline date only

#### Scenario: Overdue while awaiting acceptance

- **WHEN** a task's deadline passes while the task is AWAITING_ACCEPTANCE or COMPLETED
- **THEN** no overdue message is emitted

### Requirement: Overdue marking on Android

Android SHALL display a red 「已逾期」 marker for a task when the task is not completed and its deadline instant (end date plus optional end time in the enterprise business timezone) is earlier than the current instant. The marker SHALL appear on the home task board card, calendar view entries, and the company summary task table. Overdue marking SHALL NOT change task state, block any completion or acceptance action, or alter filtering and sorting behavior.

#### Scenario: Overdue task on home board

- **WHEN** the current user views a task card whose deadline passed and whose completion is not final
- **THEN** the card shows the red 「已逾期」 marker alongside its existing status

#### Scenario: Completed task is never marked overdue

- **WHEN** a task is COMPLETED or its deadline is in the future
- **THEN** no overdue marker is shown

#### Scenario: Timezone boundary

- **WHEN** the device timezone differs from the enterprise business timezone
- **THEN** overdue determination uses the enterprise business timezone, consistent with deadline reminders
