## MODIFIED Requirements

### Requirement: Administration workspace session
The Web client SHALL authenticate through the backend and verify ADMIN authority before showing organization, personnel/roles and company important items. It SHALL expose no company creation, self-registration, tags, task, journal or business-statistics page. Company information SHALL remain in organization management; account/settings SHALL use corner controls. Credentials SHALL be tab-scoped; expired/revoked sessions SHALL clear identity. Logout SHALL revoke the session and retain data/preferences.

#### Scenario: Enter and leave workspace
- **WHEN** an administrator logs in and later confirms logout
- **THEN** the client loads the authorized server workspace, revokes that session on logout and returns to login without deleting published directory data or preferences

#### Scenario: Rejected identity
- **WHEN** the server reports a non-administrator or a revoked/expired credential
- **THEN** the protected workspace is unavailable and prior-account cached records are not exposed

#### Scenario: Removed destination
- **WHEN** a user opens an old tag, journal, task or statistics hash
- **THEN** the client returns to personnel management without requesting business records

### Requirement: Load server-owned enterprise administration data
The Web repository SHALL load the current enterprise, organizations, users/roles and company items from backend APIs, retrieving every cursor page and mapping string IDs, versions and supervisor references. It SHALL NOT load shared tags. Writes SHALL use server results and version/idempotency commands. Identity changes SHALL invalidate old snapshots. Invalid responses or network failures SHALL report failure without substituting demo data.

#### Scenario: Directory exceeds a page
- **WHEN** the server directory contains more than the first page of users
- **THEN** every cursor page is loaded without duplicate/repeated-cursor loops or a silent first-page limit

#### Scenario: Identity changes during a write
- **WHEN** identity is cleared while a server write is pending
- **THEN** the former enterprise response cannot repopulate the new or logged-out workspace

### Requirement: Complete list feedback
Important-item searches SHALL provide a clear-search control and result count, paginate long lists and reset pagination after search changes. Empty initial lists SHALL provide creation, while unmatched searches SHALL provide filter reset. Tags SHALL have no administration page.

#### Scenario: Search has no match
- **WHEN** an important-item search has no matching records
- **THEN** the page shows zero results and allows clearing the search without creating or altering records
