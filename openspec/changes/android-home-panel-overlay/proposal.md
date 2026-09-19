## Why

The current Home prototype leaves substantial screen space unused, previews only three records per panel, and opens panel content as a separate generic detail page. The stakeholder reference calls for a more spacious dashboard and a focused, layered panel interaction that keeps the surrounding dashboard visible.

## What Changes

- Enlarge the four Home panels so they use the available viewport while remaining responsive.
- Show up to ten concise items in each panel, with scrolling inside the panel when needed.
- Use one consistent warm-cream card surface, warm-orange border, and pale warm-yellow dashboard background.
- Animate a tapped panel into a centered, elevated overlay with a dimmed backdrop and visible portions of the other panels around its edges.
- Keep a clear close/back action and preserve the existing mock data and bottom navigation.

## Capabilities

### New Capabilities

- `home-panel-overlay`: Spacious Home panels with ten-item previews and layered expand/close interaction.

### Modified Capabilities

<!-- Existing prototype behavior is refined by the new capability; no main spec is changed. -->

## Impact

- Android Home screen composables and theme colors only.
- No new dependencies, APIs, persistence, or changes to `backend/`, `web/`, `main`, or the formal requirements document.
