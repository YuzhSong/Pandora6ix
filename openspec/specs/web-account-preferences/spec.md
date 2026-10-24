# web-account-preferences Specification

## Purpose
Define the current administrator account display and per-account theme and font preferences with local persistence and storage error feedback.
## Requirements
### Requirement: Read own account information
The account page SHALL show the current administrator's name, login name, organization, job title, supervisor and roles as read-only information and SHALL NOT simulate successful password changes without a working authentication service.

#### Scenario: View own account
- **WHEN** the administrator opens account settings
- **THEN** the client displays their directory information and does not offer controls that alter their roles or organization through this page

### Requirement: Account-scoped display preferences
The client SHALL allow light, dark or system theme and small, standard or large font size, persist preferences per account on the current device, and preserve them after logout.

#### Scenario: Reload with dark theme
- **WHEN** the user saves dark theme and large font size then reloads
- **THEN** both preferences remain active for that account

#### Scenario: Switch accounts
- **WHEN** preferences are read for a different account
- **THEN** the other account's saved preferences or defaults are used

### Requirement: Report preference storage failures
The client SHALL keep the previous display preference if persistence fails and SHALL report the failure instead of showing successful saving.

#### Scenario: Storage is full
- **WHEN** the user changes theme but local storage rejects the write
- **THEN** the previous theme remains active and the account page shows an error
