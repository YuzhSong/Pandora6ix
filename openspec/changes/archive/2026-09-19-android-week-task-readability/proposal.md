## Why

Week task bars currently use a smaller type and shorter height than the Month bars, and ellipsis hides useful labels. The two calendar modes should share a readable task-bar treatment.

## What Changes

- Match Week task-bar height and label size to the Month treatment.
- Remove ellipsis from Week task labels so visible text is shown directly.

## Capabilities

### New Capabilities
- `week-task-readability`: Readable, non-ellipsis Week task bars.

### Modified Capabilities

## Impact

- Android Compose Week task bar only; task span calculations and navigation remain unchanged.
