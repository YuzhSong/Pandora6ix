# Validation

- Android :app:compileDebugUnitTestKotlin and :app:assembleDebug: PASS (existing warnings).
- Compiled tests via JUnitCore: WeekAllDayBarsTest, WeekTaskStackTest, CalendarLayoutTest, DayTaskColumnsTest, RecurrenceRuleTest: OK (33 tests).
- New tests cover continuous interior/full-day ranges, partial endpoint cards without duplicates, midnight start/end and empty final day, 00:00–23:59 display threshold, shorter intervals, week clipping and overlapping explicit all-day bars.
- Static form review: custom interval/unit controls are guarded by customRepeat; preset chips force customRepeat=false and repeatRule interval=1. Weekly weekday controls remain available. Draft and dirty snapshot persist the selection.
- Strict OpenSpec validation and git diff --check: PASS.
- No device visual verification or installation performed.
