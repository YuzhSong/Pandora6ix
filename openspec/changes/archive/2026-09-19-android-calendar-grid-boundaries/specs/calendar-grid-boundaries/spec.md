## ADDED Requirements

### Requirement: Home cells share available height
The four Home panels SHALL be equal-height and equal-width within a two-by-two grid on the available screen.

#### Scenario: Resize Home
- **WHEN** Home is displayed on a supported phone size
- **THEN** both rows and all four panel cells use the same measured height and aligned edges

### Requirement: Month ends at the month-ending week
Month view SHALL include dates through the last day of the selected month and only the trailing days from that same calendar week.

#### Scenario: View September
- **WHEN** September is selected and its last day is Tuesday
- **THEN** the grid includes the following Wednesday through Sunday in that week but does not create another week

### Requirement: Calendar guides mark boundaries
Week and Month views SHALL use light dashed vertical guides between day columns and dashed horizontal separators between rows.

#### Scenario: Read task alignment
- **WHEN** a calendar view is displayed
- **THEN** guides sit between adjacent days and task labels remain readable with the increased task text size
