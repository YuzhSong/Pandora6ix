## MODIFIED Requirements

### Requirement: Preserve explicit calendar navigation
The Android prototype SHALL expose day, week, month and year modes. Pinch gestures SHALL select adjacent modes in that order. Selecting a date in week mode SHALL open that day in day mode, as specified by android-calendar-swipe-nav. Selecting a date in month or year mode SHALL retain the current mode, and selecting a task SHALL directly open its detail. Overflow SHALL remain accessible without forced drill-down.

#### Scenario: Month task selection
- **WHEN** a user taps a task in month mode
- **THEN** its detail opens directly without changing the calendar mode

#### Scenario: Date and mode selection
- **WHEN** a user selects a date in month mode and then explicitly chooses day mode
- **THEN** the date tap retains month mode and the explicit choice opens the selected day

#### Scenario: Week date selection
- **WHEN** a user taps a date in week mode
- **THEN** day mode opens on that selected date

#### Scenario: Pinch boundaries
- **WHEN** a user zooms in from week or out from month
- **THEN** day or year mode respectively is selected, and zooming beyond day/year keeps that boundary mode

#### Scenario: Large font calendar dates
- **WHEN** the user selects the large font setting and opens the year or month calendar
- **THEN** date digits remain legible without inheriting a line height larger than their date cell

## ADDED Requirements

### Requirement: Consistent year-grid positioning
The year calendar SHALL keep its year heading and visible grid synchronized. Returning to today after scrolling into an adjacent year SHALL reposition the grid to the current year. Year reporting SHALL preserve a valid selected date, and window reanchoring SHALL retain the visible scroll offset.

#### Scenario: Return from adjacent year
- **WHEN** a user scrolls from the current year into the next year and taps return to today
- **THEN** the heading and visible January grid both display the current year
