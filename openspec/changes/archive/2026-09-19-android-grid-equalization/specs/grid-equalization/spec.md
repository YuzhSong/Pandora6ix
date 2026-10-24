## ADDED Requirements

### Requirement: Home uses equal grid cells
The four Home panels SHALL be equal-width and equal-height rounded rectangles arranged in a two-by-two grid with consistent narrow gutters and reduced screen margins.

#### Scenario: Open Home grid
- **WHEN** Home is displayed
- **THEN** all four panels share the same dimensions and aligned edges

### Requirement: View header action remains inset
The View mode selector SHALL remain on the right side of the 工作视图 header while retaining a visible end margin rather than touching the screen edge.

#### Scenario: Open View
- **WHEN** the View page is displayed
- **THEN** the mode selector is aligned to the header grid with a right-side inset

### Requirement: Border uses muted warm orange
Home panel borders SHALL use a lower-saturation warm-orange accent.

#### Scenario: Inspect panels
- **WHEN** Home panels are displayed
- **THEN** their borders use the muted warm-orange token consistently
