## ADDED Requirements

### Requirement: One creation approval per series
New recurring schedules SHALL create one approval for the entire group. One decision SHALL approve/reject every occurrence atomically after checking all authorization and review contexts. Completion states SHALL remain separate.

#### Scenario: Group creation
- **WHEN** three occurrences are submitted and approved once
- **THEN** one review exists and all three occurrences become active

### Requirement: Explicit atomic mutation scope
Recurring details and completion edits SHALL offer SINGLE or THIS_AND_FUTURE after editing. The anchor SHALL be immutable occurrence date. The server SHALL validate all targeted versions and permissions atomically, rejecting conflicts without partial writes. Personal completed occurrences SHALL support returning to not started; delegated completion SHALL retain creator acceptance.

#### Scenario: Future details
- **WHEN** an owner saves details for an occurrence with future scope
- **THEN** only that occurrence and later ones change, without new creation approvals

#### Scenario: Single or cancel
- **WHEN** single scope is selected or the scope dialog is cancelled
- **THEN** only the selected occurrence changes, or no mutation occurs respectively

#### Scenario: Conflict
- **WHEN** a target changes after the scope snapshot was read
- **THEN** the request conflicts and no target is updated

### Requirement: Concise task forms
Task forms SHALL use controls and explicit scope buttons instead of redundant explanatory captions. Necessary field labels, values and errors SHALL remain.

#### Scenario: Editing
- **WHEN** a task form or scope dialog opens
- **THEN** controls and primary actions convey the interaction without secondary explanatory paragraphs
