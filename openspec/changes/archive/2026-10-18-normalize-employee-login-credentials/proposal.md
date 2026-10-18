# Proposal

## Why
The user confirmed that existing employee accounts should use name pinyin and one supplied initial password, with the team lead executing the cloud database maintenance. Current APIs cannot rename accounts or administratively reset passwords.

## What Changes
- Supply a one-off offline H2 maintenance tool and an explicit mapping of the current 31 employee accounts.
- Preserve user IDs, profiles, roles and business records, protect administrators, validate collisions, reset passwords with BCrypt and revoke old sessions in one transaction.
- Default to rollback-only preview; provide backup, execution and verification instructions for the team lead. Do not run against cloud from this chat.
- Add an ADMIN-only, versioned employee password-reset API for forgotten-password recovery. Revoke target sessions and preserve personnel and business data; Web integration is tracked in integrate-web-personnel-backend.

## Capabilities
### New Capabilities
None.
### Modified Capabilities
- `backend-identity-directory`: controlled employee credential maintenance.

## Impact
Maintenance files, directory API/service/contracts, generated OpenAPI and backend integration tests. No schema migration, automatic deployment or database access permission changes.
