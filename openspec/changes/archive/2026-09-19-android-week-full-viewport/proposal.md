## Why

Week view currently places the gantt area in a fixed-height box, leaving unused screen space between the task bars and the bottom navigation. The entire area below the date header should be available for the timeline.

## What Changes

- Remove Week view's internal scrolling and fixed timeline height.
- Let the task timeline fill all remaining space between the date row and the bottom navigation.

## Capabilities

### New Capabilities
- `week-full-viewport`: Full-height Week timeline layout.

### Modified Capabilities

## Impact

- Android Compose Week view layout only.
