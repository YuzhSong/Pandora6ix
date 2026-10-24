# Proposal

## Why

The `backend-xjy` branch currently contains only a Spring Boot liveness endpoint, while the Android client has already established the first domain vocabulary for tasks, calendars, work logs, tags, dashboard panels, and user settings. The backend needs a maintainable boundary before real persistence and business workflows are implemented, otherwise client mock data will become an accidental API contract.

## What Changes

- Add a modular backend structure separating API, application, domain, and infrastructure responsibilities.
- Establish versioned JSON API conventions under `/api/v1`, including request correlation and a uniform error body.
- Add explicit domain vocabulary for task kind, review status, assignment status, time kind, completion source, and tag roles.
- Add configuration profiles for local development and future database integration without requiring a database to start.
- Add ports for identity context, task, journal, tag, calendar, and dashboard use cases so controllers can be added without coupling to storage.
- Preserve the existing `/health` endpoint and keep reserved inbox/review contracts out of runtime behavior.

## Capabilities

### New Capabilities

- `backend-foundation`: Runtime configuration, API error conventions, module boundaries, and domain vocabulary needed by later business features.

### Modified Capabilities

- None.

## Impact

- `backend/pom.xml` and `backend/src/main/**`
- Backend startup configuration and test structure
- No Android or Web source changes
- No database schema, credentials, migration, or external infrastructure is introduced in this change
