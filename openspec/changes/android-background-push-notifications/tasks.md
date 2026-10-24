# Tasks

## 1. Backend persistence and device APIs

- [x] 1.1 Add an additive migration for review event types, account-bound push installations, and delivery outbox rows; verify Flyway migration tests.
- [x] 1.2 Add authenticated register/deactivate endpoints and deactivate installations during logout; verify cross-account and cross-enterprise isolation tests.
- [ ] 1.3 Add JPush REST delivery with environment-only credentials, bounded retry/backoff, and disabled-provider behavior; add payload/auth/retry unit tests.
- [x] 1.4 Create reviewer request and submitter decision notifications transactionally; verify approval, rejection, rollback, and deduplication integration tests.
- [x] 1.5 Update generated OpenAPI and run backend tests plus OpenAPI `--check`.

## 2. Android integration

- [x] 2.1 Add JPush SDK and non-secret AppKey build configuration; verify debug and release Kotlin compilation without a configured key.
- [ ] 2.2 Register/deactivate the current installation based on account, preference, permission, token refresh, and logout; add Android repository or focused instrumentation checks.
- [ ] 2.3 Handle notification taps by opening the app and loading the persisted in-app notification list; verify cold-start and warm-start intent behavior.
- [ ] 2.4 Keep local reminders functional and request Android notification permission only after the user enables notifications; verify permission-denied behavior.

## 3. Validation and deployment

- [x] 3.1 Run strict OpenSpec validation, backend tests, Android unit tests, and debug assembly; record any environment-dependent checks.
- [x] 3.2 Document JPush console setup and server environment variables without committing AppKey secrets or Master Secret; verify secret scanning and configuration examples.
