## ADDED Requirements

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
