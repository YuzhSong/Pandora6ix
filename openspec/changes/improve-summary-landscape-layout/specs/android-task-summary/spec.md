## ADDED Requirements

### Requirement: Usable landscape summary viewport

When the task summary is in landscape, Android SHALL hide its primary bottom navigation, combine title, back, scope selection, rotation and refresh in one compact 48dp toolbar, and allocate remaining content height to the task table. System safe insets SHALL be applied once. The compact layout SHALL omit the separate scope tab row and horizontal scrolling hint. The header row and task-name column SHALL remain fixed while task rows and other columns remain scrollable. Portrait SHALL retain its existing title and tab layout. Scope, loaded data and scroll positions SHALL survive orientation and detail navigation changes.

#### Scenario: Short landscape window
- **WHEN** summary content has a landscape viewport 280dp high after safe insets
- **THEN** its toolbar starts at the top, occupies 48dp, and the table receives at least 220dp rather than a narrow remaining strip

#### Scenario: Change task scope in landscape
- **WHEN** a user opens the compact scope menu and selects another scope
- **THEN** the corresponding task table is shown and back, rotation and refresh remain reachable

#### Scenario: Leave landscape summary
- **WHEN** a user closes the summary
- **THEN** the primary navigation returns and the existing calendar position is preserved
