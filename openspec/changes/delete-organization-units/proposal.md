# Proposal

## Why
Administrators cannot remove unused departments or teams because neither the API nor Web exposes deletion. The user confirmed that this change may add the backend endpoint as well as the Web action.

## What Changes
- Add versioned, enterprise-scoped deletion of empty non-root organization units.
- Add a Web confirmation dialog, clear blocking reasons, and immediate directory updates after success.
- Export the endpoint in OpenAPI and verify authorization, dependencies, stale versions and concurrent directory writes.

## Capabilities
### New Capabilities
None.
### Modified Capabilities
- `backend-identity-directory`: safe deletion of empty organization units.
- `organization-directory`: confirmed department/team deletion in Web.

## Impact
Backend directory controller/service/contracts and integration tests; Web repository, organization page and tests; generated OpenAPI and handoff documentation. No schema migration, cascading deletion or change to business records.
