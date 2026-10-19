# Personnel lifecycle
## Purpose
Support administrator-only personnel departures and job changes while preserving account identity and history, without exposing company tasks, journals or approval workflows.
## ADDED Requirements

### Requirement: Atomic manager replacement
The system SHALL replace a company/department/team manager without a vacant intermediate state, atomically appointing an eligible active successor, assigning in-service direct reports to the successor and changing the former manager to an employee reporting to the successor. It SHALL version-check both managers, preserve accounts/profiles/business records, revoke affected sessions and record personnel changes. Ordinary removal or disabling of a required manager SHALL require a successor.

#### Scenario: Replace a department manager with existing staff
- **WHEN** an administrator chooses an eligible successor and confirms replacement
- **THEN** the successor takes the department role and organization, existing direct reports follow the successor, and any invalid or stale input rolls back the entire change

#### Scenario: Prevent manager vacancy
- **WHEN** the last founder of a company with departments, or a department/team manager with remaining personnel or child organizations, is removed or disabled without a successor
- **THEN** the server rejects the change and Web indicates that manager replacement is required
### Requirement: Separate employment and account state
The system SHALL distinguish in-service/departed employment from active/disabled accounts. Departure SHALL record its actual date, immediately disable login, revoke sessions and erase handoff credentials while preserving identity and history. It SHALL reject future dates, self-departure, stale versions and unresolved in-service direct reports.
#### Scenario: Process departure
- **WHEN** an authorized company administrator handles an eligible departure
- **THEN** the employee is departed and disabled, sessions and temporary credentials are revoked and historical records remain unchanged
#### Scenario: Prevent accidental reactivation
- **WHEN** ordinary profile, role, reset or job-adjustment endpoints target a departed employee
- **THEN** the request is rejected and the account remains disabled
### Requirement: Atomic job adjustments
Administrators SHALL adjust an in-service employee's title, roles, organization and supervisor atomically with version checks and hierarchy validation. Account identity, password and personal profile SHALL remain unchanged. Changes to authorization SHALL invalidate old sessions.
#### Scenario: Promote an employee
- **WHEN** a valid promotion changes role and organization
- **THEN** the employee keeps the same account and receives the new directory permissions without retaining stale-session access
### Requirement: Personnel history and scoped interface
Lifecycle operations SHALL store actor, time, effective date and non-sensitive before/after personnel fields in the same transaction, queryable by company administrators. Web SHALL offer employment filtering and exclude departed users from personnel selectors, with no business handover controls.
#### Scenario: Review a change
- **WHEN** the administrator views an employee's personnel change records
- **THEN** only that company's personnel changes are returned without passwords, identity numbers or business data
