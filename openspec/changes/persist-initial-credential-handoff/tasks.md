# Tasks

## 1. Backend durable handoff
- [x] 1.1 Add encrypted expiring storage, atomic creation/reset/change lifecycle and scoped no-store retrieval; verify security/lifecycle/concurrency tests and document key/migration requirements with OpenAPI.

## 2. Web onboarding delivery
- [x] 2.1 Replace page-only lookup with fresh server reads for details and batch copy/download; verify repository/flow tests, reload behavior and concise rendered UI without explanatory prose.

- [x] 2.2 Show available initial passwords in the table and download real XLSX with text cells; verify workbook roundtrip, preserved leading zeros, employee login using exported password and rendered preview.

- [x] 2.3 Add editable bulk creation rows, reuse validation and submission, remove explanatory prose and verify table validation, build and local browser creation.

## 3. Integration checks
- [x] 3.2 Unify CSV import into the editable table, remove raw text entry and verify background scroll locking and local imported-row correction.
- [x] 3.1 Run backend tests/package, Web tests/build/lint, real isolated lifecycle integration and strict OpenSpec validation; record results and inspect rendered dialogs.

## Workflow follow-up
- Submit for review when requested; deploy the configured backend before Web and archive after merge.
