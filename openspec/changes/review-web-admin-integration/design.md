## Context

Review PR #11 (7nfc, Web-cy 936a931) into dev f5283e6. The implementation replaces local directory storage with the existing Spring API and adds visual/form updates; its author recorded Web-only scope in web documentation. Repository guidance requires OpenSpec and a non-author review before integration.

## Goals / Non-Goals

**Goals:** Validate the submitted Web implementation against existing backend contracts and record server-owned identity/data behavior. Keep changes independently buildable and review the exact remote head.

**Non-Goals:** New backend APIs, Android integration, approval/inbox, production deployment, live server push, or broad dependency upgrades.

## Decisions

- Review in a managed worktree; preserve the user's backend-syz checkout and untracked files.
- Auth requires ADMIN from the server; tab-scoped bearer sessions expire/revoke and clear cached directory state. A local workspace session must not confer server identity.
- Parallel workspace reads after /me, full cursor traversal, generation guards and server-returned snapshots avoid mixing identities or pretending failed writes succeeded.
- Backend expectedVersion and explicit refresh replace local-storage mutation notices for external edits; no unsupported server notification mechanism is promised.
- Run repository unit/build/lint plus its isolated real Spring/H2/Vite integration. Browser plugin is absent; use bundled Playwright with preinstalled Chrome and a new test profile, random test credentials and isolated loopback backend. Store transient scripts/screenshots outside the repository.
- Record the inherited source-map-js 1.2.1 development advisory separately: lockfile and dependency manifests are unchanged from dev. Do not broaden this integration into an untested dependency upgrade.

## Risks / Trade-offs

- No GitHub CI jobs/status contexts are configured → verify locally and record commands on the reviewed commit.
- Custom select/popover and validation changes → exercise keyboard controls, real saves, responsive layout and dark mode in a rendered browser.
- No production address/credentials → isolated local verification is not production or Android acceptance.

## Migration Plan

Approve from a non-author account after checks; merge the reviewed SHA using a normal PR merge into dev. Archive this completed integration change with main-spec synchronization after merge and validation. Preserve existing local browser directory data; it is not silently uploaded.
