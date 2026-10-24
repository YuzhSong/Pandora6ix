## Why

The 2026-10-13 audit and web integration checklist found no implemented business APIs. The user requests backend-only implementation on backend-syz using the confirmed V1.3 requirements, API0.1 and DB0.2, after synchronizing dev, without a PR.

## What Changes

- Implement persisted, enterprise-scoped identity, revocable authentication, enterprise initialization, organization and account administration.
- Implement shared tags, authorized task creation/read/calendar, per-recipient progress and completion, published journals with stable entries, company items, personal important selections and dashboard/basic statistics.
- Add version checks, transactions, optional idempotency, pagination, typed contracts, integration tests and backend developer documentation.
- Keep task approval/inbox Reserved and Q-01–09 unresolved rules closed. Pending tasks never become executable automatically. No drafts, deletion, AI, notifications or frontend changes.

## Capabilities

### New Capabilities
- `backend-identity-directory`: Enterprise initialization, authentication, profiles, administration and scope authorization.
- `backend-task-workflow`: Tags, pending task applications, calendar and separate personal/delegated execution.
- `backend-journals-dashboard`: Unique journals, stable entries, important items, selections and basic read models.

### Modified Capabilities
None. Existing health and legacy error fields remain compatible.

## Impact

backend sources, dependencies, database migrations, tests, API documentation and this OpenSpec change. JDBC/Flyway and embedded durable H2 enable independent local builds without a new infrastructure service. Deployment database configuration is external; passwords/bootstrap credentials are never checked in. Development stays on backend-syz; no PR, dev merge or archive is part of this request.
