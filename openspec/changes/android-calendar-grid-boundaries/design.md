## Context

Home currently uses two fixed 350dp rows, which can be clipped or appear unequal after accounting for headers and navigation. Month always creates 42 cells even when the last row is unnecessary. Existing guides are centered in columns rather than marking boundaries.

## Decisions

- Put the two Home rows inside a weighted grid container and give each row equal weight.
- Compute Month cell count from the first weekday offset and the last day of the selected month, rounded to a whole week.
- Draw dashed guides at six internal day boundaries and at each row bottom; retain the grid's visual structure without adding a seventh trailing week.
- Increase task bar text by one step in both Week and Month views.

## Non-Goals

No changes to task dates, navigation routes, or persistence.
