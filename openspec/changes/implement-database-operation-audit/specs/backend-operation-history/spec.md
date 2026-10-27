# Spec Delta

## Purpose

Provide company administrators with a secure, database-backed view of successful personnel and account operations, using stable historical summaries and safe snapshots.

## ADDED Requirements

### Requirement: Administrator operation history
The backend SHALL persist administrator personnel and account mutation records in the same enterprise transaction as the successful mutation. It SHALL expose administrator-only list and detail endpoints under `/api/v1/admin/operation-logs`, enforce enterprise isolation, support the Web keyword/module/action/source/result/date filters and cursor pagination, and return stable before/after display snapshots. New records SHALL retain actual before/after entity versions when available and SHALL return null when a historical version was not captured. Detail SHALL list real successful history records for the same enterprise, object type and object ID in chronological version order; unsuccessful writes SHALL NOT create committed versions. Snapshots SHALL preserve actor, target, organization and supervisor names as observed at operation time. Passwords, tokens, identity numbers, phone numbers and raw credentials SHALL NOT be persisted or returned.

#### Scenario: List operation history
- **WHEN** an administrator requests the operation-log list with supported filters
- **THEN** the backend returns a cursor page scoped to that enterprise with total count and snapshot token

#### Scenario: Read operation detail
- **WHEN** an administrator requests a returned operation ID
- **THEN** the backend returns its summary, actor/object metadata, the actual before/after versions where known, all real successful same-object history, and safe before/after snapshots

#### Scenario: Historical organization and personnel labels
- **WHEN** an administrator opens an older operation after related organizations or people are renamed or moved
- **THEN** snapshots continue to show names captured at the time of each operation, and unavailable legacy values remain null

#### Scenario: Failed mutation is not a committed version
- **WHEN** a mutation fails and its transaction rolls back
- **THEN** no successful operation-log version is added to the object's history

#### Scenario: Unauthorized or foreign operation
- **WHEN** a non-administrator or another enterprise supplies the endpoint or ID
- **THEN** the backend denies access or returns the uniform missing-resource response without leaking the record

#### Scenario: Sensitive data exclusion
- **WHEN** an operation changes an account with personal or credential fields
- **THEN** its persisted and returned snapshots exclude identity numbers, phone numbers, password hashes and session tokens
