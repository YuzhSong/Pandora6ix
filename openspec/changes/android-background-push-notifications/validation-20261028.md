# JPush service repair validation (2026-10-28)

## Failure and repair

The configured debug APK logged `AndroidManifest.xml missing required service:cn.jpush.android.service.JCommonService` and neither selected account had an active cloud push installation. Initialization and a configured AppKey alone did not establish the channel.

Added `PandoraPushService : JCommonService` and its enabled, non-exported `:pushcore` service declaration with the provider discovery action. This follows [JPush Android quick-start integration](https://docs.jiguang.cn/jpush/quickstart/Android_quick). No backend, account, permission, approval, or credential contract changed.

## Checks passed

- `android/gradlew.bat -p android testDebugUnitTest assembleDebug compileReleaseKotlin --no-daemon`: success; 36 unit tests, zero failures/errors. Existing icon-deprecation warnings remain.
- `openspec validate android-background-push-notifications --strict` and `git diff --check`: passed.
- Packaged APK inspection: the service is enabled, non-exported, uses `:pushcore`, and has `cn.jiguang.user.service.action`. Compiled class inspection confirms it extends the SDK's `JCommonService`.
- The APK AppKey matches the private local Gradle property; its value was not printed or committed.
- In-place update of both independent API 35 emulators (`Pixel_9a`, `Pixel_9a_Push_Test`) succeeded without clearing data. Both apps and their pushcore processes run; both have Android notification permission.
- Both SDK profiles report registration result code `0` and successful server-login timestamps after the repair.
- Cloud read-only check: `chenlin` has one active push installation. `luchuan` has none yet; the new emulator is at the login screen and has no Pandora session. Login is required before account-bound registration.

## Acceptance still pending

No production task, approval, notification, or provider send was created by this verification. The user can sign in as `luchuan` on the second emulator, keep `chenlin` on the first, submit an approval, and check foreground/background receipt and notification taps. End-to-end receipt, permission-denied behavior, switching/logout, cold-start taps, and physical-device background behavior are not marked complete by this repair. Existing unchecked tasks 2.2–2.4 remain open.
