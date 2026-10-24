## Context

Home rows currently rely on a fixed card height but not an explicit row height/fill contract, while the View header action has no explicit end inset. The layout should make the grid relationship intentional rather than relying on content measurement.

## Goals / Non-Goals

**Goals:** Equalize the four rounded rectangles, reduce screen/row gutters, soften the orange, and retain a visible header margin.

**Non-Goals:** No changes to scrolling, navigation behavior, or data.

## Decisions

- Give each Home row a fixed shared height and make each child fill that height; use 10dp outer padding and 6dp row/column gaps.
- Add an explicit 12dp end padding to the ViewMode header action.
- Change `WarmOrange` to a muted peach-orange token used by all Home borders/dividers.

## Risks / Trade-offs

- [Compact gutters] Less outer whitespace may feel denser → preserve rounded corners and internal padding.

## Open Questions

- Confirm the final border color against the stakeholder palette.
