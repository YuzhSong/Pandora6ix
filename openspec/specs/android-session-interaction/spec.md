# android-session-interaction Specification

## Purpose
Define safe Android session renewal, account-scoped review badges and task-form intent preservation.
## Requirements
### Requirement: Password change renews the correct session
Android SHALL retain a successful password change in a fresh authenticated session when saved login identity is available. An overlapping 401 for the token being renewed SHALL fail that request without clearing the renewal. Normal unauthorized requests SHALL still invalidate the session. Failed renewal SHALL clear the revoked session and return to login. Passwords SHALL NOT be persisted.

#### Scenario: Overlapping old-token response
- **WHEN** the password change succeeds and a concurrent old-token request returns 401 before login renewal finishes
- **THEN** the renewal completes without a global logout, and subsequent requests use the new token

#### Scenario: Renewal fails
- **WHEN** the password changes but new-password login fails or identity is missing
- **THEN** the old session is cleared and the app requests manual login

#### Scenario: Wrong current password
- **WHEN** the password endpoint rejects the current password
- **THEN** the existing session is retained and ordinary unauthorized handling remains active

### Requirement: Account scoped review badges
Pending review counts SHALL load only for authenticated users with review permission. Account switches and permission loss SHALL reset the badge; canceled or stale requests SHALL NOT publish a count to a different account.

#### Scenario: Account switch during request
- **WHEN** an old account's review request finishes after another account becomes active
- **THEN** its count is discarded

### Requirement: Preserve task form intent
Task exit detection SHALL include task kind together with every existing editable field. Untagged tasks SHALL remain valid according to the task-tags specification. Task detail screens SHALL load the full server detail before rendering complete recipients and labels.

#### Scenario: Only kind changed
- **WHEN** a user changes only personal versus delegated task kind then closes the form
- **THEN** the unsaved-change prompt appears

#### Scenario: Untagged task
- **WHEN** an otherwise valid task has no tags
- **THEN** the client allows it to be submitted
