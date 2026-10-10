# backend-foundation Specification

## Purpose
Define the database-free backend boundary, uniform API error contract, application ports and stable task workflow vocabulary.
## Requirements
### Requirement: Expose a versioned backend boundary

The backend SHALL reserve `/api/v1` for business APIs while keeping liveness endpoints outside that prefix.

#### Scenario: Business API namespace

- **WHEN** a future business controller is added
- **THEN** its route starts with `/api/v1` and does not replace `/health` or `/actuator/health`

### Requirement: Return a uniform error contract

The backend SHALL return a JSON error body containing `timestamp`, `status`, `code`, `message`, `path`, and `requestId` for handled HTTP errors.

#### Scenario: Unknown route

- **WHEN** a client requests an unmapped route
- **THEN** the response is JSON, has HTTP status 404, uses code `RESOURCE_NOT_FOUND`, and includes a request ID

### Requirement: Keep modules independently replaceable

The backend SHALL separate controller, application, domain, and infrastructure concerns so business rules do not depend directly on HTTP or database implementations.

#### Scenario: Use-case port

- **WHEN** a task, journal, tag, calendar, dashboard, or identity feature is implemented
- **THEN** its application boundary can be called through a port without importing a controller class

### Requirement: Make domain workflow states explicit

The backend SHALL define typed values for `PERSONAL` and `DELEGATED` tasks, `ALL_DAY` and `TIMED` scheduling, pending/approved/rejected review, assignment execution states, and completion sources.

#### Scenario: Client state mapping

- **WHEN** an Android or Web client maps a task state
- **THEN** it uses stable machine values rather than localized display text

### Requirement: Start without persistence

The backend SHALL start in local development without MySQL or another external persistence service.

#### Scenario: Local startup

- **WHEN** the backend is started with the local profile and no database is available
- **THEN** the application starts and exposes liveness and actuator health
