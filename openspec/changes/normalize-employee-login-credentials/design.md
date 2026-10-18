# Design

## Context
See proposal. Current UserPatch intentionally lacks loginName and password. Cloud H2 is a file database, and deployment credentials cannot execute arbitrary commands. The user selected team-lead execution rather than providing maintenance access.

## Goals / Non-Goals
Goals: a reviewable offline mapping and atomic maintenance tool, preserving existing IDs and associations.
Non-goals: account recreation, changing administrators, executing cloud maintenance here.

## Decisions
- Use Java source-file launch with the deployed H2 and Spring Security BCrypt libraries. No installation or schema change is needed.
- Require an existing file DB, stopped backend and operator backup. Preview locks/validates and rolls back; only --apply commits. Password comes from environment and is never printed or saved in the mapping.
- Match enterprise domain, old login and expected display name; protect any ADMIN account; reject duplicate targets and collisions with unselected accounts. Lock rows, rename through temporary unique names, write independently salted BCrypt hashes, increment versions and revoke all selected users' sessions in one transaction.
- Use name pinyin; distinguish identical full display names with suffixes -1/-2. Keep original administrator login and credentials.
- For ongoing recovery, POST /admin/users/{id}/password-reset accepts expectedVersion and newPassword, requires ADMIN and rejects ADMIN targets. Reuse directory enterprise lock, optimistic version check and BCrypt byte limits; increment user version and revoke all employee sessions transactionally. Return only the existing admin user projection, never plaintext/hash. Existing cloud requires deployment before using Web reset.

## Risks / Trade-offs
- User-selected shared password → apply only the supplied value; tool does not expose plaintext or introduce a password backdoor.
- Live file lock or stale mapping → stop and rollback; operator reviews output before applying. Rerunning an already applied rename fails before writes.
- Recovery → operator retains pre-change offline backup; script transaction handles failures before commit.
