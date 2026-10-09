## Why

The Month view still scrolls on the phone because weekly rows are taller than the available viewport. The review requires a direct, at-a-glance five-row calendar with a predictable two-task preview per week.

## What Changes

- Render Month view as a non-scrolling five-row grid that fills the available content area.
- Give each week equal height and show two task bars plus a `+N条计划` overflow slot.

## Capabilities

### New Capabilities
- `month-five-row-fit`: Single-screen five-row Month layout.

### Modified Capabilities

## Impact

- Android Compose Month view layout only; navigation and task data remain unchanged.
