## 1. Calendar navigation

- [x] 1.1 Reanchor month content on explicit date navigation and recenter on mode entry; suppress stale scroll feedback.
- [x] 1.2 Keep adjacent day/week arithmetic relative to the cursor and show the year in period headings.
- [x] 1.3 Replace year anchor/jump effects with atomic window initialization and fresh scroll state per navigation; preserve edge recenter position.
- [x] 1.4 Defer hidden month index generation and cache visible year-card dates.
- [x] 1.5 Reduce date allocations, cache task lanes and flatten year day-cell rendering without losing interactions.
- [x] 1.6 Deduplicate period requests and propagate cancellation without stale failure state.

## 2. Validation

- [x] 2.1 Add regression tests for distant month indexing, leap years, adjacent periods and year boundaries.
- [x] 2.2 Run Android unit tests, assembleDebug, lintDebug and strict OpenSpec validation; record device verification steps and limitations.
- [ ] 2.3 Verify date selection, gestures, mode changes and return-to-current on a device/emulator.
- [x] 2.4 Add year-window jump/recenter regressions and rerun unit tests, assembleDebug, lintDebug and strict validation.
- [ ] 2.5 Add date/period regressions; run unit tests, build, lint and OpenSpec validation and record available device evidence.
