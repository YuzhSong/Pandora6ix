## Why

PR #11 replaces the local Web workspace with the implemented backend from PR #10. The user requests review and merge into dev; this change records integration requirements and non-author verification missing from the original Web-only submission.

## What Changes

- Specify trusted administrator authentication, revocable sessions and server-owned company data, complete pagination and optimistic conflict handling.
- Replace obsolete local-storage cross-tab notification assumptions with the available server version/refresh contract.
- Review the existing implementation and verify unit tests, build, lint, real Spring integration and rendered flows before merging the exact reviewed head.

## Capabilities

### New Capabilities
None.

### Modified Capabilities
- `web-admin-shell`: Trust backend administrator identity, retrieve shared enterprise data and protect external edits through versions rather than local-storage mutation notices.

## Impact

PR #11 changes web only. This integration change adds OpenSpec review records; no backend/Android behavior or new API is introduced. Existing source-map-js development-dependency advisory is unchanged from dev and recorded for a separate dependency update. Backend approval/inbox and Android integration remain outside this PR.
