# Design

## Context

Authentication uses BCrypt; Web currently keeps entered creation/reset passwords only in React state. See proposal.md for motivation. The existing self-service endpoint serves both Android and Web.

## Goals / Non-Goals

Deliver initial/reset credentials after reload while ensuring self-service passwords never become recoverable. No cloud changes, historical backfill, default-password assumption or permanent password vault.

## Decisions

- Add a separate Flyway V5 handoff table, storing AES-256-GCM ciphertext with random 96-bit nonces and enterprise/user-bound authenticated data. External Base64 32-byte key is required, never committed, with configurable TTL (default seven days). BCrypt remains the authentication source; storing only hashes cannot support later delivery, and plaintext persistence is rejected.
- Bind each record to a fingerprint of the current authentication hash. Mark changed and erase ciphertext in the same employee password transaction. Mismatched hashes from offline maintenance fail closed. Scheduled cleanup removes expired/mismatched secrets, leaving concise state. Reset replaces the record transactionally. Missing key fails writes with an actionable error; invalid configured key fails startup.
- All capture/reset/self-change/export operations serialize using the existing enterprise lock then user lock ordering. Export is admin-only, excludes ADMIN and disabled targets and returns current names plus AVAILABLE/CHANGED/EXPIRED/UNAVAILABLE status. Fetches have Cache-Control no-store. No secret appears in ordinary directory/profile projections, logs or localStorage.
- Web loads handoffs on demand, discards on close/error/session loss, refreshes every five seconds and on focus. Copy/download always fetch again. Previously downloaded credentials cannot be revoked from a recipient's filesystem; server guarantees no export after a committed change. No UI explanatory prose.

## Risks / Trade-offs

- External key loss makes outstanding handoffs unreadable; fail closed and reset affected credentials. Back up key in approved secret storage separately from DB; key rotation requires draining old handoffs or planned reset.
- A delivery window bounds secret retention but requires reset when it expires. Exported files need normal operational handling outside application control.
- A failed read empties plaintext rather than retaining stale data; HTTP responses never claim delivery success when the key or server is unavailable.

## Migration Plan

Configure the external key, back up database, deploy backend with V5 migration before Web. Existing users remain UNAVAILABLE; no default password is synthesized. Do not roll back to old credential-writing code while keeping handoff records, since it cannot revoke them on employee change; clear handoff ciphertext first during coordinated rollback. No deployment is performed by this implementation task.

## Review update

Bulk creation uses one editable table with add/delete rows, selectors and direct validated submission. CSV file import and template download share its toolbar; imported values populate the table for correction, with no mode switch or raw CSV area. Structural import failures preserve existing rows. Empty rows are ignored. All rows use the existing validation and one-shot submission path. Open dialogs lock document scrolling with CSS and contain internal overscroll; closing restores scrolling automatically. No input or password is persisted in the browser.

The handoff table includes plaintext initial passwords only for AVAILABLE records. Downloads use a lazily loaded ExcelJS writer to generate genuine XLSX, all credential cells explicitly text to preserve leading zeros and formula-like passwords. Copy remains plain text. Local --preview fixtures use a readable password; ordinary automated runs retain random fixture passwords. No production password defaults or cloud edits are introduced.
