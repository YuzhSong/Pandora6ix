# Tasks

## Foundation

- [x] Add validation and web support dependencies required by the API boundary.
- [x] Create API error model, exception types, request-ID filter, and global exception handler.
- [x] Create application port packages for identity, tasks, journals, tags, calendar, and dashboard.
- [x] Create stable domain enums for task and workflow states.
- [x] Add local configuration documentation without datasource credentials.

## Verification

- [x] Add tests for request-ID propagation and uniform 404 errors.
- [x] Add tests for domain enum values.
- [x] Run `mvn test`.
- [x] Run `openspec validate backend-architecture-foundation --strict`.
