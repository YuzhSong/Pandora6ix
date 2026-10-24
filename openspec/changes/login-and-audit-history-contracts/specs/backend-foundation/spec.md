# Spec Delta

## MODIFIED Requirements

### Requirement: Return a uniform error contract
The backend SHALL return a JSON error body containing `timestamp`, `status`, `code`, `message`, `path`, and `requestId` for handled HTTP errors. Database integrity conflicts SHALL retain a generic client message and code while server diagnostics record request ID, method/path, SQLState, vendor code and a safely extracted constraint identifier when available. Diagnostics SHALL NOT contain raw SQL, request bodies, credentials or personal values.

#### Scenario: Unknown route
- **WHEN** a client requests an unmapped route
- **THEN** the response is JSON, has HTTP status 404, uses code `RESOURCE_NOT_FOUND`, and includes a request ID.

#### Scenario: Integrity conflict diagnosis
- **WHEN** a database integrity violation is mapped to `DATA_CONFLICT`
- **THEN** server logs include correlation and database error identifiers without exposing SQL or user-submitted values, while the client receives only the generic error contract.
