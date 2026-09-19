## Context

Home's outer `Column` currently owns a vertical scroll state. The requested interaction treats the 2x2 panels as a dashboard canvas, so only the content region inside each panel should consume scroll gestures.

## Goals / Non-Goals

**Goals:** Remove outer Home scrolling, preserve inner item scrolling, and make the title bar visually continuous with the warm background.

**Non-Goals:** No changes to navigation, mock data, overlay behavior, or other pages.

## Decisions

- Remove `verticalScroll` from the Home root column and size it to the scaffold viewport; keep `verticalScroll` on each panel's item column.
- Set `PageHeader`'s app-bar container to transparent so the parent page color shows through consistently.

## Risks / Trade-offs

- [Small viewport] Fixed panels may leave less room for headings → keep item regions independently clipped and scrollable.

## Open Questions

- Confirm whether the same transparent header treatment should be applied to every future page-specific background.
