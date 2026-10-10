## Why

The user requested review and integration of PRs #4, #5 and backend-xjy into dev. Review found conflicting imports, lost in-session Android state, calendar regressions against the confirmed V1.3 baseline, duplicate daily logs and incorrect HTTP error statuses.

## What Changes

- Integrate all three histories while preserving both Android feature sets.
- Record PR #4's actual scope: log editor/IME layout, current-date mock data, year overview and pinch navigation.
- Retain day/week/month modes and explicit mode selection; date selection stays in the current mode and task taps open details directly.
- Keep personal items, published daily logs and notification preferences in application memory across navigation; supplement today's existing log instead of creating a duplicate.
- Preserve client-error HTTP statuses and headers in the backend uniform JSON error contract.
- Validate Android, backend, Web and related OpenSpec changes before updating remote dev.

## Capabilities

### New Capabilities

- `dev-integration-review`: Compatibility and regression requirements for the requested branch integration.

### Modified Capabilities

None. Existing source changes remain described by android-home-settings-refresh and backend-architecture-foundation; this change records integration repairs and PR #4's previously missing specification.

## Impact

Android Compose state/calendar/logs, backend exception handling and regression tests, OpenSpec records. No business endpoint, persistent storage, credential or infrastructure service is introduced. PRs: https://github.com/YuzhSong/Pandora6ix/pull/4 and https://github.com/YuzhSong/Pandora6ix/pull/5. Backend head: 30edc22.
