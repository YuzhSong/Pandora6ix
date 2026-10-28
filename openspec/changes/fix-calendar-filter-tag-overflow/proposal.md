## Why

Selecting many calendar filter tags compresses their labels and can wrap text or hide controls, reducing space for calendar content.

## What Changes

- Keep selected tags on one horizontally scrollable line.
- Keep the filter prefix and clear action visible, with calendar content below the row.
- Preserve existing selection and AND filtering behavior.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calendar-view`: Require a single-line scrolling selected-tag summary.

## Impact

Android CalendarScreen layout only; no API or dependency changes.
