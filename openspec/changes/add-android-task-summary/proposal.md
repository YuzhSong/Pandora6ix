## Why

Android users need a compact route from the existing calendar to concrete tasks, direct reports and recent journals. The confirmed task-summary brief and references B/C define the scope; no dashboard metrics or new bottom navigation are needed.

## What Changes

- Add a small task-summary button on the existing calendar control row.
- Add a three-panel summary, authorized organization browser and member detail using existing repositories and detail screens.
- Preserve calendar selection/filter/scroll state and summary state through secondary navigation.
- Surface missing member contact/rank and reporting-chain data honestly; do not use admin APIs or invented records.

## Capabilities

### New Capabilities
- `android-task-summary`: Scoped task lists, direct reports, organization browsing, member tasks/journals and recent journal navigation.

### Modified Capabilities
None.

## Impact

Android Compose navigation, calendar header, reusable remote avatar and a new summary feature. Existing `/tasks`, `/tasks/recipient-candidates`, `/work-logs` and avatar endpoints only. No backend/web changes, schema changes, dependencies or infrastructure. User explicitly authorized direct implementation after inspection.
