# Design

## Context

The Web client expects `/api/v1/admin/operation-logs` list/detail responses with cursor pagination, stable snapshots, and administrator-only access. Existing personnel lifecycle writes run inside Spring transactions and already persist before/after JSON snapshots.

## Goals / Non-Goals

**Goals:** Provide trustworthy persisted records for administrator personnel/account operations and the Web list/detail contract.

**Non-Goals:** Capture every historical task/journal mutation in this first slice, export server-side files, or expose passwords, tokens, identity numbers, or phone values.

## Decisions

- Add an `operation_log` table with actor/object metadata, module/action/source/result, summary, request ID, failure reason, and before/after JSON snapshots.
- Record successful personnel lifecycle operations in the same transaction as their business mutation.
- Use cursor pagination ordered by occurred time and ID, with filters matching the Web client. Details return the base record plus a versions array and snapshots.
- Keep failure records out of mutation paths until a transaction-safe error event boundary is available; the API supports failure fields for future coverage.

## Risks / Trade-offs

- Initial coverage is limited to personnel/account administration; the UI will show those records while task/journal coverage remains incomplete.
- JSON snapshots can grow with object size; snapshots are bounded to safe display fields and excluded secrets.

## Migration Plan

Apply the additive Flyway migration. Rollback is removing the endpoint and migration only before production data depends on the table.
