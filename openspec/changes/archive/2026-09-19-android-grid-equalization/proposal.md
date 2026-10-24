## Why

The latest screenshots show the View mode action too close to the right edge, and the Home grid does not communicate a strict equal-cell rhythm. The panel border is also more saturated than the requested soft orange.

## What Changes

- Keep a right-side margin around the View mode action while preserving a header grid.
- Make all four Home panels explicit equal-height/equal-width grid cells with tighter outer and inner gaps.
- Use a lower-saturation warm-orange border token.

## Capabilities

### New Capabilities
- `grid-equalization`: Equal Home cells and inset-safe View header actions.

### Modified Capabilities

## Impact

- Android theme token and MainActivity layout only.
