## ADDED Requirements

### Requirement: Validate personnel entry in the Web directory

Web personnel entry SHALL accept names made entirely of Han characters or entirely of English letters. Creation and name changes SHALL reject mixed scripts, whitespace, digits and symbols. Creation, profile changes and bulk preview SHALL reject identity and phone numbers already assigned to another person. Bulk preview SHALL also reject duplicates within the batch.

#### Scenario: Invalid single or bulk input
- **WHEN** an administrator enters a name with symbols or submits a duplicate identity number
- **THEN** the Web interface explains the invalid field before writing personnel data.

### Requirement: Display the current business role

The Web directory SHALL suppress the ordinary employee label when a person has a business leadership role. Personnel search and role filters SHALL use the displayed roles. Assigned permissions SHALL remain intact.

#### Scenario: Employee becomes a team lead
- **WHEN** a person holds both the employee and team lead roles
- **THEN** the Web personnel views show the team lead label and the employee filter excludes that person.
