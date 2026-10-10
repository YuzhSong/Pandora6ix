## ADDED Requirements

### Requirement: Home panels use the available dashboard space
The Home page SHALL render the four Pandora business panels in a responsive two-column layout with warm-cream surfaces, warm-orange borders, and a pale warm-yellow dashboard background.

#### Scenario: Spacious dashboard
- **WHEN** Home is opened on a common phone viewport
- **THEN** the four panels occupy the main content area with balanced spacing and no fixed width overflow

### Requirement: Panels preview up to ten items
Each Home panel SHALL show up to ten concise items from its existing centralized mock collection, with bounded text and scrolling when all rows cannot fit simultaneously.

#### Scenario: Ten-item preview
- **WHEN** a panel has ten or more records
- **THEN** the panel exposes ten records in its scrollable content region without text escaping the card bounds

### Requirement: Tapping a panel opens a layered centered overlay
The Home page SHALL animate the tapped panel into a centered elevated overlay above a dimmed dashboard, leaving visible edges of the other panels around it, and SHALL provide close/back actions.

#### Scenario: Expand and close a panel
- **WHEN** a user taps a Home panel
- **THEN** that panel scales into the screen center, the background is dimmed, and the other panels remain partially visible behind it

#### Scenario: Dismiss overlay
- **WHEN** a user taps the close action or the scrim outside the expanded panel
- **THEN** the overlay closes with a reverse transition and the four-panel dashboard is restored
