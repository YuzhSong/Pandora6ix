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
The client SHALL allow searching, filtering, adding and editing personnel with independent organization, job title, login name and direct supervisor fields, rejecting duplicate login names and supervisory cycles.

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

### Requirement: First company initialization
A browser without company data SHALL show a company initialization form and establish only the company root and administrator profile. Existing company data SHALL prevent repeated initialization. Storage failures SHALL not report success. The form SHALL NOT collect passwords or claim server authentication.

#### Scenario: Initialize once
- **WHEN** an administrator initializes an empty workspace
- **THEN** the company root and administrator are saved and subsequent visits enter that company without offering another creation

### Requirement: Grouped personnel cards
Personnel SHALL group by organization or highest configured business role, with administrators first and expandable rows of account cards. Job title text SHALL NOT determine role authority.

#### Scenario: Group accounts by role
- **WHEN** personnel are grouped by role
- **THEN** each account belongs to its highest configured role group and the administrator group appears first

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

### Requirement: Employee account handoff
Personnel cards SHALL offer copyable account and initial/reset password handoff details. Creation/reset responses SHALL use the entered password only after server success. Handoff passwords SHALL remain in page memory only and clear on logout or page reload. Details SHALL NOT retrieve current passwords from hashes. Clipboard failures SHALL offer manual selection.

#### Scenario: Hand over a newly created account
- **WHEN** personnel creation succeeds
- **THEN** the administrator sees the company login identifier, server-returned account and masked initial password, with reveal and copy actions, without repeating creation

#### Scenario: Look up an existing account
- **WHEN** the administrator opens a person's account details
- **THEN** company login identifier, login account and any available handoff password are available to select or copy, and no current-password retrieval is attempted

#### Scenario: Close receipt or lose session
- **WHEN** the receipt closes
- **THEN** the handoff password remains available in personnel details for the current page; logout or page reload clears it

### Requirement: Bulk personnel creation and credential delivery
The Web SHALL prevalidate at most 100 CSV personnel rows before sending creation requests. It SHALL report each row, retain successful results and stop after a failed or uncertain request without automatic retry. Administrators SHALL copy or download a plain-text list of available handoff credentials, with missing credentials clearly identified. No passwords SHALL be persisted in browser storage or embedded in public application code.

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
The Web integration SHALL verify supported personnel creation, subsequent login and profile reload, role edits preserving profile, and leaf organization conversion in both directions using actual backend responses. It SHALL keep current-company authorization, hide company creation and tag/business administration, and retain pending status for unimplemented audit storage and avatar upload.

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
