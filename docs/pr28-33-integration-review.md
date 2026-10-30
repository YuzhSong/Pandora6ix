# PR 28, 30, 31, 32 and 33 integration review

## Confirmed decisions

- Backfilled journals can be edited/deleted on their submission date in the enterprise timezone; the three-date creation window remains.
- Newly created Web accounts use generated 16-character random passwords. Existing account passwords are unchanged.

## Review repairs

Preserved journal/calendar/task routes while resolving Android conflicts. Mixed self/subordinate delegation now uses the same scoped policy during creation, pending edit and approval. Historical names come from stored snapshots, including actor search; valid version zero differs from unknown null. Bulk integration fixtures use the new CSV format and generated account passwords. Push delivery now fences retry leases, checks current ownership and active users, preserves queued messages on same-owner registration, and reconnects Android SDK registration and notification taps.

## Verification

- Backend Maven verify: 113 tests, no failures or errors. Two affected integration methods rerun after additional pending-edit and historical-actor assertions.
- Android: testDebugUnitTest, assembleDebug, compileReleaseKotlin passed.
- Web: 135 tests, lint and production build passed; existing React effect warnings and Excel chunk-size warning remain.
- Real local Spring/H2/Vite repository integration passed.
- Existing Playwright/Chromium fallback (Browser plugin unavailable): desktop and mobile, accountIdentifier login, default organization route, randomly generated password usable for login, no manual-password field, audit version-zero history navigation, no browser errors. Screenshots inspected outside Git.
- OpenAPI: 63 operations, generated contract check passed. All 11 affected OpenSpec changes validated strictly.
- Private exclusion scan confirmed old credential values and the original credential-bearing commit are absent from the candidate history.

## Remaining external acceptance

No ADB device is connected. Physical keyboard/calendar/navigation, permission denial, background/cold-start notification taps and actual JPush delivery require a device. No live provider send was attempted. Rotate the exposed old Master Secret in JPush, then configure the new protected server credentials and matching Android AppKey before end-to-end push acceptance. Missing credentials leave remote push disabled and preserve in-app notifications.

Deployment verification compares personnel and organization snapshots and checks the final release SHA and health. No production test tasks or accounts are created by these checks.
