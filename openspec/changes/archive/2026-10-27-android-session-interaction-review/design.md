## Context
PR #23 adds silent login after a password change and asynchronous review badges. Password changes revoke the current token, so an overlapping authenticated request can return 401 during renewal. Existing ApiClient rejects responses whose request token no longer matches the active session.

## Goals / Non-Goals
Goals: preserve account isolation, renew the password session reliably, and retain established optional-tag behavior. Non-goals: new backend APIs, token refresh protocols, production account mutation or new dependencies.

## Decisions
- Scope renewal to the token being changed. While that exact token is renewing, a 401 still fails the request but does not clear the session or fire the global logout callback. A failed password request removes the guard; a failed post-change renewal clears the revoked session. Successful renewal replaces the token and clears the guard. Capture login identity before changing the password and propagate coroutine cancellation.
- Keep normal 401 invalidation unchanged outside renewal. Reject stale token responses as before. Atomically compare the expected token when saving login identity or invalidating an unauthorized session, so an account switch between response validation and persistence cannot be overwritten. Use injectable preferences/base URL only to run real local HTTP regression tests without Android or production accounts.
- Load review badges directly inside a keyed LaunchedEffect, with cancellation checks and a remembered count keyed to the account. Include auth readiness and review permission in its keys.
- Include kind in the initial/current form snapshot. Remove the new mandatory-tag check because the existing task-tags specification explicitly permits untagged tasks.
- Keep the PR's visual changes and full-detail fetch; verify all three module builds independently.

## Risks / Trade-offs
- Missing stored identity or login/network failure after a successful change requires manual login; clear the revoked token in that case.
- The app does not store passwords. Login identity is private preferences; the new password only exists in the current request/dialog lifetime.
- Cancellation during a successful remote change can require manual login; always release the renewal guard and clear revoked state.

## Migration Plan
No database or dependency migration. Review fixes are committed to PR #23 before it is merged. Backend/Web changes in PR #24 are validated separately.
