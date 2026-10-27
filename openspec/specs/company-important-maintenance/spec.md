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

### Requirement: Reorder important-item cards
Administrators SHALL reorder the complete company-item list using drag/drop or accessible move-up, move-down and move-to-top actions. The first item SHALL receive display order 1, followed by contiguous integers. Changes SHALL remain a draft until explicitly saved. Filtering SHALL disable reordering. Saving SHALL use existing versioned commands; partial failure SHALL report the limitation and reload actual server data rather than claim atomic success.

#### Scenario: Move an item to top
- **WHEN** the administrator drags an item to first position or uses move-to-top
- **THEN** the displayed draft renumbers every item from 1 and offers Save order and Cancel

#### Scenario: Save a draft
- **WHEN** the administrator saves the reordered list
- **THEN** changed order values are sent with each item's original version and server-returned records update the workspace

#### Scenario: Later write fails
- **WHEN** an order save fails after earlier records were saved
- **THEN** the page reports that partial changes may exist and reloads the actual server list without silently retrying the sequence
