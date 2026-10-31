# Verification — 2026-10-31

## Implemented behavior

- Overview contains three white panels. Task tabs filter only the first panel; task and journal previews contain at most three entries, and the direct-report row contains at most four people.
- Member details use separate white panels for personal information, responsible tasks and recent journals. Member task/journal previews also contain at most three entries.
- Task and journal entries share a soft theme background, individual border and spacing. Full task, journal and organization destinations also use white content panels.
- Full task lists retain the selected scope; member full lists retain the executor/author filter. Returning restores the parent destination without overwriting the overview task filter.
- Existing authenticated CalendarRepository.tasks, TaskRepository.recipientCandidatePage and JournalRepository.journals remain the task/people/journal sources. Existing capability checks, backend authorization and task/journal detail callbacks remain in use. No bottom navigation or mock-data changes.
- Compact task entries retain all fields using explicit line heights, reduced padding/chips/avatar sizes, and lighter entry borders. Removed the managed-task explanatory caption and organization implementation note.
- Member telephone reads use UserRepository.memberContact and the dedicated `GET /users/{id}/contact`, returning only id and phoneNumber from app_user. Existing active management scope and enterprise checks govern access; candidate lists still exclude personnel secrets. The UI distinguishes loading, a missing phone and read failure, with retry. Contact numbers are not persisted by the client, and the server applies its existing no-store response policy.
- All current work is on the explicitly requested `Android-syz` branch. No additional branch was created for this follow-up.

## Automated checks

- `android/gradlew.bat -p android :app:assembleDebug :app:testDebugUnitTest --console=plain`: passed after the final source change; 104 tests, zero failures or errors.
- `openspec validate refine-task-summary-previews --strict`: passed.
- `git diff --check`: passed.
- Build log: `C:/Users/34755/AppData/Local/Temp/pandora-summary-panels-final-build.log`.

Follow-up checks:

- Android assembleDebug and testDebugUnitTest passed again: 104 tests, zero failures/errors. Log: `C:/Users/34755/AppData/Local/Temp/pandora-syz-compact-android.log`.
- Backend `BusinessApiIntegrationTests#memberContact*+recipientCandidatesExposeStableDepartmentMetadataWithoutPersonnelSecrets`: four tests passed, zero failures/errors. Verified actual/null/updated phones, limited response fields, no-store, authorized founder/manager/lead reads, employee/admin denial, unknown/self/superior/out-of-scope/inactive/cross-enterprise denial, and scope rechecks after a move. The existing candidate privacy regression remains passing. Log: `C:/Users/34755/AppData/Local/Temp/pandora-syz-contact-backend.log`.
- Initial header assertions exposed that the shared BusinessResponses advice enforces `Cache-Control: no-store`; the new route and contract reuse this existing policy. The shared advice was not changed.
- OpenAPI generator and `--check` both passed: 69 operations, including the new contact route.
- Backend `mvnw.cmd -DskipTests package`: passed. Log: `C:/Users/34755/AppData/Local/Temp/pandora-syz-contact-package.log`.

## Emulator checks

Latest debug APK installed and launched on emulator-5554 with the existing authenticated session. Default screen: 1080 × 2424, density 420, font scale 1.0.

- Confirmed scope switching stays inside the overview and preserves all three task tabs.
- Confirmed the subordinate-task preview contains three entries, while “查看全部任务” opens a task-only list containing additional real tasks. Back returns to the overview.
- Inspected actual member detail screenshots: personal information and responsible tasks have separate white surfaces, visible borders and spacing; individual task rows have their own boundaries.
- Source-reviewed shared journal-row styling, preview limits, member executor/author filtering, permission conditions and return state.
- Follow-up APK installed and launched. Inspected compact summary and member screenshots; confirmed four real direct reports are fully visible in one row and the redundant managed-task caption is absent. Verified the current cloud backend's unavailable-contact UI and retry, without blocking member tasks.

Local screenshots (outside version control):

- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/member-after.png`
- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/member-tasks-logs-after.png` (shows personal-information/task panel boundaries and separate task rows)
- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/summary-after.png`
- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/full-subordinate-tasks.png`
- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/compact-summary-after.png`
- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/compact-member-contact-after.png`
- `C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/compact-member-tasks-after.png`

Limits: concurrent navigation on the shared emulator interrupted scripted traversal. Complete member-filtered task/journal navigation and large-font/narrow-screen checks were not verified end to end. No emulator settings were changed. This change does not claim a passing lint run.

Deployment limit: the contact endpoint is implemented and verified locally; the cloud backend has not been deployed by this task. The cloud-connected emulator therefore shows “暂不可用” with retry. Successful actual-phone API reads are verified against the isolated backend integration database, not the cloud service. Legacy records without a phone render “未填写” after a successful read.
