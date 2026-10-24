## Why

The week view currently renders each task as separate day cells, which hides the continuity of a multi-day task. Month navigation also uses an incorrect shared cursor adjustment, and users have no quick way to return from a different date range to the demo's current day.

## What Changes

- Render week tasks as continuous bars spanning their covered date columns, with separate lanes for overlaps.
- Make month arrows explicitly move one month backward or forward.
- Simplify week range text to a date-only range.
- Show a “回到今天” action whenever the selected day/week/month is not anchored on the demo today and restore the active mode to today when tapped.

## Capabilities

### New Capabilities
- `view-gantt-navigation`: Continuous task bars and consistent date-range navigation.

### Modified Capabilities

## Impact

- Android View composables and date navigation state only; no new dependency or backend changes.
