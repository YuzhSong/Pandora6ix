## MODIFIED Requirements

### Requirement: Real task content and existing details
Task rows SHALL show title, server tags, real priority, one task-wide completion state, deadline and actual authorized executor avatars; opening a row SHALL load the existing full detail by stable ID with existing action authorization. Completion labels SHALL be 未完成 or 已完成, with 已完成，待验收 for submitted completion awaiting the verifier. Rows SHALL NOT use mixed-state, not-started or in-progress labels as task completion states. Task detail SHALL show the same task-wide state for every current recipient. Pending verification SHALL remain visible on the home task board until final acceptance, after which it SHALL be hidden.

#### Scenario: Multiple executors
- **WHEN** a task has multiple authorized assignments
- **THEN** its row uses recipientUserId for executor avatars, does not present the creator as executor, and displays one shared completion state

#### Scenario: Another recipient submitted completion
- **WHEN** recipient A submits completion for a task shared with recipient B
- **THEN** both see 已完成，待验收 in task details after authoritative data reload and the task remains on the home board with only the annotation 待验收

#### Scenario: Home board completion annotations
- **WHEN** a user reads the home task board or its expanded list
- **THEN** ordinary incomplete tasks show no completion annotation and pending-verification tasks show only 待验收; 未完成 and 已完成，待验收 are not displayed as home board annotations

#### Scenario: Approved or rejected completion
- **WHEN** the verifier accepts or rejects the shared completion
- **THEN** every recipient sees 已完成 or 未完成 respectively after reload, and only final completion removes the task from the home board
