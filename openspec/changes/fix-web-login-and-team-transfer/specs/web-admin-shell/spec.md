## ADDED Requirements

### Requirement: Role appropriate personnel controls
Founders and system administrators SHALL have no supervisor selection or current-profile supervisor display. Personnel commands for those roles SHALL submit a null supervisor. System administrators SHALL have no job adjustment action. Other business roles SHALL retain their existing reporting hierarchy validation.

#### Scenario: Inspect an administrator
- **WHEN** an administrator account appears in the personnel directory or profile editor
- **THEN** no job adjustment action or supervisor control is shown

#### Scenario: Choose a founder during creation
- **WHEN** the business role changes to founder
- **THEN** the supervisor input is hidden and the created personnel command has no supervisor

### Requirement: Readable operation details
Operation details SHALL use Chinese field and enum labels and resolve organization/person references using current directory data without rewriting historical snapshots. Unknown references SHALL remain distinguishable from empty references. The modal SHALL use the available viewport width, support vertical wheel scrolling over comparisons and preserve raw change detection even when formatted values match.

#### Scenario: Review a departure
- **WHEN** an administrator opens a departure log
- **THEN** employment state appears as 在职 or 已离职, known organization/person IDs appear as readable names and the enlarged modal supports scrolling

#### Scenario: Distinguish teams and personnel
- **WHEN** an operation contains business roles and organization or person references
- **THEN** roles provide the canonical position labels without duplicate free text titles, and references include the department, team and person path where available using current directory names

### Requirement: Organization first login
The Web client SHALL show organization management as the fallback destination and after each successful login. Valid destinations SHALL remain available when refreshing an authenticated session. The login form SHALL omit the administrator-only account creation footer note.

#### Scenario: Sign in from a previous personnel page
- **WHEN** an administrator successfully signs in with a previous personnel hash
- **THEN** organization management becomes the active destination

#### Scenario: Refresh a signed in page
- **WHEN** an authenticated administrator refreshes a valid destination
- **THEN** that destination remains active
