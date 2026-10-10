# Dev integration review — 2026-10-11

## Reviewed inputs

- PR #4, Android-swj: `6730320912ccd8f1f721e2f57fcd051289d8f23c`.
- PR #5, Android-lj: `e1610ccc44b4f2f7b2abcaba324c74ed71e174bd`.
- backend-xjy: `30edc22e0745c4233c5d2e9d8b7a5722adc357bc`.
- Target dev before integration: `e7dc7029dc54d336369180cfa45c8d4b1f22f56c`.
- Reviewed by Codex on behalf of the requesting repository maintainer, independently of the two PR authors. No provider approval review was submitted.

## Findings resolved

1. One MainActivity import conflict; retain BorderStroke and the long-press drag gesture import alongside PR #4's log editor changes.
2. PR #4 description omitted calendar/year/current-date/tooling changes and did not reference OpenSpec. Record actual scope in this integration change.
3. Calendar dropped day mode and retained automatic date drill-down, conflicting with confirmed V1.3 FR-VIEW-05. Restore explicit modes and direct task details; preserve the contributed year/pinch behavior. Overflow opens a list within month mode.
4. Personal-item edits, notifications and published logs disappeared when page compositions were disposed. Hoist session data and preference state to PandoraApp; update the profile notification label from shared state.
5. Supplementing a seeded current-day report could add a duplicate because the mutable collection and mock history were combined. Use one shared collection with an upsert by user/date; regression tests cover seeded and newly created dates.
6. Framework HTTP errors were caught as generic exceptions and returned 500. A failing POST /health regression reproduced 500 instead of 405. Extend ResponseEntityExceptionHandler to preserve status/Allow headers and retain the uniform error body.
7. Large-font year dates were visibly cut off by inherited Material text line heights. Give compact dates explicit line heights and sufficient cell height; check the corrected rendering.
8. Ignore generated Android Kotlin compiler state under android/.kotlin/.

## Checks

- Android: `gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console=plain`; 9 unit tests, no failures. Lint has no errors, two warnings (missing application icon and existing direct test dependency declaration) and two informational state-boxing suggestions. Icon deprecation warnings remain non-blocking.
- Backend: `mvnw.cmd -B -ntp verify`; 9 tests, no failures, executable jar built. Covers JSON 404/request ID, 405/Allow, validation 400, malformed JSON 400, safe 500, enum values and database-free local-profile context startup. Test-only probe endpoints are confined to test source.
- Web: `npm ci --no-audit --no-fund`, `npm run build`; production build passed.
- Strict validation: android-home-settings-refresh, backend-architecture-foundation and integrate-dev-review.
- `git diff --check`: no whitespace errors.

## Emulator checks

Pixel_9a, 1080 × 2424, with the integrated debug APK:

- Disable notifications, leave settings, revisit: switch remains off and profile displays 已关闭.
- Choose large font: settings page and bottom navigation remain operable; inspect screenshot.
- Long-press first personal item and drag to third: order changes; add ReviewItem; navigate away/back and reopen editor: order and item remain.
- Open home expanded overlay: inspect numbered badges, separators, typography and scrolling.
- Publish ReviewDailyLog with IME visible: editor, save and publish controls remain above keyboard; inspect screenshot. Navigate away/back, supplement content: exactly one updated daily report remains.
- Select a month date: month mode remains. Tap a month task: task detail opens directly. Day and year modes remain selectable.
- Inspect corrected large-font year calendar.

Actual two-finger pinch injection and the complete device/landscape/font matrix were not exercised; adjacent-mode/boundary rules have unit coverage. This is an in-memory prototype: process exit resets mock data.

## Delivery

Remote heads are rechecked before the normal fast-forward push to dev. GitHub provider diagnostics could not connect in this session; no provider CI success is claimed. The repository currently has no checked-in workflow directory. Merge histories preserve all three reviewed source heads.
