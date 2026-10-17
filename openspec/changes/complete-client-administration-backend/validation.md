# Validation — 2026-10-17

## Integrated upstream PRs

- Reviewed and merged #12 at e18b4e5 and #13 with review corrections at 00871fc into dev a493dfe. backend-syz was fast-forwarded and pushed to the same dev before implementation.
- Web review: 66 tests pass, lint exits zero with two existing effect warnings, production build passes.
- Android review fixes: empty POST logout, account-scoped persisted drafts/avatar preferences, stale-account response rejection, calendar fixture migration to TaskItem; selected journal deletion now removes selection references with version conflict protection.
- GitHub Actions run 39015531456 succeeded; public /deployment.json reports a493dfec83b4c6bb7b7b807b9f8e6a3b0444d8d5, runNumber 6 / attempt 1; /backend/health is UP.

## New backend-syz implementation

- Maven package passes 58 tests, zero failures/errors/skips. The additional company-timezone boundary regression passes separately: 59 unique backend tests verified. Evidence: backend/target/admin-backend-build.log, admin-full-suite XML, admin-timezone-test.log.
- New cases cover profile creation/login/reload, null legacy data, role-only preservation, invalid values and duplicate rollback, unauthorized creation, bootstrap restriction, pure ADMIN business denial, leaf conversion both directions, root/child/role rejection, concurrent promotion/conversion invariant, department metadata with cursor paging and no personal secrets, immutable backfills and out-of-window creation.
- Company timezone regression changes enterprise timezone across the date boundary immediately after publication and confirms edit/delete denial, independent of elapsed time. Android JVM boundary test checks Asia/Shanghai midnight with a different device timezone.
- OpenAPI exports/verifies 46 operations. Administrative User and non-sensitive Profile schemas are separate; profile fields and date/actions are documented.
- Real HTTP smoke passes 112 requests against isolated file H2, including V1 company/account upgrade through V3, old null data, required fields saved and reloaded across restart, existing task approval/execution flows, journal date policy and immutable backfills. No cloud business fixtures were created for these checks.
- Real Spring + H2 + Vite + Web repository acceptance passes: profile creation and reload, role edits preserving identity/phone, department↔team persistence, company items, authentication, password and logout. Tests use the explicitly selected current backend jar and a fresh loopback-only database.
- Android assembleDebug and 18 JVM tests pass. Web 66 tests, lint (two existing warnings) and build pass. Device/end-to-end UI acceptance is not claimed.

## Live account and release boundary

User explicitly selected department-manager level 1. Existing test account was updated through administrator PATCH, moved from Android team to its parent 产品研发部, assigned DEPARTMENT_MANAGER, and given the existing FOUNDER as direct supervisor. Original credentials remain local and unchanged. Login confirms manage/createDelegatedTasks true and 16 recipient candidates; no task was submitted. The pre-change profile was saved outside Git in the local private Pandora directory.

The deployed a493dfe still contains #13's old rolling 24-hour rule. The user's subsequent company-date correction, personnel migration and permissions in this change require non-author review, merge into dev and a successful new deployment. Do not claim these new APIs are already live. No system operation audit collector/store/query or avatar endpoint was added; storage remains a client decision. No other server projects or services were modified.
