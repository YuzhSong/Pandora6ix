## ADDED Requirements

### Requirement: Latest progress note in task views
Task list and detail views SHALL include nullable `latestProgressNote`: the note of the most recent progress record with a non-blank note among the assignments authorized for the current user in that view, ordered by recorded time then ID. It SHALL NOT expose notes from assignments outside `authorizedAssignments`.

#### Scenario: Two executors with notes
- **WHEN** a creator lists a delegated task whose two executors both recorded progress notes
- **THEN** the task item's latestProgressNote is the newer note

#### Scenario: Executor sees only their own assignment
- **WHEN** an executor who is not the creator lists the same task
- **THEN** latestProgressNote comes only from that executor's own assignment
