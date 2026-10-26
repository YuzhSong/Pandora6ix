# Proposal

## Why

Web needs company-independent login input without confusing same-named accounts across enterprises. Audit records also need stable, truthful versions and historical names, while prior job-change conflicts need enough sanitized diagnostics to identify the actual database constraint.

## What Changes

- Accept an unambiguous `loginName@loginDomain` account identifier without a separate company field, retaining the legacy request fields during client migration.
- Return genuine nullable snapshot versions and actual same-enterprise/object history from operation-log details.
- Freeze actor, target, organization and supervisor display names in new operation/personnel history records.
- Log sanitized SQLState/vendor/constraint diagnostics with request IDs for integrity conflicts; never log raw SQL or personal values.

## Capabilities

### New Capabilities

### Modified Capabilities
- `backend-identity-directory`: define account-identifier login and immutable personnel history names.
- `backend-foundation`: add server-side traceability for data-integrity conflicts without exposing database details to clients.

## Impact

Backend login contract, V9 additive database migration, operation-log and personnel-history serializers, error diagnostics, OpenAPI contract and integration tests. Web and Android can migrate their login forms independently to the new account identifier.
