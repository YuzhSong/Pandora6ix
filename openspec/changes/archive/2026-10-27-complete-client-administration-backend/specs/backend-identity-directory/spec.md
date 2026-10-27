## MODIFIED Requirements

### Requirement: Persistent enterprise identity and revocable sessions
The backend SHALL initialize enterprises only using a configured deployment bootstrap secret, hash passwords, issue expiring revocable bearer sessions, and provide login, profile, own password change and logout. A company ADMIN SHALL NOT initialize another enterprise using a bearer credential alone. Identity SHALL come from server authentication and never a body-supplied actor. Timezones SHALL be explicit and valid IANA identifiers.

#### Scenario: Logout invalidates a credential
- **WHEN** a user logs out and reuses the bearer token
- **THEN** authenticated routes return 401 without deleting published data.

#### Scenario: Missing timezone
- **WHEN** a business-day read requires an enterprise timezone that is not configured
- **THEN** the server returns a clear configuration error without deriving it from the client.

#### Scenario: Company administrator cannot create another company
- **WHEN** an authenticated ADMIN calls enterprise initialization without the configured bootstrap secret
- **THEN** the server rejects the operation without creating company, root or account records.

## ADDED Requirements

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
