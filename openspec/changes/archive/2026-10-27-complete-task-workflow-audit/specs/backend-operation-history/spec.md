## ADDED Requirements

### Requirement: Transactional task and journal operation history
The backend SHALL record successful task and journal creation, modification, progress, review and deletion operations where implemented, inside the corresponding business transaction. Records SHALL retain authenticated tenant, actor, captured names, client source, actual versions, safe snapshots and request correlation. Sensitive identity/account fields SHALL NOT be captured.

#### Scenario: Successful mutation and idempotent replay
- **WHEN** an authenticated mutation commits and its idempotency key is replayed
- **THEN** exactly one successful operation record describes the committed change

#### Scenario: Business rollback
- **WHEN** a mutation rolls back
- **THEN** its success audit and business writes both roll back

### Requirement: Sanitized failed operation history
Authenticated failed task/journal mutation requests SHALL produce a separate FAILURE operation after rollback with safe code/status metadata, without snapshots or committed versions. The backend SHALL NOT infer tenant identity from an anonymous request or resolve a foreign target label.

#### Scenario: Invalid or forbidden mutation
- **WHEN** an authenticated Web or Android mutation fails validation, authorization or version checks
- **THEN** its enterprise administrator can read one safe FAILURE record and no successful version is fabricated
