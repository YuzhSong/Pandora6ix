# Design

## Context

Login currently requires `enterpriseDomain` and `loginName`. Operation snapshots stringify values and hard-code version 0; personnel history stores IDs and resolves some labels from current directory data. Integrity exceptions are normalized without server diagnostics.

## Goals / Non-Goals

**Goals:** Preserve tenant-safe account resolution, expose actual historical versions and names, and make future integrity conflicts diagnosable by request ID.

**Non-Goals:** Infer a tenant from a non-unique username, rewrite existing historical records with guessed data, or claim to identify the unavailable historical cause of Zhao Ke's prior 409.

## Decisions

- Parse a supplied account identifier at its final `@` into exact login name and enterprise domain. Require both non-empty components and perform a single tenant-scoped lookup. Keep the existing pair accepted for compatibility.
- Add nullable version and immutable display-name columns to the operation log; store safe structured snapshots, deriving the actual entity version at mutation time. Query history by enterprise, object type and object ID, newest first. Legacy unavailable versions/names remain null.
- Extend personnel-change before/after snapshots with organization and supervisor names at capture time, and store actor display/login names on the record. Do not backfill historical labels from today's directory.
- For integrity failures, walk causes for SQLState/vendor code and extract only a recognized constraint identifier. Log request ID, HTTP method/path and diagnostics; do not log SQL text, exception message or request body. Client response remains generic `DATA_CONFLICT`.

## Risks / Trade-offs

[Existing duplicate usernames] → Domain is part of the required new identifier, so identical usernames remain unambiguous without a global uniqueness migration.

[Legacy rows lack reliable versions/names] → Return null where unavailable; do not fabricate or retroactively infer values.

[Database exception formats differ] → SQLState/vendor diagnostics are primary; constraint extraction is best-effort and dialect-neutral, with no sensitive exception text emitted.

## Migration Plan

Apply additive V9 nullable columns. New writes populate the columns; existing rows stay null. Rollback requires removing the new response fields/queries and is not a data-destructive operation, but preserve the migration once deployed.
