# Validation

## Evidence

Connected device crash buffer contained four main-thread IllegalArgumentException crashes on 2026-10-30 (13:06:07, 13:06:15, 13:14:36, 13:23:52). Each points to JournalEntryField.kt:62 / TextLayoutResult.getCursorRect. Offsets 10, 4, 2 exceeded layout maxima 9, 3, 1 respectively. No journal contents were collected.

## Checks

- Android :app:assembleDebug: PASS.
- Focused regression tests compiled successfully. Gradle test worker failed to start with ClassNotFoundException for GradleWorkerMain; this did not execute the tests. Ran the compiled JournalCursorTest via JUnitCore with JUnit 4.13.2 and project runtime classes instead: OK (3 tests).
- Tests cover observed length mismatches, deletion, same-length replacement, updated matching layout, multiline/emoji/empty text and selection boundaries.
- openspec validate fix-journal-cursor-crash --strict: PASS.
- git diff --check: PASS (line-ending warning only).
- adb install -r app-debug.apk: Success; updated connected phone without clearing app data.
- Device continuous typing verification pending user retry. No claim of completed end-to-end reproduction after fix.

## Review

Frame suspension now precedes reading measured layout and current TextFieldValue. Matching and bounded offset calculation immediately precede getCursorRect without a suspension point, eliminating the captured old-layout/current-selection mismatch seen in device traces.
