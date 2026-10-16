# backend-task-review Specification

## Purpose
Define authorized immutable task review history and direct-supervisor decisions that atomically activate task assignments.

## Requirements
### Requirement: Authorized review history and current pending queue
The backend SHALL expose paginated pending reviews to the designated direct-supervisor reviewer and submitted review history to its submitter. Detail SHALL return the immutable submitted snapshot, attempt, review/task versions, currentness and allowed actions. Other users and enterprises SHALL receive uniform 404 for detail; administrative privilege alone SHALL NOT grant review access. Pending queues SHALL exclude superseded attempts; cursors SHALL bind actor, enterprise and scope.

New submissions and edits SHALL use the same snapshot command fields, including recipientIds and tags, without mutable UI/action metadata. Previously stored snapshots SHALL remain unchanged.

#### Scenario: Task edited before review
- **WHEN** the submitter edits a pending task
- **THEN** a new review attempt appears in the pending queue, the old snapshot remains unchanged in authorized history and the old review has no decision actions

#### Scenario: Unrelated account opens review
- **WHEN** another employee or ADMIN-only account supplies a review ID
- **THEN** it cannot read the snapshot or approve the task

### Requirement: Atomic direct-supervisor decision
The backend SHALL accept APPROVE or REJECT only from the designated current business supervisor, with expectedVersion, an active submitter, a current pending content version and valid active recipients. A stale version, superseded review or changed hierarchy SHALL reject the entire command. APPROVE SHALL atomically persist audit actor/comment/time, advance review/task versions and activate every pending assignment; REJECT SHALL retain its reason and keep all recipients inactive. A task SHALL NOT become effective merely because it was submitted or reaches progress 100.

#### Scenario: Approve multi-recipient delegation
- **WHEN** the current direct supervisor approves an unchanged valid delegated task
- **THEN** every recipient assignment becomes ACTIVE with an activation time and new version, and the creator can observe the approval result through public APIs

#### Scenario: Outdated review or recipient
- **WHEN** the reviewed content changed or an intended recipient is disabled or outside the creator's management scope
- **THEN** no approval audit, task status or assignment is partially updated

### Requirement: Repeatable decisions and genuine lifecycle acceptance
Review commands SHALL support the existing Idempotency-Key contract; exact repeats SHALL return the original result and altered commands with the same key SHALL conflict. Concurrent decisions SHALL produce exactly one persisted transition. API acceptance SHALL create, approve, progress and complete PERSONAL tasks and create, approve and individually accept DELEGATED assignments without directly seeding approved database state.

#### Scenario: Concurrent approval and rejection
- **WHEN** two decisions race with the same expected review version
- **THEN** exactly one succeeds and the other reports a conflict without overriding the first decision
