## ADDED Requirements

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
