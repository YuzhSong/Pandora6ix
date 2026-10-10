## Context

Home is currently a warm-yellow canvas with scrolling panel bodies. View arrows are owned by the shared mode row, and month/week cells route directly to generic details. The requested interaction makes the dashboard static and places date controls near the data they change.

## Goals / Non-Goals

**Goals:** Match the cream Home content surface, provide a compact date/mail header, keep ten preview rows static, make controls stable at the bottom, and route calendar selection through day view.

**Non-Goals:** No new data model, persistence, network layer, or changes to the existing overlay behavior.

## Decisions

- Keep the outer Home column non-scrollable and remove `verticalScroll` from panel preview columns; use compact bounded text so ten rows fit the fixed cards.
- Build Home header as a row with `DemoDate.shortLabel()` on the left and a circular mail `IconButton` on the right.
- Pass date-change/select callbacks into Day, Week, and Month views. Each view owns its date-row arrows; ViewScreen only owns the mode menu.
- Render “回到今天” in a `Box` aligned to the bottom end of View content, above the scaffold navigation, with a left-rounded bookmark silhouette.

## Risks / Trade-offs

- [Dense cards] Ten static rows are compact on phone screens → use one-line ellipsis and preserve expanded overlay for full reading.
- [Day-first navigation] Task bars cannot infer an exact tapped column → choose the task's visible start date for the day transition.

## Open Questions

- Confirm final mail action behavior once notification requirements are defined.
