# Validation — 2026-10-28

## Latest scroll-performance changes — not yet built

- Implemented allocation-free comparison of valid dates by year/month/day. Consecutive month days now share one Calendar iterator instead of allocating a Calendar per output day; ordinal/date arithmetic APIs remain unchanged.
- Cached month/week task lanes by displayed dates and task-content snapshots. Fixed tag-filter caching to track task content, added distinct stable month-row keys/content types and removed a redundant BoxWithConstraints from task lanes.
- Replaced each year day cell's Column plus two Boxes with one clickable Box. Today circle/task dot are drawn behind the retained date Text; task colors are cached by month and task snapshot.
- Period requests now use canonical year/month/Monday/day anchors. Obsolete requests propagate cancellation rather than applying error/empty state, and cannot clear the newer request's loading flag.
- Added CalendarScrollCalculationTest with five cases: ordering equivalence, month generation equivalence through leap years and DST in three timezones, unique month-row keys, same-period anchor reuse and cross-period invalidation.
- The Gradle command for unit tests, assembleDebug and lintDebug was rejected by the user before execution. Do not infer success from the earlier 44-test result or existing APK; those belong to the previous year-navigation implementation. No retry, alternative test execution, APK rebuild or installation was attempted after rejection.
- Current checks: `git diff --check` PASS; strict OpenSpec validation PASS. Runtime/build verification and before/after device frame measurements remain pending (task 2.5). No quantified FPS or smoothness claim is made.

## Earlier month/week/day validation

- `:app:testDebugUnitTest`: PASS, 40 tests, including 4 new CalendarNavigationTest cases covering real month content/neighbor mappings at 1900, 1990, 2000, 2030, 2090 and 2100, leap-century arithmetic, day/week year boundaries and year-aware headings.
- The default Windows test worker failed to load GradleWorkerMain through the Chinese user directory. Re-running with `-g E:\S\Pandora6ix\android\.gradle\verification-user-home` passed. This ignored local junction points to the existing user Gradle directory; no project dependency/configuration changes were needed.
- `:app:assembleDebug`: PASS. APK: `android/app/build/outputs/apk/debug/app-debug.apk`.
- `:app:lintDebug`: FAIL on 3 existing errors, all confirmed present in HEAD: NotificationHelper.kt MissingPermission, CalendarScreen.kt UnusedBoxWithConstraintsScope at the existing task lane, AndroidManifest.xml PermissionImpliesUnsupportedChromeOsHardware. No new lint errors from this change.
- `openspec validate fix-calendar-distant-date-navigation --strict`: PASS.
- `git diff --check`: PASS.
- Initial validation had no attached devices. See the year-mode follow-up below for the subsequent partial device verification; task 2.3 remains open for the complete acceptance matrix.

## Year-mode follow-up

- Replaced the two-effect anchor/scroll sequence with an immutable YearGridWindow and a fresh LazyGridState, initialized together. Explicit navigation replaces the grid with a new revision, and old callbacks check that revision even before recomposition. Edge recentering preserves month and pixel offset with a new window/state.
- Deferred hidden month-list generation and cached month weeks and overlapping-task candidates for visible year cards.
- Final `:app:testDebugUnitTest`: PASS, 44 tests, 0 failures. Four new YearGridWindowTest cases cover all 12 months of 1900/1990/2000/2030/2090/2100, adjacent-month mappings, both recenter thresholds with pixel offsets, ordinary scrolling and 20 successive recenters in both directions.
- Final `:app:assembleDebug`: PASS; APK regenerated with the callback revision guard and rendering optimization.
- Final `:app:lintDebug`: FAIL, same 3 pre-existing errors (MissingPermission, UnusedBoxWithConstraintsScope, PermissionImpliesUnsupportedChromeOsHardware); 25 warnings. No new lint errors.
- Connected V2561A phone detected on follow-up; `adb install -r` succeeded, preserving application data, and Pandora opened with its existing session.
- Device check: selected October 2030 from the picker, confirmed, and captured the hierarchy before any subsequent gesture. The heading was `2030年`; September/October occupied the initial grid row. The September card began `26,27,28,29,30,31,1`, matching the real September 2030 week layout rather than September 2026. This verifies content positioning, not only heading text. No latency timing has been claimed.
- Evidence is in ignored local build output: `android/app/build/year-navigation-qa/picker-2030.xml` and `jump-2030-before-gesture.xml`.
- Additional phone operations for 2090 were rejected before execution. Stopped device interaction. Rapid reselection, fling interruption, return-to-current, other remote years and the full prior month/week/day UI matrix remain unverified on-device; keep task 2.3 open. Automated model tests do not replace those UI checks.

## Additional year-mode device acceptance

1. Repeat the 2030 check with 1900, 2090 and 2100, checking actual weekday/date positions before any new gesture.
2. Select a new date during a fling; rapidly reselect a different year and reselect the same date after browsing away. The latest selection must win.
3. Scroll in both directions to the window edge, checking that the visible month and row offset remain stable after recentering.
4. Return to the current year and switch into year mode from a distant day/week/month, checking immediate content positioning.

## Device acceptance steps

1. Install the new APK and enter calendar month mode. Pick June 2030, then June 2090; check the actual day grid and scroll forward/back through adjacent months.
2. Pick February 1900 and February 2000; check 28/29 days and neighboring months.
3. Pick December 31, 2030 and a date in 1990 in day/week modes; swipe both ways, including across year boundaries. Check heading year and weekday/date columns.
4. Switch day → month, week → month, and select a month from year mode; content must match the current cursor immediately.
5. Use return-to-current in each mode after distant navigation; check content and subsequent swipes remain at the current period.
6. Repeat with the date picker while a month list is settling and with pinch transitions; an old scroll position must not overwrite the selected date.
