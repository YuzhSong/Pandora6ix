## ADDED Requirements

### Requirement: Home panels use a navigation-matched background
The Home content background SHALL use the same pale warm-yellow surface as the bottom navigation while panel cards retain their cream surfaces.

#### Scenario: Open Home
- **WHEN** Home is displayed
- **THEN** the area surrounding all four cards matches the bottom navigation color

### Requirement: Panel titles stay fixed while records scroll
Each Home panel SHALL keep its title and footer fixed while the record region scrolls independently through up to ten records.

#### Scenario: Read all records
- **WHEN** a user swipes inside a panel's record region
- **THEN** the title remains in place and additional records become visible through the tenth item

### Requirement: Panel records are readable and separated
Panel records SHALL use legible typography, generous row spacing, and visible low-contrast dashed separators without overflowing their card bounds.

#### Scenario: Inspect panel records
- **WHEN** a panel is visible
- **THEN** each row is readable, separated from its neighbor, and truncated safely when text exceeds its row
