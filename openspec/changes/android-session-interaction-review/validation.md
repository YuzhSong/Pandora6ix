# PR #23 integration review — 2026-10-21

- Android: JDK 21, `testDebugUnitTest assembleDebug --no-daemon` passed after the final session changes. All 36 tests passed, including six real loopback HTTP regression cases for password renewal.
- Concurrent old-token 401 cannot clear the renewal; ordinary 401 still logs out. Invalid current password retains the session, renewal failure/missing identity clears it, cancellation propagates, and a delayed renewal cannot replace or clear another account.
- Code review: badge loading now stays inside an account/permission/auth-readiness keyed effect and checks cancellation before publishing; full-detail loading also checks cancellation. Task kind participates in dirty detection. Removed the new mandatory-tag restriction to preserve the existing task-tags spec. Tag controls use server capability.
- Strict OpenSpec validation passed. Backend integration candidate: 92 tests passed; Web: 123 tests passed, lint and production build passed. Backend OpenAPI generator verified 56 operations. Existing icon deprecations and Web effect/chunk warnings remain non-blocking.
- No physical-device walkthrough is claimed. APK is built locally; deployment of dev updates the backend/Web and does not install this APK on phones.
