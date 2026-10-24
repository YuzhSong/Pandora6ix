## ADDED Requirements

### Requirement: Administrator avatar upload preparation
Clicking the own-account card avatar SHALL open a dialog for selecting and previewing a PNG, JPEG or WebP image up to 5 MiB. Until a backend avatar endpoint is agreed, saving SHALL be unavailable and the dialog SHALL state that the preview is not saved. The client SHALL NOT persist the image in browser storage or replace the saved account avatar with an unconfirmed preview.

#### Scenario: Preview without backend support
- **WHEN** the administrator selects a valid avatar image
- **THEN** a temporary preview appears with a clear pending-backend message, and no upload request or persistent browser write is made

#### Scenario: Reject unsupported file
- **WHEN** an unsupported or oversized file is selected
- **THEN** the page explains the error without treating the file as a saved avatar
