## MODIFIED Requirements

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
The backend SHALL provide authorized day, Monday-based week and complete-week month ranges using task overlap, enterprise timezone, AND tags and pagination. It SHALL NOT return journals in calendar. Confirmed direct-supervisor review decisions SHALL be available; inbox, approved task editing, resubmission and unresolved timed endpoints SHALL remain closed.

#### Scenario: Historical full month
- **WHEN** an authorized user queries a historical six-week month
- **THEN** the complete range is available across pages without a ten-day history restriction or journal data.
