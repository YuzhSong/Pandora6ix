## Why

The latest review found that the Home grid can appear uneven on different screen sizes, and the Month grid includes an unnecessary extra week. Calendar guides should sit on day boundaries and remain light, while task labels need to be more readable.

## What Changes

- Make the Home two-by-two grid consume remaining space with equal row heights.
- Limit Month view to the final week containing the month's last day.
- Use dashed vertical day boundaries and horizontal week separators in Week and Month views, with larger task labels.

## Capabilities

### New Capabilities
- `calendar-grid-boundaries`: Equal Home cells and bounded dashed calendar grids.

### Modified Capabilities

## Impact

- Android Compose Home, Week, and Month layout only; shared mock data remains unchanged.
