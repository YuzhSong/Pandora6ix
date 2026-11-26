## ADDED Requirements

### Requirement: Show occupied leadership positions

Personnel creation, bulk preview and position changes SHALL explain when the requested leadership position already has an in-service holder.

#### Scenario: A team already has a lead
- **WHEN** an administrator attempts to make another person team lead in that team
- **THEN** the Web interface explains that the position is occupied and points to the manager replacement workflow.

#### Scenario: New employee in a department without a manager
- **WHEN** an administrator selects a department with a vacant manager position and leaves the role at its default
- **THEN** the role defaults to department manager.

### Requirement: Enforce direct organizational membership by position

New assignments SHALL place ordinary employees in concrete teams and department-direct personnel SHALL hold the department manager position. Existing legacy assignments SHALL remain available for correction.

#### Scenario: A department manager hands over a position
- **WHEN** a department manager is replaced and becomes an employee
- **THEN** the former manager is assigned to a team as part of the same operation.

### Requirement: Derive the direct supervisor from the organization

The Web directory SHALL derive the direct supervisor from the target organizational hierarchy on creation and transfer. It SHALL choose the nearest eligible active leader at a higher rank. Founders and administrators SHALL have no automatically assigned supervisor.

#### Scenario: Staff transfer to a different team
- **WHEN** an administrator changes an employee's team
- **THEN** the saved supervisor is the active lead of the new team, or the nearest eligible leader above it.

#### Scenario: Manager leaves after replacement
- **WHEN** an administrator hands over a leadership role before departure
- **THEN** direct reports point to the successor.
