## Why

The task dispatch overlay is too short for the information a manager must enter, and the current accent-colored fields distract from the low-fidelity review. The walkthrough needs a clearer priority control, a larger notes area, and a compact but usable multi-select recipient list.

## What Changes

- Extend the task dispatch overlay vertically while preserving the layered modal pattern.
- Add a one-to-five-star priority selector below the task name.
- Increase the task notes area and recipient list viewport; keep recipient rows compact and scrollable.
- Use neutral gray field borders and controls for the dispatch and daily-log forms.

## Capabilities

### New Capabilities
- `task-dispatch-form`: Priority, notes, recipient selection, and neutral visual treatment for the manager dispatch overlay.

### Modified Capabilities

## Impact

- Android Compose task dispatch and daily-log overlays only.
- Mock interaction state; no backend, persistence, or network changes.
