# Spec Delta

## MODIFIED Requirements

### Requirement: Persistent enterprise identity and revocable sessions
The backend SHALL initialize enterprises only using an authenticated ADMIN or configured bootstrap secret, hash passwords, issue expiring revocable bearer sessions, and provide login, profile, own password change and logout. Login SHALL accept an unambiguous account identifier consisting of the exact login name followed by `@` and the enterprise login domain, without requiring a separate enterprise field. The backend SHALL resolve both components together so same-named accounts in different enterprises cannot be confused. During client migration it SHALL continue accepting the legacy enterpriseDomain plus loginName pair. Identity SHALL come from server authentication and never a body-supplied actor. Timezones SHALL be explicit and valid IANA identifiers.

#### Scenario: Login with account identifier
- **WHEN** a user submits `loginName@loginDomain` and the matching password
- **THEN** the backend authenticates only the account matching both the exact login name and enterprise domain.

#### Scenario: Same login name in separate enterprises
- **WHEN** two enterprises have accounts with the same login name and a client submits one account identifier
- **THEN** only the account in the identifier's enterprise can authenticate; the other account is never considered.

#### Scenario: Legacy client login
- **WHEN** an existing client submits enterpriseDomain, loginName and password
- **THEN** the backend preserves the existing tenant-scoped login behavior during migration.

#### Scenario: Logout invalidates a credential
- **WHEN** a user logs out and reuses the bearer token
- **THEN** authenticated routes return 401 without deleting published data.

#### Scenario: Missing timezone
- **WHEN** a business-day read requires an enterprise timezone that is not configured
- **THEN** the server returns a clear configuration error without deriving it from the client.

## ADDED Requirements

### Requirement: Immutable personnel history names
Personnel-change records SHALL preserve the actor display name and the before/after organization and supervisor names as they were when each state snapshot was recorded. Historical values that were not captured SHALL be returned as null rather than resolved from mutable current directory records.

#### Scenario: Organization or supervisor later changes name
- **WHEN** an administrator reads a personnel change after its organization or supervisor has been renamed or changed
- **THEN** the record retains the name captured with that before/after state.

#### Scenario: Legacy history without names
- **WHEN** a legacy personnel-change row has no captured historical name
- **THEN** the API returns null for that name and does not substitute a current name.
