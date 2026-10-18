## Context

The current PATCH user command updates profile, status and roles but has no loginName field. The database already has a unique (enterprise_id, login_name) key, and authentication matches enterprise domain and login name exactly. Sessions and business records reference immutable user IDs. Web independently disables the input and rejects changed login names in its HTTP repository.

## Goals / Non-Goals

**Goals:** Allow an administrator to maintain existing company account login names while preserving identity, passwords, profile data, authorization and optimistic concurrency. Return useful conflict/validation responses and document a compatible contract.

**Non-Goals:** Automatic pinyin generation, bulk renaming cloud users, changing login case/normalization semantics, password resets, new routes or database migrations.

## Decisions

- Add nullable loginName to UserPatch. Omitted/null preserves the stored name; a supplied name must contain a non-whitespace character, have no surrounding whitespace and be 1–128 characters. Keep exact case-sensitive matching consistent with create/login and the database key; do not silently trim or transliterate names.
- Reuse the existing enterprise write lock. Read only the target in the authenticated enterprise, check expectedVersion, then detect another account with that name in the same enterprise. Return 409 LOGIN_NAME_TAKEN for duplicate names and 409 VERSION_CONFLICT for stale edits. Keep the database unique constraint as the final safeguard.
- Update login_name in the existing versioned user UPDATE, with all existing reference/role/status protections retained. Do not write password_hash or change ID-based relations. Existing bearer sessions remain bound to the same ID; account disabling continues revoking sessions as before. A renamed admin can continue using the current session.
- Reject blank names and surrounding whitespace with a loginName field error, including Unicode whitespace. Existing login forms trim surrounding whitespace, so accepting it on rename could make the saved identifier impossible to submit. Bean validation supplies length checks. Regenerate the existing OpenAPI artifact from the command record and document service-level rules.
- Backend regression tests exercise the actual HTTP endpoints and database state: old/new login, unchanged hash/profile/relations, legacy omission/null, tenant boundaries, duplicates, invalid names, self-admin updates and concurrent rename requests.

## Risks / Trade-offs

- Same-name accounts exist in different enterprises → duplicate checks and updates are enterprise-scoped, with tests proving another enterprise's name is allowed and its users cannot be changed.
- Existing clients do not send the new field → nullable additive field preserves compatibility; roles-only edits do not reset loginName.
- User confirmed delivering Web with the backend. Enable only the existing login-name control and add loginName to PATCH when it changes; roles-only updates continue omitting it. Verify the returned name before accepting a successful write, so an older backend cannot silently ignore the field and claim success. Translate LOGIN_NAME_TAKEN into a Chinese duplicate-name message.
- Historical nullable profiles must not prevent an unrelated login-name edit → make native profile required flags creation-only; the existing HTTP repository still requires complete valid profile details for new people or changed profile data.

## Migration Plan

No schema or data migration. Submit reviewed backend-syz changes as a PR into dev. Deployment occurs only after merge through the existing pipeline; do not modify production login names as part of implementation.
