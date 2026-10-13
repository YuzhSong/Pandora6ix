## ADDED Requirements

### Requirement: Persistent enterprise identity and revocable sessions
The backend SHALL initialize enterprises only using an authenticated ADMIN or configured bootstrap secret, hash passwords, issue expiring revocable bearer sessions, and provide login, profile, own password change and logout. Identity SHALL come from server authentication and never a body-supplied actor. Timezones SHALL be explicit and valid IANA identifiers.

#### Scenario: Logout invalidates a credential
- **WHEN** a user logs out and reuses the bearer token
- **THEN** authenticated routes return 401 without deleting published data.

#### Scenario: Missing timezone
- **WHEN** a business-day read requires an enterprise timezone that is not configured
- **THEN** the server returns a clear configuration error without deriving it from the client.

### Requirement: Enterprise directory and scope authorization
The backend SHALL expose enterprise, organization and user administration to ADMIN, protect tenant references, reject organization/supervisor cycles, support multiple roles, and require expectedVersion for edits. ADMIN alone SHALL NOT grant private business data access. Read lists SHALL have stable, filter-bound opaque pagination.

#### Scenario: Invalid organization or supervisor
- **WHEN** an administrator attempts a cross-enterprise reference or circular hierarchy
- **THEN** the entire write is rejected with no partial directory changes.

#### Scenario: Administrative role is not business authority
- **WHEN** an ADMIN-only account requests a private journal or task
- **THEN** it cannot read that resource through administrative privileges.
