## Context

The reference shows five simple line-style symbols: a four-square grid for 首页, a calendar for 视图, a note with pencil for 日志, a brain for AI地图, and an outlined person for 我的.

## Goals / Non-Goals

**Goals:** Improve semantic recognition and visual alignment with the reference while preserving the current Material 3 navigation component and orange selected state.

**Non-Goals:** No custom bitmap assets, route changes, label changes, or new dependencies.

## Decisions

- Use the closest built-in Compose Material Icons so the prototype remains self-contained and vector-based.
- Map Home to `GridView`, View to `CalendarMonth`, Logs to `NoteAlt`, AI map to `Psychology`, and Profile to `PersonOutline`.
- Keep the existing `NavigationBarItem` sizing and color configuration.

## Risks / Trade-offs

- Built-in icons may differ slightly from the reference glyphs, but avoid asset licensing and density issues.

## Open Questions

- A later visual pass can replace these vectors with an approved custom icon set if exact artwork is supplied.
