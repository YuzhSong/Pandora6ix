# Design

## Scope

This change establishes the backend skeleton only. It does not implement authentication, business CRUD, database migrations, or the reserved inbox/review workflow. Those features will depend on reviewed OpenSpec changes and confirmed policy decisions.

## Package Layout

`com.pandora6ix.backend`

- `api`: HTTP-level error handling, request ID propagation, and future DTO/controller boundaries
- `application`: use-case ports grouped by identity, tasks, journals, tags, calendar, and dashboard
- `domain`: stable enums and value objects shared by application services
- `infrastructure`: adapters and configuration; persistence remains absent in this change
- `controller`: existing liveness controller retained as a compatibility endpoint

The first implementation uses Java interfaces as application ports. Ports are intentionally empty or minimally typed until their corresponding business change defines request and response contracts.

## Error Contract

`ApiError` is serialized with:

- `timestamp`
- `status`
- `code`
- `message`
- `path`
- `requestId`

`GlobalExceptionHandler` handles validation, bad requests, missing routes, and unexpected failures. A servlet filter creates or forwards `X-Request-Id`, adds it to the response, and makes it available to the error handler.

## Configuration

The default configuration remains database-free. `application-local.yml` documents local defaults and `application-prod.yml` is reserved for deployment configuration supplied outside source control. No credentials or datasource bean is added.

## Validation

The backend keeps `/health` and `/actuator/health` as the first smoke-test boundary. Unit tests cover enum stability and API error serialization; a context test verifies the application starts without persistence.
