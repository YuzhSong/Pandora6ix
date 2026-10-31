## ADDED Requirements

### Requirement: Clear task and approval presentation
Task editing SHALL resolve authorized recipient display names when opened without requiring an @ search. Modal alert/date/time/confirmation surfaces SHALL be white. Editor tags SHALL all use visible fills derived from their stored color. Detail tags SHALL appear primary first using exact stored colors and a 1dp theme-neutral outline. Execution actions and text history SHALL appear inline, without a task-description dialog. Approval lists and details SHALL use profile-like themed off-white cards and neutral borders rather than dashboard yellow.

#### Scenario: Open editor and task detail
- **WHEN** an authorized creator edits a task and then reads its details
- **THEN** existing recipients have resolved names, tags retain primary order and recognizable color, and execution records do not require a modal

### Requirement: Explicit company-wide tasks
The system SHALL allow only FOUNDER business users to publish explicitly company-wide delegated tasks. The company panel SHALL list only approved company-wide tasks in the reader's enterprise, identically for all business readers. Legacy and ordinary delegated tasks SHALL remain private. Public reading SHALL NOT grant write, approval or execution rights.

#### Scenario: Private delegation
- **WHEN** a manager delegates a normal task to an employee
- **THEN** it is absent from company panels, while remaining visible in authorized personal/task views

#### Scenario: Public task isolation
- **WHEN** a founder publishes a company-wide task
- **THEN** same-enterprise business readers can list and read it without becoming executors, and other enterprises cannot access it

### Requirement: Complete editable information panel
Task detail SHALL place task information in one white segmented panel with an authorized edit action and execution in a second panel. Creators SHALL edit all user-supplied task information through a complete form. Non-founder changes SHALL require the configured superior's approval before replacing effective data; founder behavior SHALL follow the established top-level approval exception.

#### Scenario: Awaiting edit approval
- **WHEN** a creator submits changed content, dates, priority, tags or executors
- **THEN** the superior reviews that proposed version and the current effective task stays unchanged until approval

### Requirement: Completion acceptance loop
Task execution SHALL expose authorized accept/reject controls for pending completion requests and refresh authoritative state after decisions.

#### Scenario: Creator accepts completion
- **WHEN** an authorized creator accepts a pending completion request at its current version
- **THEN** the request and corresponding assignment are completed, while unauthorized or stale decisions are rejected

### Requirement: Rejected publication creates a new submission
A rejected publication result SHALL offer re-edit below the result, prefill the rejected content and submit a new task and approval without overwriting the original rejected task or review.

#### Scenario: Resubmit after rejection
- **WHEN** the submitter edits and republishes a rejected application
- **THEN** a new authorized submission is created and the previous rejection remains available in the approval center

### Requirement: Profile action ordering
The profile SHALL display approval and settings before organization information and SHALL remove the About Pandora generated text.

#### Scenario: Profile layout
- **WHEN** a user opens the profile
- **THEN** applicable approval/settings actions precede organization information and no About block remains

### Requirement: Persistent shared custom avatars
Custom avatars SHALL persist on the backend and display through shared person components after upload, relaunch and authorized viewing by other users. Failed uploads SHALL be reported accurately.

#### Scenario: Another user reads an uploaded avatar
- **WHEN** an employee successfully uploads a custom avatar and a manager views that employee
- **THEN** the actual stored image is shown instead of a stale initial placeholder

### Requirement: Theme survives relaunch
The Android client SHALL persist the selected theme by account and restore it on relaunch; unsupported stored keys SHALL fall back to the current default.

#### Scenario: Relaunch with chosen theme
- **WHEN** a user chooses a theme and fully exits and reopens the app
- **THEN** the same account retains the selected theme without leaking another account's preference
