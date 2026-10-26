# Tasks

## 1. Persistence and API

- [x] 1.1 Add the operation-log migration and service/controller list/detail contract; verify the API returns Web-compatible pages and detail objects.
- [x] 1.2 Record personnel lifecycle and account mutations with safe snapshots in the transaction; verify the focused integration test and sensitive-field exclusion.

## 2. Verification

- [x] 2.1 Add integration tests for list/detail, filtering, isolation and sensitive-field exclusion; verify with `BusinessApiIntegrationTests`.
- [x] 2.2 Run backend tests and strict OpenSpec validation; verify both pass.

## 3. Historical integrity

- [ ] 3.1 Add nullable actual-version and immutable name columns; verify migration preserves legacy rows as unknown.
- [ ] 3.2 Populate exact actor/object/organization/supervisor labels and versions on new writes; verify rename/move history remains unchanged.
- [ ] 3.3 Return same-enterprise/object successful history and nullable legacy versions; verify history ordering and rollback exclusion.
