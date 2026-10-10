## ADDED Requirements

### Requirement: Selected navigation uses warm orange
The selected bottom navigation item SHALL render its icon and label in the warm orange accent rather than purple.

#### Scenario: Select a destination
- **WHEN** a user taps a bottom navigation item
- **THEN** its selected icon and label use warm orange while unselected items remain neutral

### Requirement: View mode control shares the header row
The Work View day/week/month selector SHALL appear in the right action area of the 工作视图 header, leaving the date row and content more vertical space.

#### Scenario: Open Work View
- **WHEN** Work View is displayed
- **THEN** the mode selector is on the same row as 工作视图 and the date controls are below it

### Requirement: Return action is compact and fixed
The 回到今天 action SHALL remain fixed directly above bottom navigation, use approximately one-fifth of the screen width, and have a compact height.

#### Scenario: Browse another date
- **WHEN** the cursor differs from today
- **THEN** a quiet compact bookmark-like button is visible above navigation and returns to today when tapped

### Requirement: Home grid has balanced density
Home SHALL use tighter grid gaps, larger readable records with reduced row spacing, fixed titles, and no helper footer below the four panels.

#### Scenario: Read Home
- **WHEN** Home is displayed
- **THEN** the four panels sit lower/closer to navigation, titles are distinct from records, and inner scrolling remains available
