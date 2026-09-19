## Decisions

- Make the Week root a `fillMaxSize()` column rather than a vertically scrolling column.
- Keep the date range and day labels at the top, then assign the remaining height to the gantt container with `weight(1f)`.
- Keep the existing dashed day-boundary guides, task span calculations, and fixed return-to-today overlay.
