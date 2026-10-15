## Why

The user requests backend readiness for the latest Android task, journal, dashboard and account flows, and explicitly authorizes direct-supervisor approval to complete the real task lifecycle. Existing endpoints cover most modules, but approval remains Reserved and tests currently seed approved states directly.

## What Changes

- Implement authorized review lists, immutable submitted snapshots and versioned APPROVE/REJECT decisions; activate every recipient atomically only on approval.
- Preserve pending submission and separate progress/personal completion/delegated acceptance, including per-assignment action metadata for Android.
- Expose role-based task creation/review capabilities in the current-user profile.
- Verify all ten requested modules through actual HTTP with persisted data, update Android integration guidance and export the implemented OpenAPI contract.
- Use Android sources already integrated into dev (including recipient search, tag AND filtering, calendar and entry-based selection) as current client evidence. Do not modify Android or Web.

## Capabilities

### New Capabilities
- `backend-task-review`: Direct-supervisor task approval, submitted snapshots, audit history, safe pagination and atomic task activation.

### Modified Capabilities
- `backend-task-workflow`: Allow the confirmed approval backend while retaining unconfirmed policies and Reserved inbox; expose assignment-specific actions.
- `backend-identity-directory`: Publish role-based personal/delegated creation and review capabilities to replace Android demo permissions.

## Impact

Changes are limited to backend sources, tests, migrations, interface/integration documentation and this OpenSpec change on backend-syz. Add three `/task-reviews` operations and an additive task-review audit migration; no new services or dependencies. No automatic approval, inbox implementation, founder self-approval, approved-task editing, rejected-task resubmission, cross-day timed policy, Android networking work, PR or deployment.
