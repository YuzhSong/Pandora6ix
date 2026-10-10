# dev-integration-review Specification

## Purpose
Preserve calendar navigation, Android session data, HTTP error semantics and independent module validation when integrating reviewed work into dev.
## Requirements
### Requirement: Preserve explicit calendar navigation
The Android prototype SHALL expose day, week, month and year modes. Pinch gestures SHALL select adjacent modes in that order. Selecting a date SHALL retain the current mode, and selecting a task SHALL directly open its detail. Overflow SHALL remain accessible without forced drill-down.

#### Scenario: Month task selection
- **WHEN** a user taps a task in month mode
- **THEN** its detail opens directly without changing the calendar mode

#### Scenario: Date and mode selection
- **WHEN** a user selects a date in month mode and then explicitly chooses day mode
- **THEN** the date tap retains month mode and the explicit choice opens the selected day

#### Scenario: Pinch boundaries
- **WHEN** a user zooms in from week or out from month
- **THEN** day or year mode respectively is selected, and zooming beyond day/year keeps that boundary mode

#### Scenario: Large font calendar dates
- **WHEN** the user selects the large font setting and opens the year or month calendar
- **THEN** date digits remain legible without inheriting a line height larger than their date cell

### Requirement: Preserve Android session edits
The prototype SHALL retain personal-item edits, published daily logs and notification preferences across navigation during one application session. Publishing a supplement SHALL update the existing report for that date without creating a second record. The log page SHALL use current dates, remove its message entry and keep the IME editor controls usable.

#### Scenario: Revisit an edited page
- **WHEN** a user edits personal items or disables notifications, leaves the page and returns
- **THEN** the edit or setting is retained and the profile notification label matches the setting

#### Scenario: Supplement today's seeded report
- **WHEN** the user supplements today's existing mock report and publishes
- **THEN** the report content is replaced in the shared collection and only one report exists for today

### Requirement: Preserve client error semantics
The backend SHALL return its uniform ApiError body for framework HTTP errors while retaining their original status and relevant HTTP headers. Unexpected failures SHALL remain 500 without exposing internal exception details.

#### Scenario: Unsupported method
- **WHEN** a client posts to the GET-only health endpoint
- **THEN** the backend returns 405 with an Allow header and a JSON error body containing path and request ID

#### Scenario: Invalid input
- **WHEN** MVC rejects invalid request input or validation
- **THEN** the response retains the client-error status rather than returning 500

### Requirement: Validate integrated modules
The integration SHALL preserve Android, backend and Web independent builds and validate the related OpenSpec changes strictly before the remote dev update.

#### Scenario: Integrated validation
- **WHEN** the integration is ready to push
- **THEN** module checks and strict OpenSpec validation results are recorded with any unperformed device checks identified
