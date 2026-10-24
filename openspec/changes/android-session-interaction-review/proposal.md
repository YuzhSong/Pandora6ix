## Why
PR #23 improves Android password changes, review visibility and task entry, but has no change record. Review must preserve session isolation and avoid losing task type edits before merging the requested PRs into dev.

## What Changes
- Keep successful password changes signed in through a fresh authenticated session; fall back to login if renewal fails.
- Prevent revoked old-token requests and previous-account review responses from interrupting or contaminating the current session.
- Show pending review badges, refresh full task details, honor tag management permissions, and include task kind in exit-change detection.
- Retain existing optional task-tag behavior unless the product contract requires a tag.
- Preserve the PR's theme, priority and form interaction polish.

## Capabilities
### New Capabilities
- `android-session-interaction`: Android session renewal and account-scoped interaction guarantees.
### Modified Capabilities
None.

## Impact
Android only. Existing identity/task/tag APIs, no backend schema or dependency additions. Unit tests exercise local HTTP session renewal and rejection of delayed responses.
