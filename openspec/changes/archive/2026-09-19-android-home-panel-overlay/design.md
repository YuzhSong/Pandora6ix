## Context

Home currently renders four equal cards in a compact two-row layout and routes taps to a generic detail screen. The requested interaction is a dashboard-preserving overlay: the selected card grows toward the viewport center, rises above the other cards, and leaves a dimmed glimpse of the underlying dashboard. The emulator viewport is narrow, so all sizing must remain constraint-based.

## Goals / Non-Goals

**Goals:**

- Use the full content area for a balanced 2x2 dashboard.
- Render up to ten items per card without overflowing the screen.
- Implement a lightweight Compose animation for expand/close, with an accessible close button and scrim dismissal.
- Match the requested warm cream/orange/yellow palette without importing reference artwork.

**Non-Goals:**

- Changing task or log semantics, adding persistence, or replacing centralized mock data.
- Recreating the reference app's urgent/important categories or illustrations.

## Decisions

- **Overlay state in HomeScreen.** Keep `expandedPanel` local to Home and render a `Box` containing the dashboard plus a conditional full-screen scrim/overlay. This avoids introducing navigation or dialog dependencies while allowing the underlying cards to remain visible.
- **Animated scale/alpha/offset.** Use `Animatable`-free `animateFloatAsState` and `AnimatedVisibility`/`scale` for a deterministic low-fi transition; the selected panel is re-rendered as a larger card centered in the available content area.
- **Scrollable item region.** Increase card height with the parent constraints and put the ten preview rows in a clipped `Column` with a vertical scroll state. The overlay uses the same data and can show all ten records.
- **Palette tokens.** Add/consume dedicated warm surface, orange border, and yellow background colors in the existing theme rather than hard-coding unrelated pastel colors per card.

## Risks / Trade-offs

- [Small screens] Ten rows cannot all remain visible at once → keep the card body scrollable and truncate individual lines with ellipsis.
- [Animation semantics] A low-fi overlay is not a full shared-element transition → make the scale/alpha and scrim clear enough for stakeholder walkthroughs and keep close behavior reliable.

## Migration Plan

Update Home composables and theme tokens, build/install the debug APK, then smoke-test panel open, scroll, scrim close, and bottom navigation. Rollback is a single commit revert because mock data and other screens remain untouched.

## Open Questions

- Final panel ordering, exact item typography, and whether the expanded panel should support swipe-to-close need stakeholder confirmation.
