## 1. Backend login-name maintenance

- [x] 1.1 Add a compatible optional loginName update with validation, enterprise uniqueness and expectedVersion enforcement.
- [x] 1.2 Add HTTP integration coverage for authentication, preservation, legacy clients, errors, isolation and concurrent writes.
- [x] 1.3 Regenerate OpenAPI and document rename behavior and frontend handoff.

## 2. Web integration

- [x] 2.1 Enable existing-person login editing, submit changes, verify server results and translate duplicate errors without blocking historical profiles.
- [x] 2.2 Cover renamed/legacy/duplicate responses and run Web unit tests, lint, build and actual backend integration.
- [x] 2.3 Exercise the editor in a disposable local browser session and record actual evidence or a concrete runtime limitation.

## 3. Verification and delivery

- [x] 3.1 Run backend tests/package, API contract drift check, strict OpenSpec validation and diff checks.
- [x] 3.2 Record validation evidence and submit backend-syz PR to dev without merge or production data edits.
