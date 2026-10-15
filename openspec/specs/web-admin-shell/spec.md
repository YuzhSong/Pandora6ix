# web-admin-shell Specification

## Purpose
Define the local administrator workspace entry, fixed-company initialization, navigation, editing safeguards and transport preparation for future authentication integration.
## Requirements
### Requirement: Keep working context during refresh
Reloading directory data SHALL retain search, filter, ordering, page-size and selected-organization state. If reload fails after a successful load, the client SHALL retain the last readable snapshot, visibly report the failure and prevent edits until loading succeeds.

#### Scenario: Refresh a filtered list
- **WHEN** an administrator refreshes data after filtering and selecting an ordering
- **THEN** the same controls and page context remain available with the refreshed data

### Requirement: Shared application visual language
The Web administration interface SHALL use the supplied App references’ warm yellow accent and cream surfaces, with clear spacing and subtle navigation materials inspired by Apple Human Interface Guidelines. Focus indicators, primary actions, selected navigation, dialogs and dark mode SHALL remain readable and recognizable without depending on color alone.

#### Scenario: Change display mode
- **WHEN** an administrator uses light or dark display settings on desktop or a narrow viewport
- **THEN** the same restrained accent identifies actions and selection while text, controls and feedback remain legible

### Requirement: Complete list feedback
Tag and important-item searches SHALL provide a clear-search control and result count. Important items SHALL paginate long lists and reset pagination after search changes. Empty initial lists SHALL provide the existing creation action, while unmatched searches SHALL provide a filter-reset action.

#### Scenario: Search has no match
- **WHEN** a tag or important-item search has no matching records
- **THEN** the page shows zero results and allows clearing the search without creating or altering records

### Requirement: Avoid unchanged edits
Editors SHALL prevent submission of unchanged existing records and SHALL bring failed-save feedback into keyboard focus while retaining the entered content.

#### Scenario: Open an existing record
- **WHEN** an administrator opens an unchanged existing record
- **THEN** saving remains unavailable until a field changes

### Requirement: Practical directory navigation
The personnel directory SHALL offer read-only personnel details, deterministic name and login sorting, and selectable page sizes. Search, role and organization filters SHALL remain applied when changing sorting or page size. Product wording SHALL use direct administrative language, and repeated decorative summaries SHALL not obscure the current task.

#### Scenario: Inspect a filtered directory
- **WHEN** an administrator filters personnel, changes ordering or page size, and opens a personnel detail
- **THEN** the directory retains the filters, presents the matching records and shows full profile, organization, supervisor and role information without changing data

### Requirement: Notice external directory updates
The server-backed workspace SHALL protect external changes with expectedVersion, HTTP 409 feedback and explicit reload. A rejected edit SHALL retain the user's input rather than overwrite newer server data. Reload SHALL remain unavailable while an editor is open. The client SHALL NOT claim live cross-tab/server update notifications without an implemented server event contract.

#### Scenario: Another writer saves while editing
- **WHEN** another writer changes a server record before an open editor saves its older version
- **THEN** the save reports a conflict, preserves the draft and requires closing/reloading before editing the latest version

### Requirement: Administration workspace session
The Web client SHALL authenticate through the backend and verify ADMIN authority from trusted user data before showing organization, personnel/roles, tags and company important items. Current company information SHALL remain in organization management; account and display settings SHALL use separate corner controls. Bearer credentials SHALL be tab-scoped, and expired/revoked sessions SHALL clear cached identity and return to login. Local directory/session records SHALL NOT impersonate backend identity. Successful logout SHALL revoke the current server session while retaining published data and account-scoped display preferences.

#### Scenario: Enter and leave workspace
- **WHEN** an administrator logs in and later confirms logout
- **THEN** the client loads the authorized server workspace, revokes that session on logout and returns to login without deleting published directory data or preferences

#### Scenario: Rejected identity
- **WHEN** the server reports a non-administrator or a revoked/expired credential
- **THEN** the protected workspace is unavailable and prior-account cached records are not exposed

### Requirement: Uniform API transport feedback
The client transport SHALL distinguish network failures, unauthenticated responses, version conflicts and server failures, accept documented JSON success responses and no-content responses, and preserve request identifiers for error diagnosis.

#### Scenario: Conflicting server edit
- **WHEN** an API request returns HTTP 409 with a request ID
- **THEN** the transport reports the conflict with that request ID and does not return a successful result

### Requirement: Accessible responsive administration
The client SHALL support labelled forms, keyboard-operable navigation and dialogs, feedback for loading, empty results and failed saves, and usable narrow-screen layouts.

#### Scenario: Narrow screen and keyboard dialog
- **WHEN** a user opens a form at a narrow viewport or operates it with a keyboard
- **THEN** controls remain reachable, the dialog contains focus, Escape can request closing, and focus returns to the initiating control

### Requirement: Protect uncommitted changes
The client SHALL request confirmation before discarding a changed form and SHALL leave the entered content available after a rejected save.

#### Scenario: Cancel an edited form
- **WHEN** a user attempts to close a changed form and declines the discard confirmation
- **THEN** the form remains open with the user's content

### Requirement: Top workspace navigation
The workspace SHALL place primary administration destinations above page content in a sticky horizontal navigation bar, with account and display settings accessed from corner controls, retain hash routing and active-page semantics, and provide horizontal navigation scrolling on narrow screens without widening the page. Account and logout controls SHALL remain accessible in the header.

#### Scenario: Navigate on a narrow screen
- **WHEN** an administrator uses a narrow viewport and selects a destination in the top navigation
- **THEN** the chosen page opens, its link identifies the active page and the content remains within the viewport with wide tables scrolling independently

### Requirement: Dedicated discard confirmation
Closing a dirty editor SHALL present a separate modal confirmation with Continue editing and Discard actions. Continuing or cancelling the confirmation SHALL retain the draft and restore focus to the editor; discarding SHALL close both dialogs and restore focus to the initiating page control. Required markers SHALL appear alongside their field labels.

#### Scenario: Keep unsaved edits
- **WHEN** a user closes a changed editor and continues editing from the separate confirmation
- **THEN** the confirmation closes and the editor retains its draft with keyboard focus restored

### Requirement: Compact management destinations
The primary navigation SHALL combine personnel and roles into one destination. The administrator avatar SHALL open the personal account and a separate corner settings icon SHALL open display settings. Existing role links SHALL resolve to personnel management.

#### Scenario: Open settings
- **WHEN** an administrator selects the corner settings action
- **THEN** display preferences open independently from personal profile data

### Requirement: Load server-owned enterprise administration data
The Web repository SHALL load the current enterprise, organizations, users/roles, shared tags and company items from documented backend APIs, retrieving every cursor page and mapping string IDs, versions and supervisor references. Writes SHALL use server-returned results and documented version/idempotency commands. Identity changes SHALL invalidate old in-flight snapshots. Missing network or invalid responses SHALL report failure rather than substitute demo data.

#### Scenario: Directory exceeds a page
- **WHEN** the server directory contains more than the first page of tags or users
- **THEN** every cursor page is loaded without duplicate/repeated-cursor loops or a silent first-page limit

#### Scenario: Identity changes during a write
- **WHEN** identity is cleared while a server write is pending
- **THEN** the former enterprise response cannot repopulate the new or logged-out workspace
