# Spec Delta

## MODIFIED Requirements

### Requirement: Authorized review history and current pending queue
The backend SHALL expose paginated pending reviews to the designated direct-supervisor reviewer and submitted review history to its submitter. Detail SHALL return the immutable submitted snapshot, attempt, review/task versions, currentness and allowed actions. Other users and enterprises SHALL receive uniform 404 for detail; administrative privilege alone SHALL NOT grant review access. Pending queues SHALL exclude superseded attempts; cursors SHALL bind actor, enterprise and scope.

The backend SHALL expose an authenticated pending-count endpoint for the current actor. It SHALL count exactly the records that would appear in that actor's `scope=pending` queue, excluding superseded or stale task versions, and SHALL return zero when no reviews are pending. Actors without review authority SHALL be denied consistently with the pending queue. The count SHALL be recalculated on each request so decisions and task edits are reflected without a database migration or client-side detail download.

New submissions and edits SHALL use the same snapshot command fields, including recipientIds and tags, without mutable UI/action metadata. Previously stored snapshots SHALL remain unchanged.

#### Scenario: Reviewer reads pending count
- **WHEN** an authorized reviewer requests the pending-count endpoint
- **THEN** the response contains the number of current pending review records for that reviewer

#### Scenario: Pending review is decided or superseded
- **WHEN** a reviewer decides a pending review or a task edit creates a newer review attempt
- **THEN** the next pending-count request excludes the decided or superseded record

#### Scenario: User without review authority requests count
- **WHEN** an employee or ADMIN-only account requests the pending-count endpoint
- **THEN** the backend denies the request using the same authorization policy as the pending review queue

#### Scenario: Reviewer has no pending reviews
- **WHEN** an authorized reviewer has no current pending review records
- **THEN** the response reports a count of zero

#### Scenario: Task edited before review
- **WHEN** the submitter edits a pending task
- **THEN** a new review attempt appears in the pending queue, the old snapshot remains unchanged in authorized history and the old review has no decision actions

#### Scenario: Unrelated account opens review
- **WHEN** another employee or enterprise supplies a review ID
- **THEN** it cannot read the snapshot or approve the task
