## ADDED Requirements

### Requirement: Scroll work reuses period data

Calendar scrolling SHALL reuse unchanged date/task layout calculations and preserve current day highlights, task markers, individual date clicks and accessibility text. A cursor move within an unchanged mode, task scope and displayed period SHALL NOT trigger another task read unless explicitly retried. A cancelled obsolete request SHALL NOT clear current tasks or report a loading failure.

#### Scenario: Same period
- **WHEN** the selected day changes within the same displayed year, month or Monday-based week
- **THEN** the existing period query is reused while the new cursor remains selected

#### Scenario: Updated tasks
- **WHEN** tasks or tag filters change
- **THEN** cached task layouts and year markers refresh using the new task content

#### Scenario: Drawing and interaction
- **WHEN** a year day cell is displayed or tapped
- **THEN** its date text, today circle, first matching task marker and selected date behavior remain unchanged

#### Scenario: Rapid period changes
- **WHEN** a period request is cancelled by a newer selection
- **THEN** cancellation is propagated without applying failure UI or clearing tasks from the newer request

### Requirement: Year selection positions content atomically

Year mode SHALL position the selected month in its target year without requiring a follow-up gesture or traversing intervening years. A newer explicit navigation SHALL replace the old scroll state, including an active fling; obsolete layout reports SHALL NOT overwrite its year. Only visible month cards SHALL be generated and hidden month-mode indexes SHALL NOT be eagerly built on a year jump. Edge recentering SHALL preserve the visible month and pixel offset.

#### Scenario: Distant year selection
- **WHEN** the user confirms a date in 1900, 2030, 2090 or 2100 in year mode
- **THEN** the next grid is initialized at that date's month in the selected year without waiting for another drag

#### Scenario: Repeated and interrupted selections
- **WHEN** the user selects a distant date during a fling, rapidly selects another year, or selects the same date after scrolling away
- **THEN** the latest selection wins and the old grid cannot restore its year or position

#### Scenario: Window edge
- **WHEN** continuous year scrolling reaches either edge recenter threshold
- **THEN** the new window preserves the first visible month and pixel offset and scrolling can continue through neighboring years

#### Scenario: Return and mode entry
- **WHEN** the user returns to the current year or enters year mode from a distant month/day/week
- **THEN** year content initializes at the current cursor's year and month with no retained obsolete grid position

### Requirement: Explicit date navigation remains authoritative

The Android calendar SHALL position visible content at the explicitly selected date in month, week and day modes throughout the date picker's supported 1900–2100 years. Headings SHALL identify the displayed year. Subsequent gestures SHALL navigate relative to that selected period. Entering month mode SHALL position its list at the current cursor; old scroll state SHALL NOT overwrite an explicit selection.

#### Scenario: Distant future month
- **WHEN** the user selects June 2030 or June 2090 in month mode
- **THEN** the visible month is June of that year and scrolling continues through adjacent months of that year

#### Scenario: Distant past month
- **WHEN** the user selects February 1900 or February 2000
- **THEN** the month content and heading show that year with the correct dates

#### Scenario: Adjacent week and day
- **WHEN** the user selects a date in 2030 or 1990 in week/day mode and swipes
- **THEN** the next or previous period is computed from the selected date, including crossing month/year boundaries

#### Scenario: Change mode after selection
- **WHEN** the user selects a distant date in week/day mode and switches to month, or enters month from the year grid
- **THEN** month content positions at that date's month without returning to its previous scroll position

#### Scenario: Return to current period
- **WHEN** the user returns to the current period after browsing a distant date
- **THEN** both heading and visible content return to the actual current period and subsequent gestures continue from there
