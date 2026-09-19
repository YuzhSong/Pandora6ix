## Why

The latest walkthrough shows two interaction mismatches: Home's cards inherit a warm dashboard surface and scroll internally, while the desired composition uses the normal cream content surface and static card content. View navigation also needs fixed bottom controls and day-first selection from week/month content.

## What Changes

- Restyle Home content with the shared cream background, replace the Pandora title with date text and a circular mail button, and make card previews static.
- Move day/week/month date arrows into each view's date row and add a fixed bookmark-shaped “回到今天” control above bottom navigation.
- Make week/month date or task content select the date and enter day view before any detail state.

## Capabilities

### New Capabilities
- `home-view-polish`: Refined Home header/static panels and fixed, day-first view navigation.

### Modified Capabilities

## Impact

- Android `MainActivity` Home/View composables and existing theme only; no new dependencies or backend changes.
