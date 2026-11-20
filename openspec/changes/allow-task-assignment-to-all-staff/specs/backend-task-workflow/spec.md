# Spec Delta

## MODIFIED Requirements

### Requirement: Shared enterprise tags and pending task submission
The backend SHALL expose shared tag queries, authorized creation/maintenance, task lists/details/recipient candidates and task submission. PERSONAL SHALL have only the creator as recipient. DELEGATED SHALL accept any active business member of the creator's enterprise regardless of organization hierarchy or rank, including the creator themself alone (self-only delegation is allowed); disabled accounts, foreign enterprises and non-business users SHALL remain rejected. Any business user (including EMPLOYEE) SHALL be able to create delegated tasks, subject to the existing direct-supervisor review for non-founders. Tag filters SHALL use AND, and primaryTagId SHALL belong to the same enterprise tag set. Non-founder creation SHALL store a direct-supervisor review snapshot without automatic approval. FOUNDER creation SHALL not require a supervisor and SHALL create an approved task with active assignments immediately. Recipient candidates SHALL return the same enterprise-wide active business user set (excluding the caller) with search, stable pagination and department metadata, and SHALL be accessible to any business user. Other authorization scopes (journal scope, member contacts, reviewer-submitter relations) SHALL remain unchanged.

#### Scenario: Missing supervisor
- **WHEN** a non-founder business user without a direct supervisor submits a task
- **THEN** WORKFLOW_POLICY_UNCONFIRMED prevents creation of a self-approved task.

#### Scenario: Founder creates without a supervisor
- **WHEN** a founder submits a valid personal or delegated task
- **THEN** the task becomes approved with active assignments immediately and no pending supervisor review.

#### Scenario: Cross-department and cross-level recipients
- **WHEN** a business user delegates to active business members outside their organization subtree or above their own rank
- **THEN** the task and all assignments are persisted and the candidate list contains those members.

#### Scenario: Self-only delegation
- **WHEN** a business user submits a delegated task whose only recipient is themself
- **THEN** the task is accepted like any other delegation.

#### Scenario: Employee delegates
- **WHEN** an EMPLOYEE-rank account requests recipient candidates or submits a delegated task
- **THEN** the backend accepts the request; disabled accounts and non-business users remain rejected as recipients.

#### Scenario: Mixed levels and creator participation
- **WHEN** a business user delegates to active business members from mixed levels across the enterprise and includes themselves
- **THEN** all selected assignments are created atomically without granting access to peers' private data, foreign enterprises or non-business users.

#### Scenario: Mixed authorized recipients
- **WHEN** one delegated recipient is disabled, from a foreign enterprise or has no business role
- **THEN** no task or assignments are persisted.
