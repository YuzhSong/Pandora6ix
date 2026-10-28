## Context

The month list is anchored to today ±5 years and retains a LazyListState outside the mode branch. Missing jump targets are silently consumed. Its initial scroll observer can overwrite the cursor before a pending jump completes. Week/day rendering uses the cursor directly, but their headings omit the year and returning to month can revive its old position.

## Goals / Non-Goals

**Goals:** Make explicit date navigation authoritative, rebuild bounded month content around distant targets, recenter on every entry to month, and retain date-relative week/day navigation.

**Non-Goals:** Backend contracts, task features, or changing the date picker's year range.

## Decisions

- Keep the bounded month index for cost control, but anchor it to the selected year on explicit navigation and recreate list state when that anchor changes. A fixed today-based range cannot support the picker; eagerly generating 201 years is unnecessary.
- Suppress scroll reports while an explicit jump is pending; report only actual scrolling, using current callbacks and index. Initial/restored positions must not overwrite navigation.
- Set a fresh jump target whenever entering a scroll-based mode. Week/day consume no month/year jumps.
- Show years in week/day headings and centralize adjacent-period arithmetic for regression tests.
- Year-mode explicit navigation replaces the grid composition with a new navigation revision. An immutable year window determines both month content and initial LazyGridState position; no effect changes the anchor and scrolls an old state. This includes repeat navigation to the same date and returning to the current year.
- Own year scroll state inside its window. Edge recentering replaces that window/state together, preserving the first visible month and pixel offset. Scope observers to the window/state and deduplicate year reports, so disposed grids cannot overwrite a new selection. Retain the existing bounded, continuously recentered year range.
- Build month indexes lazily, update the month anchor only for month navigation, and remember visible year-card month weeks. Do not eagerly build offscreen calendar content when selecting years.

## Risks / Trade-offs

### Scroll performance follow-up

- Compare valid DemoDate values by year/month/day without Calendar allocation; retain ordinal and date arithmetic semantics. Generate a month's consecutive dates with one mutable Calendar per month instead of one per day.
- Remember month/week task lane calculations by immutable snapshots of tasks and displayed dates. Use stable month-row keys and content types; replace the redundant task-lane BoxWithConstraints with Box.
- Year cards keep an individual clickable day and text semantics, but draw the today circle/task dot behind a single day container instead of nesting a column and two boxes. Cache first-matching task colors by month and task snapshot; retain marker priority and blank neighboring-month semantics.
- Canonicalize query anchors to year/month/Monday/day, so cursor changes within one period do not reload unchanged data. Propagate cancellation before failure handling so obsolete requests cannot clear tasks or show spurious errors. No debounce or change to request scope/API.
- Validate date ordering and generated weeks against the previous Calendar-based calculations, including leap years and DST. Do not claim an FPS improvement without an equivalent device frame comparison.

- Compose effect ordering can revive old positions → pending jumps take priority over scroll feedback; manually verify after build on a device.
- A bounded month window still limits a single uninterrupted browse to its generated range; explicit navigation supports the entire picker range.

## Migration Plan

Install the updated Android build; no data migration. Revert the scoped calendar change to roll back.

## Open Questions

None for implementation. Device UI verification remains dependent on an available device.
