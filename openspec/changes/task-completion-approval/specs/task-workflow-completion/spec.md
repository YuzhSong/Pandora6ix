## ADDED Requirements

### Requirement: Completion approval routing

The backend SHALL resolve a single completion approver for each assignment: when the recipient differs from the task creator the creator approves; when the recipient is the creator (personal tasks and self-assigned delegated tasks) the task's reviewer — the `reviewer_user_id` of the latest task review of that task — approves. A COMPLETED selection SHALL enter `AWAITING_ACCEPTANCE` with a pending completion request whenever an approver exists, and the assignment SHALL remain incomplete until that approver accepts; rejection returns the assignment to active work. Tasks without any reviewer (for example founder-created tasks) SHALL keep direct self completion. Completion notifications, the `DECIDE_COMPLETION` action and the completion decision endpoint SHALL all use the same resolved approver.

#### Scenario: Personal task completion requires reviewer approval

- **WHEN** the recipient of a personal task (who is also the creator) selects COMPLETED and the task has a reviewer
- **THEN** the assignment enters AWAITING_ACCEPTANCE with a pending completion request, and only the task reviewer can accept or reject it

#### Scenario: Delegated completion stays with the creator

- **WHEN** a delegated recipient who is not the creator selects COMPLETED
- **THEN** the creator receives the completion request and remains the only approver

#### Scenario: Self-assigned delegated completion goes to the reviewer

- **WHEN** a delegated task recipient equals the creator and selects COMPLETED
- **THEN** the task reviewer receives the completion request and is the only approver

#### Scenario: Founder task without reviewer completes directly

- **WHEN** a personal task has no reviewer (founder-created) and the creator selects COMPLETED
- **THEN** the assignment is marked COMPLETED immediately as before

### Requirement: Completion request inbox

The backend SHALL expose `GET /task-completion-requests?scope=pending|created` returning the completion requests the current user must decide (pending) or has submitted (created, including decided history), each item carrying the task id/title, requester name, submitted note, request time and decision state, with cursor pagination ordered by request time. It SHALL also expose `GET /task-completion-requests/pending-count` returning the number of pending requests awaiting the current user. The client review center SHALL present these requests as a "完成验收" queue where the approver can accept or reject each pending request, in addition to the task-detail acceptance actions.

#### Scenario: Approver lists and decides in the review center

- **WHEN** an approver opens the review center "完成验收" queue
- **THEN** pending completion requests are listed and can be accepted or rejected (with an optional note) without opening the task detail

#### Scenario: Pending count feeds the review badge

- **WHEN** any completion request is awaiting the current user's decision
- **THEN** the pending-count endpoint returns it and the review center badge reflects the total

