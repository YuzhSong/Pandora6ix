# Tasks

## 1. Backend persistence and device APIs

- [x] 1.1 Add an additive migration for review event types, account-bound push installations, and delivery outbox rows; verify Flyway migration tests.
- [x] 1.2 Add authenticated register/deactivate endpoints and deactivate installations during logout; verify cross-account and cross-enterprise isolation tests.
- [x] 1.3 Add JPush REST delivery with environment-only credentials, bounded retry/backoff, and disabled-provider behavior; add payload/auth/retry unit tests.
- [x] 1.4 Create reviewer request and submitter decision notifications transactionally; verify approval, rejection, rollback, and deduplication integration tests.
- [x] 1.5 Update generated OpenAPI and run backend tests plus OpenAPI `--check`.

## 2. Android integration

- [x] 2.1 Add JPush SDK and non-secret AppKey build configuration; verify debug and release Kotlin compilation without a configured key.
- [ ] 2.2 Register/deactivate the current installation based on account, preference, permission, token refresh, and logout; add Android repository or focused instrumentation checks.
- [ ] 2.3 Handle notification taps by opening the app and loading the persisted in-app notification list; verify cold-start and warm-start intent behavior.
- [ ] 2.4 Keep local reminders functional and request Android notification permission while the retained notification preference is enabled; verify permission-denied behavior.

## 3. Validation and deployment

- [x] 3.1 Run strict OpenSpec validation, backend tests, Android unit tests, and debug assembly; record any environment-dependent checks.
- [x] 3.2 Document JPush console setup and server environment variables without committing AppKey secrets or Master Secret; verify secret scanning and configuration examples.

## 4. SDK service repair (2026-10-28)

- [x] 4.1 Add and declare the required application-owned JCommonService subclass without changing approval or permission behavior.
- [x] 4.2 Run Android unit tests, debug assembly, release Kotlin compilation, strict OpenSpec validation, and inspect the packaged service declaration and AppKey without exposing credentials.
- [x] 4.3 Update both running emulators without clearing application data; verify provider registration and report cloud device binding separately from actual notification delivery.


## 5. PR #40 notification presentation integration

- [x] 5.1 Rebase on PR #39 without reverting journal scroll or keyboard/navigation inset fixes.
- [x] 5.2 Describe optional resolved notification response in generated OpenAPI and the push notification specification.
- [x] 5.3 Verify Android unit/debug/release compilation and backend tests before merging PR #40.
