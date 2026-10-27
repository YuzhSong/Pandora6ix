## ADDED Requirements

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
