## MODIFIED Requirements

### Requirement: Atomic manager replacement

The system SHALL replace a company/department/team manager without a vacant intermediate state, atomically appointing an eligible active successor, assigning in-service direct reports to the successor and changing the former manager to an employee reporting to the successor. It SHALL version-check both managers, preserve accounts/profiles/business records, revoke affected sessions and record personnel changes. Ordinary removal or disabling of a required manager SHALL require a successor. The appointed role SHALL replace the successor's previous organizational roles.

#### Scenario: Replace a department manager with existing staff
- **WHEN** an administrator chooses an eligible successor and confirms replacement
- **THEN** the successor takes the department role and organization, existing direct reports follow the successor, and any invalid or stale input rolls back the entire change

#### Scenario: Prevent manager vacancy
- **WHEN** the last founder of a company with departments, or a department/team manager with remaining personnel or child organizations, is removed or disabled without a successor
- **THEN** the server rejects the change and Web indicates that manager replacement is required

#### Scenario: An employee takes over as team lead
- **WHEN** an administrator appoints an employee as the successor to a team lead
- **THEN** the successor's current organizational role is team lead and the before-change history still shows the employee role

## ADDED Requirements

### Requirement: Android profile displays the current organizational role

The Android profile SHALL display one current role. For profiles with multiple organizational roles, it SHALL use the same precedence as the personnel directory. Profiles with only the administrator role SHALL display administrator. Profiles without a role SHALL display the unassigned label.

#### Scenario: Display a team lead with a legacy employee role
- **WHEN** the Android profile receives both `TEAM_LEAD` and `EMPLOYEE` for the signed-in user
- **THEN** the current role row displays only the team lead label regardless of role order
