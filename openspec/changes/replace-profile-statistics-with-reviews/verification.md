# Verification — 2026-10-31

- Branch: Android-syz. This request changes only MainActivity plus this OpenSpec record; preceding summary/contact edits remain in the working tree.
- Android `:app:assembleDebug :app:testDebugUnitTest`: passed, 104 tests, zero failures/errors. Log: `C:/Users/34755/AppData/Local/Temp/pandora-profile-reviews-build.log`.
- Strict OpenSpec validation and `git diff --check`: passed.
- Source check: one approval-center SettingsRow remains; no statistics state, profile statistics parameter, task-statistics panel or `calendarRepo.statistics()` call remains in MainActivity. Repository/backend statistics interfaces were not removed.
- Latest APK installed on emulator-5554. Existing logged-in team-lead account shows the personal card, approval-center panel (real badge 4), organization panel and settings in that order. No task-statistics panel or duplicated approval entry is visible.
- Tapping approval center opens the existing queues; tapping its toolbar back returns to the profile with the relocated entry. Existing showReviewCenter capability check, pendingReviewCount and onOpenReviews callback are reused verbatim.
- Existing system-back behavior at the approval-list root exits to the launcher; ReviewScreen already lacks a root BackHandler. This layout-only change does not alter that behavior. No crash was observed in AndroidRuntime logs. Accounts without approval capability and large-font layouts were source-reviewed, not separately exercised.

Screenshots outside version control:

- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/profile-reviews-after.png`
- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/profile-reviews-return-after.png`
