## ADDED Requirements

### Requirement: One holder per leadership position

The backend SHALL admit at most one in-service founder per enterprise, one in-service manager per department and one in-service lead per team when assigning a leadership role. Replacement SHALL transfer the position atomically.

#### Scenario: Concurrent claims
- **WHEN** two requests claim the same vacant leadership position concurrently
- **THEN** only one succeeds and the other receives `LEADERSHIP_ROLE_TAKEN`.

#### Scenario: Different teams
- **WHEN** two eligible users are assigned to distinct teams as their respective leads
- **THEN** both assignments can succeed.

### Requirement: Validate the target organization for ordinary employees

New or changed personnel assignments SHALL require a team for ordinary employees and a department manager role for department-direct personnel. Handover and departure reversal SHALL validate the employee's team before restoring or assigning ordinary employment.

#### Scenario: Correct a historical assignment
- **WHEN** an existing department-direct ordinary employee is reassigned to a team
- **THEN** the new team is saved and the historical assignment remains in the personnel history.
