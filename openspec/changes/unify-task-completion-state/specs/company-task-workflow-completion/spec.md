## MODIFIED Requirements

### Requirement: Completion acceptance loop
Task execution SHALL expose one authorized accept/reject operation for a task's pending completion and refresh authoritative state after decisions. Acceptance SHALL atomically complete the task and all current recipient assignments; rejection SHALL atomically restore all current assignments to incomplete. All current recipients SHALL receive the shared decision notification. Existing multiple pending requests from the former per-person workflow SHALL be resolved by one task-wide decision while preserving their records.

#### Scenario: Creator accepts completion
- **WHEN** an authorized verifier accepts the task's pending completion at its current request version
- **THEN** the request and all current assignments are completed, all current recipients are notified, and unauthorized or stale decisions are rejected

#### Scenario: Verifier rejects completion
- **WHEN** the authorized verifier rejects a pending completion
- **THEN** all current recipients see incomplete, receive the rejection notification, and can resume execution
