# Spec Delta

## Purpose

Provide company administrators with a secure, database-backed view of successful personnel and account operations, using stable historical summaries and safe snapshots.

## ADDED Requirements

### Requirement: Administrator operation history
The backend SHALL persist administrator personnel and account mutation records in the same enterprise transaction as the successful mutation. It SHALL expose administrator-only list and detail endpoints under `/api/v1/admin/operation-logs`, enforce enterprise isolation, support the Web keyword/module/action/source/result/date filters and cursor pagination, and return stable before/after display snapshots. Passwords, tokens, identity numbers, phone numbers and raw credentials SHALL NOT be persisted or returned.

#### Scenario: List operation history
- **WHEN** an administrator requests the operation-log list with supported filters
- **THEN** the backend returns a cursor page scoped to that enterprise with total count and snapshot token

#### Scenario: Read operation detail
- **WHEN** an administrator requests a returned operation ID
- **THEN** the backend returns its summary, actor/object metadata, versions and safe before/after snapshots

#### Scenario: Unauthorized or foreign operation
- **WHEN** a non-administrator or another enterprise supplies the endpoint or ID
- **THEN** the backend denies access or returns the uniform missing-resource response without leaking the record

#### Scenario: Sensitive data exclusion
- **WHEN** an operation changes an account with personal or credential fields
- **THEN** its persisted and returned snapshots exclude identity numbers, phone numbers, password hashes and session tokens
