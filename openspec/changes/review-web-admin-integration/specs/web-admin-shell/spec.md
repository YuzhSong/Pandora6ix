## MODIFIED Requirements

### Requirement: Administration workspace session
The Web client SHALL authenticate through the backend and verify ADMIN authority from trusted user data before showing organization, personnel/roles, tags and company important items. Current company information SHALL remain in organization management; account and display settings SHALL use separate corner controls. Bearer credentials SHALL be tab-scoped, and expired/revoked sessions SHALL clear cached identity and return to login. Local directory/session records SHALL NOT impersonate backend identity. Successful logout SHALL revoke the current server session while retaining published data and account-scoped display preferences.

#### Scenario: Enter and leave workspace
- **WHEN** an administrator logs in and later confirms logout
- **THEN** the client loads the authorized server workspace, revokes that session on logout and returns to login without deleting published directory data or preferences

#### Scenario: Rejected identity
- **WHEN** the server reports a non-administrator or a revoked/expired credential
- **THEN** the protected workspace is unavailable and prior-account cached records are not exposed

### Requirement: Notice external directory updates
The server-backed workspace SHALL protect external changes with expectedVersion, HTTP 409 feedback and explicit reload. A rejected edit SHALL retain the user's input rather than overwrite newer server data. Reload SHALL remain unavailable while an editor is open. The client SHALL NOT claim live cross-tab/server update notifications without an implemented server event contract.

#### Scenario: Another writer saves while editing
- **WHEN** another writer changes a server record before an open editor saves its older version
- **THEN** the save reports a conflict, preserves the draft and requires closing/reloading before editing the latest version

## ADDED Requirements

### Requirement: Load server-owned enterprise administration data
The Web repository SHALL load the current enterprise, organizations, users/roles, shared tags and company items from documented backend APIs, retrieving every cursor page and mapping string IDs, versions and supervisor references. Writes SHALL use server-returned results and documented version/idempotency commands. Identity changes SHALL invalidate old in-flight snapshots. Missing network or invalid responses SHALL report failure rather than substitute demo data.

#### Scenario: Directory exceeds a page
- **WHEN** the server directory contains more than the first page of tags or users
- **THEN** every cursor page is loaded without duplicate/repeated-cursor loops or a silent first-page limit

#### Scenario: Identity changes during a write
- **WHEN** identity is cleared while a server write is pending
- **THEN** the former enterprise response cannot repopulate the new or logged-out workspace
