## Why

The user requests review and integration of PR #8 and PR #9 into dev and an evidence-based audit of backend coverage against requirements V1.3 and 7nfc's Web integration gaps. PR #8 implements the confirmed I-TASK-02 recipient interaction and needs integration specifications and verification.

## What Changes

- Integrate the Android @ recipient query and multi-selection interaction while preserving ordered task tags, validation and dirty-exit protection.
- Verify that the candidate layer is separately scrollable and usable with the keyboard; repair integration defects if found.
- Integrate the Web development proxy and explicit raw-JSON health adapter without relaxing business response envelopes.
- Record implemented, missing Planned, Reserved and unresolved backend capabilities with source evidence and runtime probes.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calendar-view`: Add the confirmed @ recipient candidate interaction used by the existing task-entry sheet.
- `dev-integration-review`: Require backend coverage reporting to distinguish actual runtime implementation from design and mock state.

## Impact

Android task-entry UI, Web development tooling, OpenSpec integration records and a backend audit under docs/reviews. The backend is inspected and tested; this change adds no business API, database, credential, infrastructure service or new product requirement. The user's instruction authorizes review, repairs necessary for integration and merge into dev.
