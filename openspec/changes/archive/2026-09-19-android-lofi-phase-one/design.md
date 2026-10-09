## Context

The Android module is a small Compose starter with no product navigation or business UI. This change is intentionally limited to an offline demonstration on the `LoFi` branch. The supplied screenshots guide spacing, pastel colors, rounded cards, and date-oriented layouts; the prompt defines Pandora's four panel names and the five navigation destinations. Existing Gradle/Compose configuration is retained and no backend contract is available.

## Goals / Non-Goals

**Goals:**

- Provide a single-activity Compose shell with five reachable destinations and a fixed, inset-aware bottom navigation bar.
- Keep mock tasks and logs in one source so Home, day, week, and month views cannot disagree about dates.
- Make the four Home panels and all date-view items clickable, with lightweight detail/list states suitable for a stakeholder walkthrough.
- Keep the code modular enough to replace mock repositories later without introducing a database or network layer now.

**Non-Goals:**

- Authentication, permissions, persistence, API clients, AI calls, analytics, or formal task/log workflows.
- Reproducing the reference app's rabbit artwork or its urgent/important quadrant semantics.
- Changes to `backend/`, `web/`, `main`, or the formal product document.

## Decisions

- **Single activity plus typed UI state.** Use `MainActivity` as the composition root and small screen/component files under `ui/`; an enum/string route state is sufficient for five prototype destinations and avoids adding a navigation dependency.
- **Material 3 primitives only.** Use the existing Compose Material 3, foundation, and activity-compose dependencies for `Scaffold`, cards, menus, lazy layouts, and transitions. This keeps Gradle sync predictable on the already configured SDK.
- **Centralized demo model.** Store `DemoDate`, `WorkTask`, `WorkLog`, and sample collections in `mock/MockData.kt`. Date helpers use `java.util.Calendar`-style arithmetic/custom date values so the API-24 minimum remains safe without adding desugaring dependencies.
- **Grid-cell task bars.** The week and month views render each task across date cells using shared start/end comparisons. Separate rows/layers are used for overlapping tasks; this is more robust on narrow phones than fixed pixel offsets and still communicates duration.
- **Explicit prototype states.** Home panel taps, task chips, day cells, and log cards open an in-app detail/list state with a back affordance. Logs, AI Map, and Me expose only the requested structure, with AI Map clearly labeled as planned.
- **Insets and scrolling.** `Scaffold` content consumes `innerPadding`, and long page content is vertically scrollable so navigation never obscures records or system gesture areas.

## Risks / Trade-offs

- [Prototype-only date model] The custom date helpers are intentionally small and not a production calendar library → keep all date calculations in one file and replace with a tested domain/date layer when real data arrives.
- [No persistence] App restart resets demo interactions → label mock/placeholder behavior in UI and final verification.
- [Narrow-screen density] Month cells may truncate long names → use ellipsis/chips and open the detail state for complete text.
- [Unconfirmed business rules] The draft document contains broader roles, approval, AI, and backend requirements than this walkthrough → keep those out of code and record them as open questions.

## Open Questions

- Final visual tokens, iconography, and empty-state illustrations need stakeholder confirmation.
- The draft's approval/re-submit rule and the exact selection algorithm for personal important items need a product decision before persistence/API work.
