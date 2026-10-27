# Spec Delta

## MODIFIED Requirements

### Requirement: Shared enterprise tags and pending task submission
The backend SHALL expose shared tag queries, authorized creation/maintenance, task lists/details/recipient candidates and task submission. PERSONAL SHALL have only the creator as recipient; DELEGATED SHALL accept only lower members in management scope. Tag filters SHALL use AND, and primaryTagId SHALL belong to the same enterprise tag set. Non-founder creation SHALL store a direct-supervisor review snapshot without automatic approval. FOUNDER creation SHALL not require a supervisor and SHALL create an approved task with active assignments immediately.

#### Scenario: Founder creates a task
- **WHEN** a business user with the FOUNDER role creates a valid personal or delegated task
- **THEN** the task is approved immediately, its assignments are active, and no pending supervisor review is required.

#### Scenario: Missing supervisor
- **WHEN** a business user without a direct supervisor submits a task
- **THEN** WORKFLOW_POLICY_UNCONFIRMED prevents creation of a self-approved task.

#### Scenario: Mixed authorized recipients
- **WHEN** one delegated recipient is outside management scope
- **THEN** no task or assignments are persisted.
