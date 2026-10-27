# Proposal

## Why
Administrators need to handle departures and promotions while keeping accounts and employment status distinct, preserving identity and history, without accessing company business workflows.

## What Changes
- Add separate employment state and departure date, immediate departure handling with account/session/credential revocation, and guards against accidental reactivation.
- Add atomic job adjustments for title, roles, organization and reporting, preserving identity/password/profile and validating hierarchy.
- Keep personnel change records with actor/time/before/after; expose only personnel data. No task, journal, approval or business handover.
- Add active/departed filters and exclude departed people from assignment selectors. Local implementation only, no deployment.

## Capabilities
### New Capabilities
- `personnel-lifecycle`: Company-scoped departure, job adjustment and personnel history.
### Modified Capabilities
None.

## Impact
Backend migration/contracts/directory endpoints and Web repository/personnel dialogs, API documentation and regression tests. General system-log storage remains a separate stakeholder decision.
