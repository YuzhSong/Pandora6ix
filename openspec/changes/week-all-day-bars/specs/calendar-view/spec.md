## ADDED Requirements

### Requirement: Continuous all-day bars in week view
Week view SHALL display all-day tasks as continuous bars above timed task cards, aligned to covered date columns and clipped at week boundaries. Overlapping bars SHALL occupy separate rows. Original start ascending, duration descending and ID SHALL determine packing order. All-day tasks SHALL NOT also appear in daily timed stacks. Bars SHALL open their task when activated.

#### Scenario: Multi-day all-day task
- **WHEN** an all-day task covers three visible dates
- **THEN** one continuous bar spans those three columns above timed cards

#### Scenario: Overlap and week boundary
- **WHEN** all-day tasks overlap or extend beyond the current week
- **THEN** overlaps use separate rows and visible bar spans are clipped to this week

#### Scenario: Empty all-day area
- **WHEN** no all-day task covers the week
- **THEN** no empty all-day row is reserved
