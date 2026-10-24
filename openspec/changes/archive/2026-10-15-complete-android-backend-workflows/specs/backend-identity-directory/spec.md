## ADDED Requirements

### Requirement: Android task role capabilities
The authenticated profile SHALL expose createPersonalTasks for business users, createDelegatedTasks for business managers and reviewTasks for business managers. ADMIN alone SHALL grant none of these business capabilities. Capabilities SHALL describe role eligibility only; configured reviewer, management scope and workflow versions SHALL still be enforced by commands.

#### Scenario: Employee can enter personal tasks
- **WHEN** an EMPLOYEE reads the current-user profile
- **THEN** createPersonalTasks is true, createDelegatedTasks and reviewTasks are false, and a missing supervisor still blocks submission with the documented policy error
