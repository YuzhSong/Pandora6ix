# android-capability-task-entry Specification

## Purpose
Define server-capability-driven Android task entry, mixed delegation authorization and integration with reviews, calendars and journals.
## Requirements
### Requirement: Capability driven Android entry points
Android SHALL show management/review/calendar entries according to server capabilities, default non-reviewers to their submitted reviews and provide useful permission failure feedback. The profile SHALL show the returned supervisor name. Existing notification defaults and one-time permission requests SHALL be preserved.

#### Scenario: Employee opens task entry and reviews
- **WHEN** a user without delegation/review capability opens these screens
- **THEN** recipients are locked to that user's real identity and reviews default to submitted history without unauthorized management controls

### Requirement: Personal and mixed delegated recipients
Task kind SHALL be PERSONAL for self-only selection and DELEGATED for selection containing other recipients. Authorized managers SHALL be allowed to participate alongside active subordinates in the same enterprise. Creation, pending edits and supervisor approval SHALL enforce identical recipient authorization. A delegated task SHALL contain at least one other authorized subordinate; this SHALL NOT expand other management scopes. Founder submissions SHALL follow the backend founder auto-approval policy; other submissions SHALL retain designated-supervisor approval.

#### Scenario: Manager participates with subordinates
- **WHEN** a manager selects themselves and an active authorized subordinate
- **THEN** submission succeeds within the same management scope, and either founder auto-approval or the designated supervisor activates assignments atomically according to backend policy

#### Scenario: Ineligible recipient
- **WHEN** a mixed selection includes an inactive, cross-enterprise or otherwise unauthorized other user
- **THEN** the entire submission or approval fails without partially activating assignments

### Requirement: Calendar and detail integration
The app SHALL retain continuous month scrolling, date-to-day navigation, overlap layout, current-time positioning, optional tags, and full task/review details from PR #30 while preserving the journal screens from PR #28.

#### Scenario: Open journal and task details
- **WHEN** the user opens either content type after the rebase
- **THEN** each opens its own correct detail screen and back navigation returns to the originating screen
