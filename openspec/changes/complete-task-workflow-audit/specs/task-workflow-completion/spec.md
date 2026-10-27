## ADDED Requirements

### Requirement: Reviewable effective task revisions
The backend SHALL store effective-task edit proposals separately from effective content, enforce creator authorization and optimistic versions, and reject superseded decisions. Ordinary creator edits SHALL require their current supervisor approval; founder edits SHALL apply immediately. Rejection SHALL preserve the old effective content.

#### Scenario: Pending effective edit
- **WHEN** an ordinary creator submits changes requiring review
- **THEN** old effective content remains usable until a current authorized approval applies the proposal atomically

### Requirement: Text progress and explicit work status
The backend SHALL accept text progress without requiring a percentage, retain legacy percentages for older clients and enforce recipient-scoped versioned NOT_STARTED/IN_PROGRESS/COMPLETED status updates. Delegated COMPLETED selections SHALL enter AWAITING_ACCEPTANCE and SHALL NOT complete until the task creator approves; rejection returns to active work. Completed assignments remain terminal. This change SHALL NOT add task deletion.

#### Scenario: Text-only progress
- **WHEN** the active recipient submits a versioned text note without progressPercent
- **THEN** the note persists without changing the legacy percentage or requiring progress approval

#### Scenario: Unauthorized status change
- **WHEN** a different recipient or foreign-enterprise user attempts to change a work status
- **THEN** the request is rejected without changing another assignment

#### Scenario: Delegated completion requires superior acceptance
- **WHEN** a delegated recipient selects COMPLETED
- **THEN** the assignment waits for creator acceptance and remains incomplete until acceptance
