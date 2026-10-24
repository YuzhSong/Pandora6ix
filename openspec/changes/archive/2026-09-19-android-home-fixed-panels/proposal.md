## Why

The expanded Home dashboard currently allows the entire page to scroll, which makes the four-panel composition move away from the background. The top title bar also reads as a separate white strip instead of one continuous warm dashboard.

## What Changes

- Keep the four Home panels fixed in the viewport while retaining scrolling only inside each panel's item list.
- Blend the Home title bar into the warm dashboard background instead of rendering a white strip.

## Capabilities

### New Capabilities
- `home-fixed-panels`: Fixed Home composition with independently scrollable panel content.

### Modified Capabilities

## Impact

- Android Home layout and shared page-header container color only; no data, dependencies, or other modules change.
