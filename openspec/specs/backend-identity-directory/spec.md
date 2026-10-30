# backend-identity-directory Specification

## Purpose
Define persisted enterprise identity, revocable bearer authentication, explicit business timezones, directory administration and enterprise/object authorization boundaries.
## Requirements
### Requirement: Persistent enterprise identity and revocable sessions
The backend SHALL initialize enterprises only using a configured deployment bootstrap secret, hash passwords, issue expiring revocable bearer sessions, and provide login, profile, own password change and logout. A company ADMIN SHALL NOT initialize another enterprise using a bearer credential alone. Login SHALL accept an unambiguous account identifier consisting of the exact login name followed by `@` and the enterprise login domain, without requiring a separate enterprise field. The backend SHALL resolve both components together so same-named accounts in different enterprises cannot be confused. During client migration it SHALL continue accepting the legacy enterpriseDomain plus loginName pair. Identity SHALL come from server authentication and never a body-supplied actor. Timezones SHALL be explicit and valid IANA identifiers.

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

#### Scenario: Company administrator cannot create another company
- **WHEN** an authenticated ADMIN calls enterprise initialization without the configured bootstrap secret
- **THEN** the server rejects the operation without creating company, root or account records.

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

### Requirement: Safe department and team conversion
ADMIN SHALL change DEPARTMENT and TEAM using the existing versioned organization PATCH. COMPANY kind SHALL remain fixed. Departments SHALL have a company parent; teams SHALL have a department parent. Conversion SHALL reject children, cycles, foreign enterprise references and department/team manager roles incompatible with the target kind. Organization and personnel mutations SHALL serialize on the enterprise lock.

#### Scenario: Leaf department converts to team
- **WHEN** ADMIN converts a leaf department without incompatible manager roles to TEAM under another department
- **THEN** kind and parent persist atomically with a new version, retaining the organization ID and members.

#### Scenario: Invalid conversion
- **WHEN** the root changes kind, a converting unit has children or an incompatible manager, or its parent is invalid
- **THEN** the operation fails with no changes; stale expectedVersion returns 409.

### Requirement: Atomic employee account and profile
Only the current enterprise ADMIN SHALL use POST /admin/users to create login credentials, personnel and requested roles in one transaction. New users MUST supply gender (MALE or FEMALE), idNumber (15 digits or 17 digits followed by digit/uppercase X), and phoneNumber (6 to 24 characters from digits, spaces, parentheses, hyphen and optional leading plus, at least six digits). Historical rows SHALL retain nulls until completed. PATCH SHALL preserve existing values when fields are omitted or null, including role-only edits. Invalid values, duplicate enterprise login names, reference failures and stale versions SHALL roll back the entire operation. No self-registration route SHALL be introduced.

#### Scenario: Create and log in
- **WHEN** ADMIN creates a valid employee and the employee logs in
- **THEN** saved profile and roles exist with a hashed password, all three profile keys appear in admin list/create/update responses, and credentials authenticate.

#### Scenario: Preserve legacy and populated data
- **WHEN** ADMIN edits roles on a historical empty profile or a complete profile without supplying new personal values
- **THEN** null historical values remain null and existing populated values remain unchanged.

### Requirement: Restricted personnel profile projections
The backend SHALL return gender, idNumber and phoneNumber only through authorized administrator personnel projections. Generic user, login, me, subordinate and task recipient projections SHALL omit these fields. Passwords, hashes and bearer tokens SHALL NOT appear in personnel responses or validation diagnostics.

#### Scenario: Business reads after profile completion
- **WHEN** the employee or a manager reads /me, subordinates or recipient candidates
- **THEN** raw identity numbers and phone numbers do not appear, while same-company admin personnel queries return the persisted fields.

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

### Requirement: Controlled correction of the existing company directory
Authorized repair SHALL apply the reviewed pinyin mapping with version checks, retain existing user IDs and credentials, and configure songyuzheng's confirmed supervisor as Lu Chuan. It SHALL preserve other profiles, roles, organizations, statuses, reporting relations and business references. Private snapshots and credentials SHALL remain outside Git.

#### Scenario: Named account correction
- **WHEN** the administrator applies the confirmed mapping and supervisor correction
- **THEN** every mapped account has its expected name, songyuzheng references the same-team active leader, unchanged names are retained and no account or password is recreated

#### Scenario: Concurrent or mismatched identity
- **WHEN** a target no longer matches its reviewed identity or expected version
- **THEN** the repair stops instead of overwriting unrelated directory changes


### Requirement: Immutable personnel history names
Personnel-change records SHALL preserve the actor display name and the before/after organization and supervisor names as they were when each state snapshot was recorded. Historical values that were not captured SHALL be returned as null rather than resolved from mutable current directory records.

#### Scenario: Organization or supervisor later changes name
- **WHEN** an administrator reads a personnel change after its organization or supervisor has been renamed or changed
- **THEN** the record retains the name captured with that before/after state.

#### Scenario: Legacy history without names
- **WHEN** a legacy personnel-change row has no captured historical name
- **THEN** the API returns null for that name and does not substitute a current name.
