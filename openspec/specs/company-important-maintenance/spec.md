# company-important-maintenance Specification

## Purpose
Define company-wide important-item maintenance, ordering, detail display and local save validation in the enterprise administrator workspace.
## Requirements
### Requirement: Maintain company important items
The administration client SHALL create and edit current-enterprise important items with nonempty title, full content and integer display order, and list them in stable order.

#### Scenario: Create an important item
- **WHEN** an administrator saves a valid title, content and order
- **THEN** the item appears in the list and is available after refresh

### Requirement: Read complete item content
The client SHALL provide full item detail without replacing stored content with a truncated list summary and SHALL render user content as text.

#### Scenario: Read a long item
- **WHEN** the administrator opens an item whose content exceeds the list summary capacity
- **THEN** the complete multiline content is visible and markup is not executed

### Requirement: Protect current versions
The repository SHALL reject stale important-item edits, preserve the publisher identity when updating existing items, and omit deletion operations until lifecycle policy is confirmed.

#### Scenario: Stale edit
- **WHEN** an item was updated after its form opened
- **THEN** saving the stale version reports a conflict and does not overwrite the latest content
