# Release and behavior notes

- Deploy backend with V12 before Android. Android calls POST /tasks/recurring for repeating schedules, so an older backend cannot silently interpret repetition as one continuous task.
- Existing task records keep their original continuous interval/all-day meaning; no historical task is automatically expanded.
- Recurrence dates are inclusive. Weekly intervals anchor to Monday of the first week. Monthly dates missing the anchor day are skipped.
- Interval is 1–365 days/weeks/months; a request covers at most ten years and creates at most 366 occurrences. Repeated timed occurrences must finish later on the same day. Each all-day occurrence covers one day.
- Each generated task has its own assignment, approval and completion state; reminders/calendar/statistics use those ordinary tasks. Editing is per occurrence. No group editing or group approval is introduced.
- Series ID and readable original rule label persist on each occurrence and in review/audit snapshots.
- Android draft includes recurrence settings, and dirty-exit protection includes rule changes.
