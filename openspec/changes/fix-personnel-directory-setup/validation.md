# Validation — 2026-10-19

## Root cause and provenance

The live songyuzheng user had supervisorUserId=null. The same-team active TEAM_LEAD Lu Chuan existed; organization membership does not populate the explicit supervisor field used by task approval. The checked-in pinyin mapping had been delivered in PR 20 but not run against the company directory.

Git provenance: the login-change explanatory note came from this assistant's PR 19 (`a476069`, YuzhSong); the historical-profile banner and missing-password name wall came from Web-cy's PR 20 (`e1d332c`, original `2138c6a`, author 7nfc). The generic supervisor hint came from the earlier Web-cy commit `aedbfd1`, author 7nfc. The integration review retained those notices; this change removes the redundant blocks without removing field validation.

## Actual cloud repair

Using the existing authenticated company ADMIN API, preflighted the full existing mapping, expected display names, target uniqueness and 31 non-admin IDs. Renamed 26 accounts, retained five existing pinyin logins, and separately set songyuzheng's supervisor to the verified same-team Lu Chuan ID. Applied 27 versioned user updates; administrator records and unrelated reporting relationships were preserved. All 32 IDs, employee profiles, organizations, statuses and roles were verified after writes. No account creation, password reset or offline database opening occurred.

Authenticated songyuzheng, luchuan, chenlin and the renamed active phone-testing account using their original employee password; each resolved to its original ID. Updated the private Android test-account handoff metadata by user ID. No real task was created in the cloud during verification. Private preflight snapshots, mapping receipts and operational helpers remain outside committed source; they contain no exported passwords or hashes.

## Code verification

- `npm test`: 88 tests passed, no failures; `npm run lint` passed with the two existing App.tsx effect warnings; TypeScript/Vite production build passed.
- Real Spring/file-H2/Vite/HTTP-repository integration passed, using the previously verified unchanged backend artifact from dev 0f8f4aa.
- Strict OpenSpec validation and diff checks passed. No backend, Android, API, dependency or infrastructure changes were made.

## Rendered verification

Browser plugin not available; used regular Playwright 1.62.1 with existing Chromium revision 1223. The flow was ADMIN login → existing-person rename/duplicate error → save/reload → reset password → bulk handoff availability → reload clearing session-only credentials. At loopback URL http://127.0.0.1:58462, desktop 1365×900 and narrow 390×844 passed page identity, nonblank render, no framework overlay and no unexpected page/console errors. The edited dialog omitted all three explanation blocks, retained historical empty-profile edits and saved renamed accounts. Bulk handoff was absent initially, appeared with one valid account after reset, omitted the missing-password name list and disappeared after reload. Copy/download controls remained available for actual captured credentials.

The narrow editor and desktop one-account handoff screenshots were inspected. Evidence and script remain outside Git under `%LOCALAPPDATA%/Pandora/directory-fix-qa/`, including `verification.json`, `mobile-editor.png`, `handoff-one-account.png` and `verify-directory.mjs`. The local backend and browser were stopped after checks.
