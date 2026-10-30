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

### Requirement: Persistent writes survive connection replacement
The backend SHALL preserve task creation and database constraint enforcement after schema initialization connections close, pooled connections retire, and a file database is reopened. Valid PERSONAL and DELEGATED tasks using ALL_DAY or TIMED intervals SHALL retain existing workflow behavior.

#### Scenario: Constraint creator connection retires
- **WHEN** an existing task database receives valid writes from replacement connections after the initializing connection closes
- **THEN** writes succeed and invalid enum/check values remain rejected by database constraints

#### Scenario: File database resumes after restart
- **WHEN** the backend opens an existing H2 file database using the pinned runtime engine
- **THEN** committed business data remains available and subsequent valid task writes succeed

### Requirement: Safe accurate constraint diagnostics
Database conflict logging SHALL extract an available actual constraint identifier and SQLState without logging SQL text, parameters, credentials or request contents.

#### Scenario: Invalid CHECK evaluation
- **WHEN** H2 reports a quoted CHECK constraint identifier and SQLState 23514
- **THEN** diagnostics record that identifier rather than the word invalid and keep the client error envelope compatible
