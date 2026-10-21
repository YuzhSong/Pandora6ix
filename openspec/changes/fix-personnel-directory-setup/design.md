## Context

The live directory has 31 non-admin accounts. Lu Chuan exists as an active TEAM_LEAD in the same backend team as songyuzheng, whose supervisorUserId is null. The checked-in pinyin map was delivered but never applied. Current Web exports only passwords captured in browser memory, so old accounts legitimately lack exportable plaintext credentials.

## Goals / Non-Goals

Goals: complete the requested account renaming, fix songyuzheng's confirmed reporting relation, and remove redundant notices while retaining validation and working handoff.

Non-goals: password resets, account recreation, unrelated users' reporting changes, storing/recovering plaintext passwords, altering workflow approval rules or adding APIs.

## Decisions

- Read the full company directory, retain a private local pre-change snapshot, and preflight all 31 mappings, names, uniqueness and roles before mutation. Use PATCH /admin/users with expectedVersion and original profile/role/organization values. Skip unchanged names; combine the requested supervisor repair with that user's existing command. Stop on unexpected versions or identities and retain per-user receipts for recovery.
- Use the existing name mapping, including distinct suffixes for two identically named phone-testing users. Preserve the existing ADMIN login. Verify all IDs, profiles, statuses, roles and unrelated supervisor relationships after writes; verify selected logins without printing credentials or creating cloud tasks.
- Remove the login-name note, legacy-profile banner and generic supervisor hint from the editor. Keep concrete error messages and field requirements. Omit the missing-password name wall, show bulk handoff only when existing non-admin accounts have credentials in the current browser session, and disable empty downloads defensively.

## Risks / Trade-offs

- Name change affects the next login → preserve passwords and ID-based sessions, publish the updated name mapping, and update the local test-account handoff metadata by identity.
- Concurrent directory edit → expectedVersion rejects it; stop and re-read rather than overwriting.
- Online corrections are per user → preflight all targets, keep original values and receipts, and verify the entire directory after completion.
- Browser reload intentionally loses handoff passwords → do not pretend to retrieve hashes as plaintext or list every old account as an error.
