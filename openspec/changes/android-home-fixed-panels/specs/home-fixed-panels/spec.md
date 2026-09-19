## ADDED Requirements

### Requirement: Home panels remain fixed while their contents scroll
The Home page SHALL keep its four-panel dashboard composition fixed within the viewport, while each panel's item list MAY scroll independently to reveal all available records.

#### Scenario: Scroll panel content
- **WHEN** a user swipes inside a panel's item list
- **THEN** the selected panel's records scroll without moving the four-panel dashboard or bottom navigation

### Requirement: Home header blends with dashboard background
The Home title area SHALL use the same warm dashboard surface as the surrounding page and SHALL NOT appear as an isolated white strip.

#### Scenario: Open Home
- **WHEN** Home is displayed
- **THEN** the title bar and dashboard background read as one continuous warm surface
