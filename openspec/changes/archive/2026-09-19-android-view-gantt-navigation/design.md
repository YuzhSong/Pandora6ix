## Context

The existing week implementation paints seven independent boxes for each task. The same task dates are already centralized in `MockData`, so a continuous bar can be produced by placing one weighted row spanning the task's start and end columns. Month navigation needs a calendar-safe month delta instead of fixed day counts.

## Goals / Non-Goals

**Goals:** Preserve shared task dates, make spans visually continuous, correct month movement, and provide a mode-aware return-to-today action.

**Non-Goals:** No production calendar library, time-zone logic, persistence, or changes to Home/Logs/AI/Me.

## Decisions

- Add `DemoDate.plusMonths` and use it for month arrows.
- Render each week task as a `Row` of leading spacer, one weighted colored bar whose width is proportional to inclusive duration, and trailing spacer; assign a lane by task order so overlaps remain visible.
- Compute today anchoring with `MockData.demoToday`; show a compact button at the bottom of View when cursor does not equal today. Returning sets the shared cursor to today without changing the selected mode.

## Risks / Trade-offs

- [Narrow phone] Long bar labels can truncate → keep ellipsis and make the bar clickable for detail.
- [Month view] Six-week grids may include adjacent-month days → retain the existing muted day styling.

## Open Questions

- Confirm whether the final product should use device-local today instead of the fixed demo date.
