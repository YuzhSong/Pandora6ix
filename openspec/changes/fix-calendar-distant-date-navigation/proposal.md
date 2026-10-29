## Why

Selecting a distant date can update the calendar heading without moving the month list. Its fixed range and retained scroll position can overwrite the selected date, including when returning from week/day modes.

## What Changes

- Position month content at the selected date throughout the date picker's supported years.
- Recenter month content on mode entry and prevent old scroll reports from overriding explicit navigation.
- Preserve the selected period when navigating adjacent days/weeks and expose its year in headings.
- Add regression coverage for distant past/future navigation and period boundaries.
- Position a fresh year grid at the selected month in a single navigation update, cancel old scroll state, and preserve the visible month/offset when recentering near a window edge.
- Defer hidden month-list construction and cache year-card dates to reduce unnecessary work during year navigation.
- Reduce scroll-time calendar allocations, cache task layouts, flatten year day-cell layout while preserving clicks/accessibility, and deduplicate task requests within the displayed period.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calendar-view`: Date selection, visible content and subsequent navigation must share the selected date across modes.

## Impact

Android calendar navigation and tests only; no backend API, database or infrastructure changes.
