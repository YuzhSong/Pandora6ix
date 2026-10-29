# PR #40 integration validation (2026-10-30)

Rebased Android-lj onto dev after PR #39. Resolved the journal editor conflict by retaining navigationBarsPadding/imePadding and the shared weighted scrolling column, while removing the outdated explanatory text requested by #40. Preserved PandoraPushService.

Checks passed: 65 Android unit tests, debug APK assembly, release Kotlin compilation; 128 backend tests; generated OpenAPI (64 operations) and optional resolved response field; strict OpenSpec validation; diff check and private AppKey exclusion. Existing unrelated deprecation warnings remain.

Review-request bodies preserve submitter names in the persisted notification, while remote push stays generic. Resolved is derived from an enterprise-scoped review; missing/non-pending reviews hide the Android pending-review action. Envelope unread counts are refreshed when opening/closing the drawer and opening a push, not claimed to stream live while idle. No new cloud test data or actual provider sends were created. Emulator/physical-device layout and notification receipt remain manual acceptance.
