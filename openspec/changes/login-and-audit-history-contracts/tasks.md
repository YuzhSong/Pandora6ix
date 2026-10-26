# Tasks

## 1. Login contract

- [ ] 1.1 Add optional accountIdentifier parsing and tenant-scoped authentication while preserving the legacy pair; verify same-name cross-enterprise and legacy-login integration tests.
- [ ] 1.2 Update generated OpenAPI and API documentation; verify contract generation and diff check.

## 2. Immutable audit history

- [ ] 2.1 Add a nullable additive migration for actual versions and historical actor/object names; verify legacy columns remain null.
- [ ] 2.2 Record typed safe values and immutable organization/personnel labels for account and lifecycle changes; verify rename/move snapshot tests.
- [ ] 2.3 Return actual same-object history and nullable unknown versions; verify committed ordering and transaction rollback behavior.

## 3. Conflict diagnostics

- [ ] 3.1 Log sanitized SQLState/vendor/constraint identifiers with request IDs; verify structured logging excludes SQL, credentials and request values.
- [ ] 3.2 Run focused job-change conflict coverage, complete backend tests, OpenAPI check, and strict OpenSpec validation.
