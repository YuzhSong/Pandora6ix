## ADDED Requirements

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
