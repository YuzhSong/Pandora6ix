## ADDED Requirements

### Requirement: Reverse an accidental departure

An administrator SHALL be able to reverse a departed employee using the current record version. The system SHALL preserve the record and credentials, restore the prior account availability, resolve the current supervisor and keep the departure and its reversal in history. If a leadership position is occupied, the employee SHALL return without taking it from the incumbent.

#### Scenario: Restore a previously active employee
- **WHEN** the administrator confirms reversal of a departed employee who was active
- **THEN** the employee returns to in-service status and can sign in with the existing password, while old sessions remain revoked.

#### Scenario: Review a departure before reversal
- **WHEN** the administrator opens the reversal dialog
- **THEN** the dialog shows the account, recorded departure date and resulting employment status. Confirmation success appears in the personnel list.

### Requirement: Select one business position during reassignment

The reassignment UI and endpoint SHALL accept exactly one of founder, department manager, team lead, or employee.

#### Scenario: Switch from employee to team lead
- **WHEN** an administrator selects team lead in the reassignment dialog
- **THEN** team lead is the single selected business position.
