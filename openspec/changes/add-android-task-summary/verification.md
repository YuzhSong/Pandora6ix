# Android task summary delivery

## Implemented behavior

- Compact checklist/text entry at the right of the existing calendar mode-selector row; exactly five bottom tabs remain unchanged.
- Task summary contains exactly three panels: scoped tasks, one horizontal row of direct reports, recent journal activity. No KPI, charts, statistics cards or contacts tab.
- Task tabs use existing `mine`, `created`, `managed` semantics. The managed caption explicitly includes subordinate-created OR subordinate-executed tasks.
- Task rows show real title, tags, priority, review/execution/work state, deadline and actual authorized executor avatars. The creator is never substituted for an executor.
- Organization page expands authorized department/team groups using the existing organization-tree helper. Member pages show available identity/department/job information and server-authorized responsible tasks and journals.
- More opens a full scoped journal list. Task/journal taps reuse the existing full detail implementations and existing command authorization.
- Calendar date, mode, tag/scope filters and day/week/month/year scroll state survive secondary navigation. Summary destinations preserve parent scroll/expanded groups and loaded data. Account changes discard feature data and avatar caches.
- Each collection has bounded requests (20/page), independent loading/error/retry, explicit pagination and honest partial-result messages. Member views reuse already-authorized pages instead of issuing a request for every subordinate. Avatar loads reuse cache and coalesce simultaneous requests for the same user.

## Files

| File | Change |
| --- | --- |
| `android/app/src/main/java/com/pandora6ix/app/MainActivity.kt` | Feature navigation, calendar SaveableStateHolder, existing task detail loader, account-isolated avatar repository |
| `android/app/src/main/java/com/pandora6ix/app/ui/calendar/CalendarScreen.kt` | Compact entry, saved month/year anchors and year-grid scroll |
| `android/app/src/main/java/com/pandora6ix/app/ui/summary/TaskSummaryFlow.kt` | Summary, organization, member and full-journal destinations; common task/journal rows and independent states |
| `android/app/src/main/java/com/pandora6ix/app/ui/summary/SummaryData.kt` | Cursor pager, direct-report/responsibility/author filtering, identity fallback and real task state mapping |
| `android/app/src/main/java/com/pandora6ix/app/ui/components/RemoteAvatar.kt` | Existing review avatar extracted for shared use |
| `android/app/src/main/java/com/pandora6ix/app/ui/review/ReviewScreen.kt` | Use shared avatar |
| `android/app/src/main/java/com/pandora6ix/app/data/TaskRepository.kt` | Bounded candidate-page read; retain existing aggregate candidate method |
| `android/app/src/main/java/com/pandora6ix/app/data/UserRepository.kt` | Coalesce avatar requests, propagate cancellation, avoid caching transient failures |
| `android/app/src/main/java/com/pandora6ix/app/data/DashboardModels.kt` | Read optional existing assignment `workStatus`; no backend contract changes |
| `android/app/src/test/java/com/pandora6ix/app/ui/summary/SummaryDataTest.kt` | Ten focused regression tests |
| `openspec/changes/add-android-task-summary/` | CLI-created change metadata, proposal, delta spec, design, task checklist and this delivery report |

## Data sources and permission boundary

| Content | Existing repository / API | Client behavior |
| --- | --- | --- |
| My/created/subordinate tasks | CalendarRepository.tasks → GET `/tasks?scope=mine|created|managed&limit=20&cursor=...` | Preserve scopes and cursor; server applies visibility. No DashboardRepository/statistics request is added. |
| Direct reports / organization | TaskRepository.recipientCandidatePage → GET `/tasks/recipient-candidates` | Requires `/me.capabilities.manage` and business; only `relationship=DIRECT` is a direct report; group only server department/team IDs. |
| Member responsible tasks | Same authorized managed task pages | Filter `authorizedAssignments.recipientUserId`; creator identity alone does not qualify. Continue paging explicitly when matches are not yet loaded. |
| Recent journals / member journals | JournalRepository.journals → GET `/work-logs?scope=managed|mine&limit=20&cursor=...` | Managed for business managers; own for other business users. Member filters by authorUserId. Preview shows first five in existing server order (work date, publication time descending). |
| Task / journal detail | TaskRepository.task / existing JournalDetailScreen → GET `/tasks/{id}`, `/work-logs/{id}` | Load actual stable IDs; existing full details and allowedActions remain authoritative. |
| Identity / avatar | Existing `/me`, authorized RecipientDto, UserRepository.avatar → GET `/users/{id}/avatar` | No admin directory call. Unknown names display explicit member ID suffix; missing image uses the existing initial fallback. |

All requests use existing ApiClient authentication, session expiry and friendly errors. No broader-scope fallback, hardcoded production records, backend/web source changes, database changes, infrastructure or new libraries were introduced.

## Backend limitations / remaining acceptance work

1. The ordinary candidate API does not return phone number or independent job rank. Member detail labels these fields “暂未提供”; jobTitle and department are used when returned. The admin profile has phone data but is deliberately not used from this feature.
2. Candidates supply department/team membership and DIRECT/DESCENDANT, but omit supervisor IDs. Authorized organizational groups expand correctly; recursively expanding one person's reports cannot be implemented reliably with this contract. The page discloses that reporting hierarchy is unavailable. No relationships are inferred from names or departments.
3. Candidates include active business subordinates only; inactive members are not invented. Assignment/journal DTOs contain IDs without display names, so names outside the loaded authorized candidate pages have an explicit ID fallback until more pages load.
4. Member-specific task/journal API filters do not exist. The client filters authorized pages and allows continued paging; an empty partial page is not presented as a complete absence of records.
5. Real business-screen interaction, different widths/font scales and navigation restoration still need authenticated device acceptance. A local Pixel_9a emulator boots and the debug APK installs/launches without a startup exception, but it has no signed-in session. The previously saved Android test credentials were checked against the live login endpoint and returned HTTP 401. No account/task/journal was created or altered, and no credentials are included in this report.

## Initial validation (before PRs 42-45 were integrated)

- `:app:assembleDebug`: succeeded; debug APK at `android/app/build/outputs/apk/debug/app-debug.apk`.
- `:app:testDebugUnitTest`: 75 tests passed, zero failures/errors; includes 10 new SummaryDataTest tests for DIRECT-only filtering, executor identity, member journal authorship, name fallback, real work state, failed page retry, filtered pagination, failed refresh retry, cursor cycle prevention and cancellation.
- `openspec validate add-android-task-summary --strict`: passed.
- `git diff --check`: passed.
- `:app:lintDebug`: executed; blocked by two existing errors in unchanged files: `NotificationHelper.kt:49` (`MissingPermission`) and `AndroidManifest.xml:9` (`PermissionImpliesUnsupportedChromeOsHardware`). Neither feature source nor the shared avatar/repository changes adds a Lint issue. Report also contains 25 existing warnings. The combined Gradle invocation therefore exits unsuccessfully even though APK assembly and unit tests pass. These unrelated notification/camera declarations were left scoped out.
- Existing JPush dependency emits D8 stack-map warnings; APK assembly succeeds.

The change remains local on `codex/android-task-summary`, unarchived and ready for review subject to the backend limitations and authenticated acceptance above.

## Rebase onto integrated dev (2026-10-31)

- PRs #42, #43, #44 and #45 were sequentially rebased/integrated and pushed to `dev`. GitHub confirmed all four merged. The resulting base is `1435a643a7b46122fd5917c9f993a287a0f539c7`.
- PRs #42/#43 retained identical commit patches. PR #45 retained identical non-merge commit patches and its final tree exactly matched the automatic merge of its original head with the new base; its historical merge resolution was reused without changing business code.
- PR #44 required explicitly user-authorized compatibility work for duplicate push callbacks: preserve background high-priority approvals, foreground approval/review prompts and the general notification queue; coordinate their IDs to prevent duplicate prompts. Four focused regression tests were added, and the notification API integration test passed. No unrelated refactor was performed.
- Task-summary changes were backed up and stashed independently of the user's existing untracked deployment files, then restored after rebasing this branch onto the integrated `dev`. Latest notification navigation and the complete existing task detail implementation were preserved verbatim; the summary loader now passes the detail's business-timezone argument.
- Final `:app:assembleDebug :app:testDebugUnitTest` succeeded: **104 tests, zero failures/errors**, including the 10 summary tests and 6 approval-prompt tests.
- Web build and 153 tests passed on the integrated base. Backend package and three focused notification/personnel integration tests passed. Relevant OpenSpec strict checks, OpenAPI check (68 operations), and final whitespace checks passed.
- Lint was not rerun in this integration pass; the earlier Lint failures above describe the initial base and were not suppressed or fixed as part of this Git integration.
- The requested task-summary delivery is a single local commit on top of `dev`; it is not pushed or merged into `dev`. Credentials, `android/local.properties`, the pre-existing push-configuration text file and `deploy/db-console/` are excluded.
