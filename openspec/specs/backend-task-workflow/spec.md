# backend-task-workflow Specification

## Purpose
Define enterprise-scoped task submission, founder exemptions, supervisor-reviewed creation and effective revisions, authorized calendar reads and recipient-specific execution with delegated acceptance.
## Requirements
### Requirement: Shared enterprise tags and pending task submission
The backend SHALL expose shared tag queries, authorized creation/maintenance, task lists/details/recipient candidates and pending task submission. PERSONAL SHALL have only the creator as recipient. DELEGATED SHALL accept active lower-ranked business members within the creator's enterprise and management scope, including mixed authorized levels. A business manager MAY join a delegated task only when it also has another authorized recipient; self-only delegation SHALL be rejected. Tag filters SHALL use AND, and primaryTagId SHALL belong to the same enterprise tag set. Non-founder creation SHALL store a direct-supervisor review snapshot without automatic approval. Founder creation SHALL require no supervisor and SHALL immediately approve the task and activate its assignments without a pending review.

#### Scenario: Missing supervisor
- **WHEN** a non-founder business user without a direct supervisor submits a task
- **THEN** WORKFLOW_POLICY_UNCONFIRMED prevents creation of a self-approved task.

#### Scenario: Founder creates without a supervisor
- **WHEN** a founder submits a valid personal or delegated task
- **THEN** the task becomes approved with active assignments immediately and no pending supervisor review.

#### Scenario: Mixed levels and creator participation
- **WHEN** a manager delegates to authorized lower-ranked members from mixed levels and includes themselves
- **THEN** all selected assignments are created atomically without granting access to peers, foreign enterprises or out-of-scope members.

#### Scenario: Mixed authorized recipients
- **WHEN** one delegated recipient is outside management scope
- **THEN** no task or assignments are persisted.

### Requirement: Separate per-recipient execution branches
The backend SHALL allow progress only for the authorized recipient of an approved active assignment, with version checks and history. A progress value of 100 SHALL NOT complete it. PERSONAL completion SHALL record SELF; DELEGATED completion SHALL require receiver submission followed by creator acceptance, updating only that receiver. Idempotent repetitions SHALL replay the original result without duplicate history. Each authorized assignment SHALL expose its own allowedActions, and task allowedActions SHALL remain the union for the current actor.

#### Scenario: Personal completion cannot accept delegated work
- **WHEN** a delegated recipient calls the personal completion route
- **THEN** the operation is rejected without changing completion facts.

#### Scenario: Acceptance affects one recipient
- **WHEN** the creator accepts one approved delegated assignment's request
- **THEN** that assignment records CREATOR_ACCEPTANCE and other assignments remain unchanged.

#### Scenario: Different recipients have different actions
- **WHEN** one recipient awaits acceptance and another is active
- **THEN** the creator receives DECIDE_COMPLETION only on the awaiting relationship and cannot update another recipient's progress

### Requirement: Calendar and unresolved workflow boundaries
The backend SHALL provide authorized day, Monday-based week and complete-week month ranges using task overlap, enterprise timezone, AND tags and pagination. It SHALL NOT return journals in calendar. Confirmed direct-supervisor review decisions SHALL be available. Approved task edits SHALL follow the separate effective-revision review contract: ordinary creators submit proposals while old content stays effective, and founder edits apply immediately. Rejected-task resubmission and task deletion SHALL remain unavailable; unrelated unconfirmed endpoints SHALL not be inferred from this contract.

#### Scenario: Historical full month
- **WHEN** an authorized user queries a historical six-week month
- **THEN** the complete range is available across pages without a ten-day history restriction or journal data.

### Requirement: Administrative role does not grant tag business authority
Pure ADMIN SHALL NOT list, create, edit or delete shared enterprise tags or access private tasks, journals or statistics. FOUNDER and DEPARTMENT_MANAGER SHALL maintain tags within their enterprise; other business users SHALL retain tag query access and cannot elevate permissions through ADMIN.

#### Scenario: Pure administrator requests business routes
- **WHEN** an ADMIN-only account requests tags, tasks, journals or statistics
- **THEN** the backend returns 403 without exposing or modifying business data.

### Requirement: Stable department metadata for recipient grouping
Authorized recipient candidates SHALL include nullable departmentId and departmentName determined from the candidate's company-scoped organization ancestry, while retaining orgUnitId and organizationName. Same-named departments SHALL remain distinct by ID. Scope, search, stable pagination and absence of sensitive personnel fields SHALL remain enforced. Android SHALL use stable department and organization IDs for department/team/person hierarchy and retrieve all candidate pages before showing a complete list. This grouping SHALL NOT narrow mixed-level candidate authorization or merge same-named organizations.

#### Scenario: Team member candidate
- **WHEN** a business manager searches a candidate assigned to a team under a department
- **THEN** the result contains that team's existing metadata and its parent department ID/name, enabling department grouping.

#### Scenario: More than one page
- **WHEN** an authorized manager has more candidates than one response page
- **THEN** each eligible candidate remains reachable and the Android picker follows pagination without dropping later candidates.
