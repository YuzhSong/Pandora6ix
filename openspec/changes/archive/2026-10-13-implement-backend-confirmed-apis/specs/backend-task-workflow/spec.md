## ADDED Requirements

### Requirement: Shared enterprise tags and pending task submission
The backend SHALL expose shared tag queries, authorized creation/maintenance, task lists/details/recipient candidates and pending task submission. PERSONAL SHALL have only the creator as recipient; DELEGATED SHALL accept only lower members in management scope. Tag filters SHALL use AND, and primaryTagId SHALL belong to the same enterprise tag set. Creation SHALL store a direct-supervisor review snapshot without automatic approval.

#### Scenario: Missing supervisor
- **WHEN** a business user without a direct supervisor submits a task
- **THEN** WORKFLOW_POLICY_UNCONFIRMED prevents creation of a self-approved task.

#### Scenario: Mixed authorized recipients
- **WHEN** one delegated recipient is outside management scope
- **THEN** no task or assignments are persisted.

### Requirement: Separate per-recipient execution branches
The backend SHALL allow progress only for the authorized recipient of an approved active assignment, with version checks and history. A progress value of 100 SHALL NOT complete it. PERSONAL completion SHALL record SELF; DELEGATED completion SHALL require receiver submission followed by creator acceptance, updating only that receiver. Idempotent repetitions SHALL replay the original result without duplicate history.

#### Scenario: Personal completion cannot accept delegated work
- **WHEN** a delegated recipient calls the personal completion route
- **THEN** the operation is rejected without changing completion facts.

#### Scenario: Acceptance affects one recipient
- **WHEN** the creator accepts one approved delegated assignment's request
- **THEN** that assignment records CREATOR_ACCEPTANCE and other assignments remain unchanged.

### Requirement: Calendar and unresolved workflow boundaries
The backend SHALL provide authorized day, Monday-based week and complete-week month ranges using task overlap, enterprise timezone, AND tags and pagination. It SHALL NOT return journals in calendar or expose Reserved approval/inbox decisions. Approved task editing, resubmission and unresolved timed endpoints SHALL remain closed.

#### Scenario: Historical full month
- **WHEN** an authorized user queries a historical six-week month
- **THEN** the complete range is available across pages without a ten-day history restriction or journal data.
