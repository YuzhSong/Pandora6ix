## ADDED Requirements

### Requirement: Day timeline parallel task columns
Day view SHALL render overlapping tasks in disjoint horizontal columns with equal widths within each connected overlap group. Start and end positions SHALL retain their minute alignment. Layout SHALL reuse available columns, remain deterministic regardless of input order, and treat touching endpoints as non-overlapping. Week view SHALL retain its existing layout.

#### Scenario: Two simultaneous tasks
- **WHEN** two day-view tasks overlap in time
- **THEN** they appear side by side without covering each other and retain their start/end timeline positions

#### Scenario: Chained overlaps and separate groups
- **WHEN** tasks overlap indirectly or occupy separate time groups
- **THEN** each group uses its own maximum concurrent column count and reuses free columns; an isolated task occupies full width

#### Scenario: All-day and cross-day tasks
- **WHEN** all-day or cross-day tasks intersect the selected date
- **THEN** their clipped date intervals participate in column allocation without modifying source times

#### Scenario: Select a parallel card
- **WHEN** a user taps or activates a task card in day view
- **THEN** that specific task opens without an overlap candidate chooser
