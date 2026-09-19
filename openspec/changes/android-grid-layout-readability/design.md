## Context

Home cards now intentionally remain fixed, but their content columns are also static and use 9sp text. The requirement is a nested interaction: the card shell and title stay in place while the list viewport scrolls. A lightweight dashed divider can be drawn with text rather than a custom canvas.

## Goals / Non-Goals

**Goals:** Restore the navigation-matched background, improve legibility, expose all ten records through per-card scrolling, and standardize spacing across Home and View.

**Non-Goals:** No change to panel overlay semantics or View business data.

## Decisions

- Use `WarmDashboard` as the Home root and keep `WarmCard` surfaces.
- Keep titles outside the scroll modifier; apply `verticalScroll` only to the item `Column` with `weight(1f)`.
- Render each record with 13sp text, 8dp vertical padding, and a light dashed Unicode divider to avoid adding a drawing dependency.
- Increase outer horizontal padding and gaps in Home/View while retaining responsive weights.

## Risks / Trade-offs

- [Dense ten-item cards] Larger type reduces visible rows → inner scrolling is the intended way to reach items 7–10.
- [Unicode divider] Divider appearance varies slightly by font → use a low-contrast dotted line that remains readable across devices.

## Open Questions

- Confirm whether the final design wants colored section-specific dividers or one neutral divider.
