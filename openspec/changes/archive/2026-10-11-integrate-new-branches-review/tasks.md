## 1. Preserve personal-item source identity

- [x] 1.1 Replace formal strings with source-linked items and retain session drafts across page navigation.
- [x] 1.2 Filter candidates by source id and current date; preserve edited text on removal and reselection.
- [x] 1.3 Add regression tests for edits, equal-content logs, date boundaries, duplicate selection and the ten-item cap.
- [x] 1.4 Correct year-grid return-to-today positioning and verify it after an adjacent-year scroll.

## 2. Validate and integrate

- [x] 2.1 Run independent module checks and strict validation of the six integration changes.
- [x] 2.2 Exercise Web desktop/mobile management flows and available Android emulator flows, recording any remaining device checks.
- [x] 2.3 Reconcile calendar requirements, verify remote heads, merge and push dev.

Validation on 2026-10-11: Web npm test (30 passed), npm run build and npm run lint (two existing effect warnings); Android assembleDebug, testDebugUnitTest (14 passed) and lintDebug; backend mvnw.cmd verify (9 passed); all six changes passed strict OpenSpec validation. Bundled Playwright Chromium at 1440x1000 and 390x844 validated fresh setup, organization/person/role/tag/company-item writes, dirty discard, preferences and reload persistence with no page or console errors. Browser plugin not available. Pixel_9a emulator verified calendar swipes, adjacent two-pointer pinch modes, adjacent-year return to today, ordered tags and AND filtering, four calendar-mode color screenshots, password guards, five themes, item edit/remove/reselect/reorder and published-log candidate linkage. No GitHub check runs were configured on either reviewed PR head. Far-range year reanchoring and physical-device acceptance were not exhaustively exercised.
