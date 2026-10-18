# backend-identity-directory Specification

## Purpose
Define persisted enterprise identity, revocable bearer authentication, explicit business timezones, directory administration and enterprise/object authorization boundaries.
## Requirements
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

### Requirement: Android task role capabilities
The authenticated profile SHALL expose createPersonalTasks for business users, createDelegatedTasks for business managers and reviewTasks for business managers. ADMIN alone SHALL grant none of these business capabilities. Capabilities SHALL describe role eligibility only; configured reviewer, management scope and workflow versions SHALL still be enforced by commands.

#### Scenario: Employee can enter personal tasks
- **WHEN** an EMPLOYEE reads the current-user profile
- **THEN** createPersonalTasks is true, createDelegatedTasks and reviewTasks are false, and a missing supervisor still blocks submission with the documented policy error

### Requirement: Administrator-maintained company login names
The existing administrator user update endpoint SHALL accept an optional loginName. Omission or null SHALL preserve the current login name. A supplied value SHALL contain a non-whitespace character, have no surrounding whitespace, be at most 128 characters and follow the existing exact matching semantics. Only an ADMIN of the target enterprise SHALL rename the account. The operation SHALL require expectedVersion, reject stale edits with VERSION_CONFLICT and reject a duplicate name within the enterprise with LOGIN_NAME_TAKEN. Failure SHALL leave the entire account, roles and relations unchanged.

#### Scenario: Rename an employee and authenticate
- **WHEN** a company administrator renames an employee with the current version
- **THEN** the response and subsequent queries contain the new name, the new login name works with the original password, the old name no longer logs in, and the user's ID, password hash, profile, roles and business associations remain unchanged.

#### Scenario: Keep existing ID-based sessions
- **WHEN** an administrator renames an account with an existing valid session, including their own administrator account
- **THEN** that session remains authenticated as the same user, and the existing last-active-administrator and account-disabling protections remain effective.

#### Scenario: Legacy profile or role update
- **WHEN** an existing client omits loginName or sends null while updating a user's other fields
- **THEN** the account's current login name is preserved.

#### Scenario: Invalid or duplicate login name
- **WHEN** the new name is empty, whitespace-only, padded with whitespace, longer than 128 characters or already belongs to another user in the same enterprise
- **THEN** the server rejects it with a validation error for loginName or LOGIN_NAME_TAKEN, without partial profile or role changes.

#### Scenario: Enterprise isolation
- **WHEN** a non-administrator or administrator of another enterprise attempts a rename
- **THEN** the request is rejected and the target account is unchanged; the same login name in a separate enterprise does not block a valid local rename.

#### Scenario: Concurrent rename requests
- **WHEN** concurrent requests use the same version of one account or attempt to claim the same name for separate local accounts
- **THEN** only one conflicting write succeeds, with stale or duplicate requests rejected atomically.

### Requirement: Empty organization deletion
The backend SHALL allow ADMIN to delete an empty department or team in their own enterprise using DELETE /admin/org-units/{id} with a required nonnegative expectedVersion JSON field. Success SHALL return 204. Company roots, units with children and units with assigned users including disabled accounts SHALL be preserved. Concurrent directory writes SHALL preserve these constraints.

#### Scenario: Empty leaf deleted
- **WHEN** an administrator deletes an empty unit with its current version
- **THEN** it disappears from directory reads and a repeated delete returns 404

#### Scenario: Protected or occupied unit
- **WHEN** deletion targets the root, a unit with children or a unit with assigned accounts
- **THEN** the server returns 409 with a specific blocking code and preserves all records

#### Scenario: Version and authority boundaries
- **WHEN** deletion has a stale version, a foreign enterprise identifier, a non-admin actor or no authentication
- **THEN** it returns 409, 404, 403 or 401 respectively without removing the unit

### Requirement: Administrator employee password reset
An authenticated ADMIN SHALL reset a non-ADMIN employee password in the same enterprise using POST /admin/users/{id}/password-reset with expectedVersion and newPassword. The server SHALL enforce password length and BCrypt byte limits, reject stale versions, increment the user version and revoke all target sessions atomically. Profiles, roles and business data SHALL be preserved. Responses SHALL NOT contain plaintext passwords or hashes.

#### Scenario: Recover employee access
- **WHEN** an administrator resets an employee password using the current version
- **THEN** the new password works, the old password and all previous sessions fail, and directory and business associations remain unchanged

#### Scenario: Reject invalid authority or input
- **WHEN** reset targets another enterprise, an administrator account, stale version, invalid password, or is requested without administrator authority
- **THEN** the command is rejected without changing passwords, sessions or personnel data

### Requirement: Controlled employee credential maintenance
Offline maintenance SHALL preview an explicit employee login mapping before applying it to one enterprise. Applying SHALL preserve user IDs and business data, protect ADMIN accounts, reject stale identities and login collisions, store only BCrypt password hashes and revoke affected sessions atomically. The tool SHALL NOT print passwords or password hashes and SHALL refuse to create a missing database.

#### Scenario: Apply reviewed mapping
- **WHEN** the operator applies a valid mapping and supplied password to the existing stopped database
- **THEN** all selected logins/password hashes/versions and session revocations commit together while profiles, roles and business references remain unchanged

#### Scenario: Preview or invalid mapping
- **WHEN** apply is absent or a mapping targets an administrator, missing identity or conflicting login
- **THEN** no account, password or session changes persist
