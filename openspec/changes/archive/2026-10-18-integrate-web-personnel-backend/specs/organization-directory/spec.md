## ADDED Requirements

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
