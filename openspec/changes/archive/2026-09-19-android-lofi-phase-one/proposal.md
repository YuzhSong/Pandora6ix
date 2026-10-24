## Why

The Android module currently contains only a Compose starter screen, so the team cannot demonstrate Pandora's core work-information flows in the upcoming stakeholder review. This change delivers a self-contained, offline low-fidelity prototype on the `LoFi` branch so the navigation, information panels, and date-based work views can be exercised before backend contracts are finalized.

## What Changes

- Add a five-section Pandora Android shell with a fixed bottom navigation bar and lightweight page transitions.
- Add a cream/coral visual theme and reusable cards, navigation, task-chip, and date-view components.
- Add centralized mock company, user, task, and log data for consistent demonstrations.
- Implement the Home page's four business panels: company important items, company dispatched tasks, personal important items, and personal logs.
- Implement View day, week, and month modes with shared task data, date navigation, task/log detail entry points, and non-destructive placeholder interactions.
- Add basic Logs, AI Map, and Me pages; AI Map remains an explicit offline placeholder.
- Change the Android display name shown in the app to `Pandora` and leave backend, web, formal product document, and `main` unchanged.

## Capabilities

### New Capabilities

- `android-lofi-prototype`: Offline Compose prototype covering the five top-level pages, mock data, Home panels, and day/week/month work views.

### Modified Capabilities

<!-- No existing OpenSpec capabilities are present; this is a new prototype capability. -->

## Impact

- Android source under `android/app/src/main`, including the activity, theme, navigation, screen, component, and mock-data packages.
- Android manifest display label only; no new network, database, AI, or third-party service dependencies.
- New OpenSpec planning/specification artifacts under this change directory. The formal product document and other modules are intentionally not modified.
