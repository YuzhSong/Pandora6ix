# Validation

- `gradlew.bat :app:compileDebugKotlin`: PASS (existing warnings).
- `openspec validate correct-journal-created-date --strict`: PASS.
- `git diff --check`: PASS.
- Binding review: creation section now uses publishedAt exclusively; workDate is no longer used as a fallback. businessDateTimeLabel uses businessCalendar for both month/day and hour/minute, so company timezone conversion applies consistently across UTC midnight.
- Manual device verification pending: backfill yesterday today and inspect creation time; inspect company-timezone midnight case; missing timestamp should show 未知. No device UI verification performed.
