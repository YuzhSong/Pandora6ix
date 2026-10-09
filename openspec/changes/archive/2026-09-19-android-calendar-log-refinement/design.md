## Context

The existing prototype has shared task data and navigation, but the calendar needs stronger visual alignment between dates and multi-day tasks. The Logs destination is intentionally mock-only for this phase.

## Goals / Non-Goals

**Goals:** Make dates self-explanatory, keep return-to-today readable, visually guide Week columns, preserve task continuity in Month rows, and give Logs a useful demo flow.

**Non-Goals:** No persistence, server calls, approval rules, or new business semantics.

## Decisions

- Add a weekday helper to `DemoDate` and reuse it in Home and Day labels.
- Use a fixed quarter-width return button and a no-wrap label.
- Use a divider under Home panel titles.
- Draw light dashed vertical guides behind Week task bars.
- Represent each Month week as date cells plus continuous task bars; show per-day `+N条计划` when more than two tasks overlap.
- Use a two-option Logs segmented control backed by the existing mock logs and tasks.

## Risks / Trade-offs

- Monthly rows become taller and scrollable to keep task labels readable; this is intentional for the low-fidelity review.
