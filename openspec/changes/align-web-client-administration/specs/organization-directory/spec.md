## REMOVED Requirements

### Requirement: First company initialization
**Reason**: The product is operated for the client's existing company only.
**Migration**: Deployment personnel provision the company separately; the Web offers administrator login only.

## MODIFIED Requirements

### Requirement: Grouped personnel cards
Personnel SHALL group by organization or highest configured business role, with administrators first. Organization grouping SHALL nest departments under company and teams under department, each independently expandable. Direct personnel SHALL appear once at their actual level. Counts SHALL include matching descendants. Organization filter options SHALL use the same company/department/team ordering and indentation. Search SHALL expose matching paths. Job titles SHALL NOT determine authority.

#### Scenario: Group accounts by role
- **WHEN** personnel are grouped by role
- **THEN** each account belongs to its highest configured role group and the administrator group appears first

#### Scenario: Expand a department and team
- **WHEN** an administrator expands a department and then one of its teams
- **THEN** the department shows its direct personnel and team headings, only the expanded team shows its members, and collapsing the department retains its teams' expansion choices

#### Scenario: Search a team member
- **WHEN** a matching employee belongs to a team under a department
- **THEN** the matching department and team expand with filtered counts and that employee appears once

#### Scenario: Company owns departments
- **WHEN** an administrator expands the company group
- **THEN** its direct personnel and departments are shown with departments one level indented and teams one further level indented

#### Scenario: Hierarchical organization filter
- **WHEN** the organization selector is opened
- **THEN** each company precedes its departments, each department precedes its teams, and labels are indented by level even when the server returns a flat alphabetical list

#### Scenario: Initial personnel view
- **WHEN** the personnel page first mounts
- **THEN** company and department groups are expanded, teams are collapsed, and organization filter entries have visible company/department/team symbols in addition to indentation

## ADDED Requirements

### Requirement: Change non-root organization type
Administrators SHALL be able to change a department or team type and select a valid parent. The client SHALL reject cycles, invalid child relationships and incompatible assigned manager roles. The company root SHALL retain its type. Server rejection SHALL preserve the draft and SHALL NOT report success.

#### Scenario: Convert a leaf team
- **WHEN** an administrator changes a leaf team without team-lead assignments to a department and selects the company parent
- **THEN** the client submits the changed type and expected version to the server

#### Scenario: Convert a department with teams
- **WHEN** an administrator tries to turn a department with children into a team
- **THEN** the client requires moving its children first and sends no invalid edit

### Requirement: Create employee account with required profile
Personnel creation SHALL collect name, login name, initial password, gender, identity number and phone, creating personnel and account in one server request with EMPLOYEE role. Employees SHALL have no self-registration. Missing server support for profile fields SHALL block creation before writing. Existing profiles SHALL retain personal fields during role edits; incomplete legacy profiles SHALL remain readable.

#### Scenario: Supported personnel creation
- **WHEN** a supported server accepts a valid new employee profile
- **THEN** the same request creates the login account and returned profile fields must match before the client reports success

#### Scenario: Old server cannot store personal fields
- **WHEN** user responses omit the agreed personal-field keys
- **THEN** the form explains that backend support is pending and no incomplete employee creation request is sent

#### Scenario: Legacy profile edit
- **WHEN** an administrator edits an existing employee without server personal-field support
- **THEN** existing supported profile fields remain editable and new personal fields are not silently discarded
