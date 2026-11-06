## MODIFIED Requirements

### Requirement: Text progress and explicit work status
The backend SHALL accept text progress without requiring a percentage and retain legacy percentages for older clients. Recipient-owned versioned updates SHALL retain per-person history, but completion SHALL be task-wide. Any current recipient selecting COMPLETED SHALL atomically place the task and every current assignment into AWAITING_ACCEPTANCE, with one new completion request identifying the submitter. No recipient SHALL independently complete a shared task or bypass its designated verifier. The verifier SHALL be the creator unless the creator is a current recipient, in which case the task's latest configured reviewer SHALL verify; the existing founder personal-task self-completion exception SHALL remain. Acceptance SHALL complete every current assignment; rejection SHALL restore all current assignments to incomplete. All current recipients and the verifier SHALL receive deduplicated notifications for submission and decisions. Completed tasks SHALL remain terminal except for the established recurring personal-task reopen operation. This change SHALL NOT add task deletion. Completion SHALL preserve all existing progress history. A nonblank completion note SHALL be appended atomically, and Android SHALL retain its input after failure and load full detail independently of abbreviated calendar refreshes. Changes to a task's recipient set SHALL be rejected while awaiting acceptance or completed. Existing mixed states SHALL be reconciled: any already completed current assignment makes the task completed; otherwise any pending acceptance makes the task await acceptance. Historic completion records SHALL remain available.

#### Scenario: Text-only progress
- **WHEN** the active recipient submits a versioned text note without progressPercent
- **THEN** the note persists without changing the legacy percentage or requiring progress approval

#### Scenario: Unauthorized status change
- **WHEN** a different recipient or foreign-enterprise user attempts to change another assignment
- **THEN** the request is rejected without changing the task or any assignment

#### Scenario: Delegated completion requires superior acceptance
- **WHEN** one delegated recipient selects COMPLETED
- **THEN** every current recipient sees the task as completed pending verification, all are notified, and none becomes finally completed before acceptance

#### Scenario: Completion preserves saved history
- **WHEN** a user saves execution notes, completes the task, and the calendar refresh finishes after the full detail request
- **THEN** the detail still displays all saved records and does not replace history with abbreviated list data

#### Scenario: Complete with current stage input
- **WHEN** a recipient enters a nonblank stage note and selects completed
- **THEN** note and task-wide state are saved atomically, prior records remain visible, and an idempotent retry does not duplicate the note or request or notification

#### Scenario: Completion fails
- **WHEN** completion is rejected due to stale versions or permissions
- **THEN** no new execution record or state change persists and Android retains the input for retry

#### Scenario: Concurrent recipients complete
- **WHEN** two recipients concurrently submit completion for the same active task
- **THEN** only one new completion request succeeds and all current assignments enter the same pending state without partial writes

#### Scenario: Reconcile existing tasks
- **WHEN** the upgrade encounters a current assignment already completed or awaiting acceptance
- **THEN** the task and all current assignments are synchronized to completed or pending respectively, preserving real completion attribution and all history

#### Scenario: Modify recipients during verification
- **WHEN** an editor tries to change the recipient set of a pending-verification or completed task
- **THEN** the transaction fails without invalidating the verifier or creating mixed completion states
