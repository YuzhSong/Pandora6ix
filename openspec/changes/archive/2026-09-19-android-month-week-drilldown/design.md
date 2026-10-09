## Context

Month rows currently measure themselves from the number of task bars, causing uneven weekly heights. The Week view also includes a redundant “任务横条” caption above the actual bars.

## Decisions

- Use a fixed 150dp Month week row: date row, three task-bar slots, and an overflow summary line.
- Render `+N条计划` when the week's visible task set exceeds three; retain per-day counts for the day where overflow is visible.
- Route Month selection to `ViewMode.WEEK` with the selected date as cursor; keep Week selection routed to `ViewMode.DAY`.
- Keep continuous bars within each Month week segment and enlarge their labels to 11sp.

## Non-Goals

No change to task dates, mock data, or the Day view.
