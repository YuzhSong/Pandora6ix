## Validation, 2026-10-18

- Java 21 / existing Android SDK 35: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug --console=plain` passed (43 tasks, 7m48s, fresh worktree).
- All 24 tests in 6 suites passed without failures, errors or skips. Six new hierarchy cases cover department direct members and multiple teams, same-name departments, same-name teams, legacy responses without department IDs, unassigned people and duplicate recipients/counts.
- Compose UI compiled successfully, including recursive expandable groups and loading/retry paths. A debug APK was produced at `android/app/build/outputs/apk/debug/app-debug.apk` (17,950,157 bytes).
- `openspec validate fix-recipient-organization-hierarchy --strict` and `git diff --check` passed.
- `MainActivity.kt` and `JournalRepository.kt` have no difference from current origin/dev. The business-date / allowedActions journal policy is retained; the PR title's original 24-hour description is superseded.
- Backend/API authorization and pagination are unchanged. Candidate metadata already supplies department and organization IDs; no backend release, schema change or cloud data change was required.
- No device/emulator interaction test was run. Native/browser automation could not initialize in this session; UI evidence is compilation plus pure hierarchy tests, not a claimed visual/device acceptance.
- Keep PR #15 and PR #16 open for review; do not merge or archive these changes in this task.
