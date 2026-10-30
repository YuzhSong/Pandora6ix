## ADDED Requirements

### Requirement: Week view ordered daily task stacks
Week view SHALL show seven aligned date columns with non-overlapping equal-height task cards and no hourly axis, hourly lines or time-proportional placement. Each daily list SHALL sort by original start date/time ascending, then total duration descending, then ID for stability. All-day tasks SHALL use midnight starts and inclusive end dates. Cross-day tasks SHALL appear on each covered date using original timing for ordering. Day view SHALL retain its timeline.

#### Scenario: Earlier starts first
- **WHEN** tasks start at different times
- **THEN** earlier-starting tasks appear above later ones regardless of priority and duration

#### Scenario: Same start longer first
- **WHEN** tasks have the same original start date/time
- **THEN** longer total duration appears first, including cross-day duration

#### Scenario: Overlapping and all-day tasks
- **WHEN** overlapping timed and all-day tasks cover a visible date
- **THEN** all tasks appear as separate stacked cards with title and start-time or all-day labels

#### Scenario: Navigation and selection
- **WHEN** a user taps a card, taps a date header, or swipes to another week
- **THEN** the card opens its task, the date header opens day view, and the swipe changes week using the existing navigation
