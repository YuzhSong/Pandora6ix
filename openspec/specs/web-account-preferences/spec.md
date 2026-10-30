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

### Requirement: Administrator avatar upload preparation
Clicking the own-account card avatar SHALL open a dialog for selecting and previewing a PNG, JPEG or WebP image up to 5 MiB. Until a backend avatar endpoint is agreed, saving SHALL be unavailable and the dialog SHALL state that the preview is not saved. The client SHALL NOT persist the image in browser storage or replace the saved account avatar with an unconfirmed preview.

#### Scenario: Preview without backend support
- **WHEN** the administrator selects a valid avatar image
- **THEN** a temporary preview appears with a clear pending-backend message, and no upload request or persistent browser write is made

#### Scenario: Reject unsupported file
- **WHEN** an unsupported or oversized file is selected
- **THEN** the page explains the error without treating the file as a saved avatar
