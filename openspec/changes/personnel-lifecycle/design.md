# Design
## Context
Account status already supports ACTIVE/DISABLED, directory writes lock the enterprise and use optimistic versions, and temporary password handoff has explicit revocation. Employment state is absent.
## Goals / Non-Goals
Maintain personnel lifecycle without business data. This iteration handles departures immediately with an actual date no later than the company-local current date; future scheduling and rehire are excluded. Preserve account IDs, passwords and all historical references.
## Decisions
- Follow-up: replace managers in one enterprise-locked transaction. Eligible successors are ordinary employees without direct reports, or existing same-role managers in the same unit; managers of other units require separate reassignment first. Preserve the successor's ADMIN permission, grant the relevant organization role, retain the former manager's ADMIN permission and make them an EMPLOYEE reporting to the successor. Reassign only in-service direct reports, increment all affected versions, revoke sessions and record before/after personnel fields. A shared directory-write policy prevents ordinary PATCH/job-change/departure from bypassing vacancy protection: company founders need a successor while departments exist, department managers while child units/personnel exist, and team leads while other in-service team members exist. Empty teams can be vacated. No task/journal data is changed.
- Flyway V6 adds IN_SERVICE/DEPARTED and departure_date with consistency constraints. Departure uses the existing versioned user patch for last-admin protection, then sets employment state and clears supervisor. Block self-departure and departures with in-service direct reports until relationships are adjusted.
- Dedicated company-admin job-adjustment endpoint changes title/organization/supervisor/roles atomically, uses existing user validations plus manager-rank validation, revokes old sessions on authorization changes and records changes.
- Personnel change records store a minimal before/after JSON snapshot of employment and job fields plus actor/time/date in the same transaction. These records are personnel history, not the pending general task/journal system-log design. No sensitive identity or credentials enter history.
- Departed people remain queryable but cannot be reactivated by ordinary profile/role/reset endpoints; disabled accounts remain distinct from departures. Web defaults to in-service, offers departed/all filters and excludes departed selections.
## Risks / Trade-offs
Future effective-date scheduling needs a separate lifecycle scheduler. Last active administrator remains protected; self operations use the account flow. Migration must precede Web rollout; no cloud rollout in this task.
## Migration Plan
Deploy V6 after existing V5. Existing people default to in-service including temporarily disabled accounts. Rollback must keep departed accounts disabled and not drop personnel history.
