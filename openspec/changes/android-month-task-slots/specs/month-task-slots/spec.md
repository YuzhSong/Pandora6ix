## ADDED Requirements

### Requirement: Month task area has four equal slots
Each fixed Month week SHALL divide the area below its date row into four equal-height task slots.

#### Scenario: Four or fewer tasks
- **WHEN** a week has at most four visible tasks
- **THEN** each task occupies one slot and its label is vertically centered in a taller task bar

#### Scenario: More than four tasks
- **WHEN** a week has more than four visible tasks
- **THEN** the first three tasks occupy the first three slots and the fourth slot displays `+N条计划` where N is the total visible task count minus three
