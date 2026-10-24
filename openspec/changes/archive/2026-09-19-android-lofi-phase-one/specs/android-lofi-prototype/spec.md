## ADDED Requirements

### Requirement: Pandora shell exposes five top-level destinations
The Android prototype SHALL display the Pandora name and provide Home, View, Logs, AI Map, and Me destinations through a fixed bottom navigation bar with visible selected state.

#### Scenario: Navigate between destinations
- **WHEN** a user taps any bottom navigation item
- **THEN** the selected item is highlighted and the corresponding page content replaces the previous page without a crash

#### Scenario: Content respects insets and scrolling
- **WHEN** a page contains content taller than the viewport
- **THEN** the page scrolls within scaffold content and the bottom navigation remains usable without covering the last record

### Requirement: Home presents the four Pandora business panels
The Home page SHALL show exactly four primary panels named 公司十大重要事项, 公司十大派发任务, 个人十大重要事项, and 个人日志, with concise mock previews and responsive two-column layout.

#### Scenario: Home panel content
- **WHEN** Home is opened
- **THEN** all four named panels are visible with mock data that reflects their business meaning and no urgent/important quadrant labels

#### Scenario: Open a panel list
- **WHEN** a user taps any Home panel
- **THEN** an in-app list/detail state opens for that panel and provides a way to return to Home

### Requirement: View supports shared day, week, and month modes
The View page SHALL offer a mode menu containing 日视图, 周视图, and 月视图 and render all modes from the same centralized mock task and log collections.

#### Scenario: Change view mode
- **WHEN** a user opens the mode menu and selects a mode
- **THEN** the menu closes, the page title updates, and the selected mode content is shown

#### Scenario: Week task duration
- **WHEN** a task spans multiple dates in the selected week
- **THEN** its colored cells form a date-aligned bar across each covered day, with separate rows for overlapping tasks and a clickable task entry

#### Scenario: Month navigation and day selection
- **WHEN** a user changes month or taps a calendar day
- **THEN** the month grid updates or the selected day is reflected in the day-information state without changing task start/end dates

#### Scenario: Day task and log details
- **WHEN** a user opens a day containing tasks or logs
- **THEN** matching tasks and logs are listed and each item can open readable detail content

### Requirement: Prototype supplies basic Logs, AI Map, and Me pages
The Logs page SHALL expose mock entry points for work logs and task management; AI Map SHALL clearly identify itself as planned/offline; Me SHALL show mock user information.

#### Scenario: Open supporting pages
- **WHEN** a user navigates to Logs, AI Map, or Me
- **THEN** a non-empty page with the requested labels is displayed and no network or backend call is attempted
