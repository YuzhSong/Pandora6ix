## Why
Administrators need to compare staff quickly, understand disabled accounts and complete manager succession without guessing the required sequence. Avatar selection currently cannot persist despite an existing backend endpoint.

## What Changes
- Retain cards and add a compact personnel table with full organization paths, supervisors, roles and separate employment/account states.
- Reset all filters and show matching/total counts; label direct and inclusive organization counts.
- Explain known departure-related disablement without inventing missing reasons, and confirm restoration of in-service disabled accounts through the existing versioned endpoint.
- Guide manager departure/job changes through atomic succession before continuing the intended action.
- Save compressed avatars to the existing authenticated endpoint and reload stored images after refresh.

## Capabilities
### New Capabilities
None.
### Modified Capabilities
- `organization-directory`: Comparable directory views, complete filter resets and explicit count scopes.
- `personnel-lifecycle`: Status recovery and succession guidance.
- `web-account-preferences`: Persisted administrator avatar.
- `web-operation-history`: Persistent table-header filters and separate module/source columns.
- `credential-handoff`: Selected available credential delivery and direct delivery of the current creation batch.

## Impact
Web UI, repository/client and tests only. Reuse existing user PATCH, manager replacement and avatar GET/PUT contracts. No new backend tables or endpoints. Do not change production personnel or credentials during validation.
