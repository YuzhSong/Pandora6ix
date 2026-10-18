## ADDED Requirements

### Requirement: Edit and verify an existing account login name
The administrator personnel editor SHALL allow editing an existing person's login name and submit the changed loginName with the existing expectedVersion to the user PATCH endpoint. A role-only edit SHALL NOT submit a replacement login name. The client SHALL accept the save only when the returned account has the requested new login name, and SHALL show a clear duplicate-name error without closing the editor or claiming success. New employee profile requirements SHALL remain enforced; unchanged nullable historical profile fields SHALL NOT block an unrelated login-name edit.

#### Scenario: Rename and reload personnel
- **WHEN** the administrator changes an employee's login name and saves
- **THEN** the existing account is updated, the new name is shown after reload, and the UI explains that the password remains unchanged.

#### Scenario: Duplicate name or an older backend
- **WHEN** the backend rejects the name as occupied or returns the old name despite a successful status
- **THEN** the editor displays an error, preserves the entered value and does not report a successful rename.

#### Scenario: Historical employee has no identity profile yet
- **WHEN** only the login name of an existing person with nullable profile details is changed
- **THEN** the editor allows saving without forcing unrelated profile completion and existing data is preserved.
