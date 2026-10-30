## Why

Only the year calendar exposes task scope controls. Users need the same permission-aware selection in day, week, and month views, with the displayed tasks matching that selection.

## What Changes

- Show existing task scope controls in all calendar modes.
- Apply selected scope when loading tasks in every mode and preserve scope across mode changes.
- Keep managed scope restricted to users with review capability and retain permission fallback.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calendar-view`: Consistent permission-aware task scope selection across calendar modes.

## Impact

Android calendar UI and task loading through existing endpoints; no new business APIs or dependencies.
