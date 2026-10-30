# organization-directory Specification

## Purpose
Define the fixed-enterprise organization hierarchy, personnel directory, supervisor relationships and business-role administration in the Web prototype.
## Requirements
### Requirement: Create within the selected organization
Organization details SHALL offer personnel creation with the selected organization prefilled and child-unit creation with the selected company or department prefilled as parent. The editor SHALL retain the ability to change the proposed relationship and SHALL apply existing hierarchy validation.

#### Scenario: Add a team under a department
- **WHEN** an administrator selects a department and opens child-unit creation
- **THEN** the editor defaults to a team belonging to that department and saving follows the existing version and hierarchy rules

### Requirement: Current enterprise and organization maintenance
The administration client SHALL show and edit the current demo enterprise's name and organization units, preserving valid parent relationships and rejecting self-parenting or cycles.

#### Scenario: Change a department parent
- **WHEN** an administrator selects the unit itself or one of its descendants as its parent
- **THEN** saving is rejected and the original organization remains unchanged

### Requirement: Personnel and supervisor maintenance
The client SHALL allow searching, filtering, adding and editing personnel with login name, organization and role-appropriate direct supervisor controls, rejecting duplicate login names and supervisory cycles. Business roles SHALL determine the canonical position label; cards and editors SHALL omit redundant free text job titles without erasing retained historical jobTitle values.

#### Scenario: Create personnel
- **WHEN** an administrator saves valid personnel details
- **THEN** the person is visible in the directory and remains after reloading the local demonstration

#### Scenario: Invalid supervisor
- **WHEN** a person is assigned themselves or a management descendant as supervisor
- **THEN** saving is rejected without changing existing personnel

### Requirement: Fixed role assignment
The client SHALL configure personnel roles from ADMIN, FOUNDER, DEPARTMENT_MANAGER, TEAM_LEAD and EMPLOYEE and SHALL NOT infer business read or approval permissions from administrator status.

#### Scenario: Configure a team lead
- **WHEN** an administrator selects TEAM_LEAD for a person and saves
- **THEN** the directory displays that role without granting the administrator daily task or journal operations

### Requirement: Safe local demonstration storage
Demo directory data SHALL be separate from the demonstration session, contain no passwords, and SHALL report storage failures instead of claiming a successful durable save.

#### Scenario: Browser storage unavailable
- **WHEN** a write cannot be persisted in browser storage
- **THEN** the UI reports failure and does not show a successful save

### Requirement: Separate profile and role maintenance
Personnel forms SHALL maintain profile and organizational relationships without changing existing roles. New personnel SHALL start with EMPLOYEE. Dedicated role configuration SHALL update only a selected person’s roles, require at least one valid role and reject stale versions without altering profile fields. Organization creation SHALL preselect an eligible parent; COMPANY root editing SHALL not request a parent.

#### Scenario: Configure roles independently
- **WHEN** an administrator saves a role selection
- **THEN** profile and organizational fields remain unchanged and stale or invalid selections are rejected

### Requirement: Company-scoped local administration
Administrators SHALL operate only their bound local company. The client SHALL NOT expose a company list or switching actions. Existing catalog data SHALL remain preserved, with its initially bound company retained despite later activeId changes. Company names and root names SHALL stay synchronized.

#### Scenario: Read a legacy catalog
- **WHEN** the client first opens an existing multi-company catalog
- **THEN** it retains the current company binding, preserves other directories without exposing them and writes only the bound company's data

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

### Requirement: Edit and verify an existing account login name
The administrator personnel editor SHALL allow editing an existing person's login name and submit the changed loginName with the existing expectedVersion to the user PATCH endpoint. A role-only edit SHALL NOT submit a replacement login name. The client SHALL accept the save only when the returned account has the requested new login name, and SHALL show a clear duplicate-name error without closing the editor or claiming success. New employee profile requirements SHALL remain enforced; unchanged nullable historical profile fields SHALL NOT block an unrelated login-name edit.

#### Scenario: Rename and reload personnel
- **WHEN** the administrator changes an employee's login name and saves
- **THEN** the existing account is updated, the new name is shown after reload, and the UI explains that the password remains unchanged.

#### Scenario: Duplicate name or an older backend
- **WHEN** the backend rejects the name as occupied or returns the old name despite a successful status
- **THEN** the editor displays an error, preserves the entered value and does not report a successful rename.

#### Scenario: Historical employee has no identity profile yet
- **WHEN** only the login name of an existing person with nullable profile details is changed
- **THEN** the editor allows saving without forcing unrelated profile completion and existing data is preserved.

### Requirement: Change non-root organization type
Administrators SHALL be able to change a department or team type and select a valid parent. The client SHALL reject cycles, invalid child relationships and incompatible assigned manager roles. The company root SHALL retain its type. Server rejection SHALL preserve the draft and SHALL NOT report success.

#### Scenario: Convert a leaf team
- **WHEN** an administrator changes a leaf team without team-lead assignments to a department and selects the company parent
- **THEN** the client submits the changed type and expected version to the server

#### Scenario: Convert a department with teams
- **WHEN** an administrator tries to turn a department with children into a team
- **THEN** the client requires moving its children first and sends no invalid edit

### Requirement: Create employee account with required profile
Personnel creation SHALL collect name, login name, business role, gender, identity number and phone, generate an independent random 16-character initial password and create personnel and account in one server request with the selected role. Passwords SHALL remain stable while correcting or retrying the same draft, SHALL NOT be editable creation or CSV fields and SHALL only be delivered after server success. Employees SHALL have no self-registration. Missing server support for profile fields SHALL block creation before writing. Existing profiles SHALL retain personal fields during role edits; incomplete legacy profiles SHALL remain readable.

#### Scenario: Supported personnel creation
- **WHEN** a supported server accepts a valid new employee profile
- **THEN** the same request creates the login account and returned profile fields must match before the client reports success

#### Scenario: Old server cannot store personal fields
- **WHEN** user responses omit the agreed personal-field keys
- **THEN** the form explains that backend support is pending and no incomplete employee creation request is sent

#### Scenario: Legacy profile edit
- **WHEN** an administrator edits an existing employee without server personal-field support
- **THEN** existing supported profile fields remain editable and new personal fields are not silently discarded

### Requirement: Employee account handoff
Personnel cards SHALL offer authorized account and available initial/reset password handoff details from the credential-handoff service. Generated creation passwords or administrator-reset passwords SHALL be deliverable only after server success. The backend SHALL retain encrypted temporary credentials for the configured delivery window; Web SHALL fetch fresh authorized credentials for copy/download, never persist plaintext passwords in browser storage and discard in-memory secrets on close or session loss. Details SHALL NOT retrieve passwords from hashes. Clipboard failures SHALL offer manual selection.

#### Scenario: Hand over a newly created account
- **WHEN** personnel creation succeeds
- **THEN** the administrator sees the company login identifier, server-returned account and masked initial password, with reveal and copy actions, without repeating creation

#### Scenario: Look up an existing account
- **WHEN** the administrator opens a person's account details
- **THEN** company login identifier, login account and any available handoff password are available to select or copy, and no current-password retrieval is attempted

#### Scenario: Close receipt or lose session
- **WHEN** the receipt closes
- **THEN** in-memory handoff secrets are discarded; a later authorized read may retrieve still-available encrypted temporary credentials from the server within the delivery window, while session loss prevents further access

### Requirement: Bulk personnel creation and credential delivery
The Web SHALL prevalidate at most 100 CSV personnel rows before sending creation requests. It SHALL report each row, retain successful results and stop after a failed or uncertain request without automatic retry. Administrators SHALL copy a plain-text list or download a genuine .xlsx workbook of currently available handoff credentials, with unavailable credentials clearly identified only in the relevant handoff context. No passwords SHALL be persisted in browser storage or embedded in public application code.

#### Scenario: Partial batch success
- **WHEN** a row fails after preceding rows succeed
- **THEN** successful accounts and their handoff credentials remain available, later rows are not sent, and the administrator is told to refresh before retrying

### Requirement: Employee password recovery and full administrative details
Personnel details SHALL display complete identity numbers already accessible to administrators. Administrators SHALL reset non-ADMIN employee passwords with confirmation and version protection, then receive copyable new credentials after server success. Older servers without reset support SHALL show an explicit deployment message and SHALL NOT claim success.

#### Scenario: Employee forgets password
- **WHEN** an administrator submits a valid confirmed replacement password
- **THEN** Web uses the reset endpoint and shows a new handoff receipt only after success

### Requirement: Incremental legacy personnel profile updates
On a server supporting gender, identity number and phone, new personnel SHALL require all three fields and create a login account in the same request. Existing personnel SHALL allow valid individual fields to be added while other legacy fields remain null. Name or role changes SHALL preserve personal fields. Clearing an existing value SHALL be rejected while the backend treats null as retain. Missing field support SHALL prevent incomplete creation.

#### Scenario: Supplement only a legacy phone
- **WHEN** an administrator supplies a valid phone for an existing person whose gender and identity are null
- **THEN** the phone saves, the other fields remain null, and reload shows the saved phone

#### Scenario: Rename historical administrator
- **WHEN** an administrator changes a name or role without adding missing personal fields
- **THEN** the change saves without requiring the missing fields and without clearing existing values

#### Scenario: Clear a known personal value
- **WHEN** the administrator clears a populated personal field
- **THEN** the client explains that clearing is unsupported rather than claiming the backend erased it

### Requirement: Verify current backend directory persistence
The Web integration SHALL verify supported personnel creation, subsequent login and profile reload, role edits preserving profile, and leaf organization conversion in both directions using actual backend responses. It SHALL keep current-company authorization, hide company creation and tag/business administration, and report actual backend availability for operation history and avatar support without placeholder success.

#### Scenario: Create employee account and convert a leaf
- **WHEN** integration verification creates personnel and converts an eligible leaf department or team
- **THEN** the account can log in, personal fields survive reload, and organization kind updates preserve its ID and version checks

### Requirement: Confirm department and team deletion
Web SHALL offer deletion for non-root organization units, explain dependency blockers, and require confirmation naming the selected unit. A successful server response SHALL remove the unit from the tree and personnel filters and select its parent. Failed requests SHALL preserve directory data and display an actionable error. Confirmation SHALL prevent duplicate submission and accidental navigation while pending.

#### Scenario: Confirm empty unit deletion
- **WHEN** the administrator confirms deletion of an empty team
- **THEN** Web sends the unit identifier and displayed version, removes it only after success, and selects the parent department

#### Scenario: Cancel or failed deletion
- **WHEN** confirmation is cancelled or the server rejects deletion
- **THEN** the unit remains visible and a rejected request displays its reason

### Requirement: Concise personnel editing and actionable credential handoff
The personnel editor SHALL retain validation and actionable failures without persistent login-change, legacy-profile or generic supervisor explanation blocks. Bulk handoff SHALL use the authorized server credential-handoff service to determine deliverable credentials for existing non-admin personnel, including credentials available after re-login within the delivery window. It SHALL NOT list all historical accounts as missing passwords or enable downloading an empty list.

#### Scenario: Edit existing personnel
- **WHEN** an administrator opens a personnel editor
- **THEN** editable values and required indicators remain available without the redundant explanation blocks, and invalid saves still receive a concrete error

#### Scenario: No captured credentials
- **WHEN** the server reports no deliverable credentials
- **THEN** bulk handoff is unavailable and no empty export dialog with a missing-password name wall is opened

#### Scenario: New or reset account
- **WHEN** the administrator has successfully created or reset an employee account
- **THEN** bulk handoff becomes available for the account through fresh authorized server reads and copy/download continue working without persisting plaintext passwords in the browser
