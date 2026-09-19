## Context

NavigationBarItem currently receives Material 3 defaults, so its selected indicator is lavender. ViewScreen puts its mode chip in a separate row, and Home cards use large row padding plus a helper caption that pushes the bottom row away from navigation.

## Goals / Non-Goals

**Goals:** Keep controls close to their context, emphasize selected navigation with orange, make the return action quiet and fixed, and use the phone viewport more efficiently on Home.

**Non-Goals:** No behavioral changes to task data, date calculations, overlay content, or routes.

## Decisions

- Configure `NavigationBarItemDefaults.colors` with `WarmOrange` selected icon/text and a subtle warm indicator.
- Extend `PageHeader` with optional trailing actions and render the ViewMode dropdown there; remove View's separate mode row.
- Use a bottom-aligned button with `fillMaxWidth(0.2f)`, 36dp height, and left-rounded shape.
- Set Home cards to 350dp, use 8dp row gap, 14sp titles, 15sp records, 4dp row padding, and remove the footer caption.

## Risks / Trade-offs

- [Small screen] Taller cards may reduce visible lower-page margin → internal lists remain scrollable and the bottom row can sit near navigation.
- [Header width] ViewMode chip may compete with title on narrow screens → only the compact mode chip appears in the action slot.

## Open Questions

- Confirm whether orange should also style unselected navigation labels or only selected state.
