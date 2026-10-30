# Validation

- `openspec validate week-view-task-stacks --strict`: PASS.
- `git diff --check`: PASS.
- Added WeekTaskStackTest: start time takes precedence over duration/priority; equal start uses total multi-day duration then stable ID; all-day midnight and inclusive end; continuation uses original start and midnight-ending tasks do not appear in zero-length final-day intervals; empty lists.
- Static review: week branch invokes WeekTaskStacks rather than TimeGrid, removing all hourly and current-time lines. Header and body share 8dp outer padding and seven equal-width columns. Cards have fixed 88dp height with 4dp gaps; each opens its own task. Day view continues TimeGrid. Existing date header navigation and outer swipe/pinch gestures are preserved.
- Android compilation, test execution, APK build and visual device checks remain pending: user rejected this change's elevated Gradle build request. No alternative build execution or device installation attempted.
