# Validation — 2026-10-29

## Log table-header filters
- Named six columns: time, operation record, module, source, result, details. Existing filter semantics moved to headers; controls stay available in loading/error/empty states.
- On the real logged-in page: selected failure -> 0 results with headers retained -> reset -> selected Web source -> 24 results whose displayed sources were Web -> reset to all 42 records. No mutation requests were performed.
- Header rectangles had the same 65px height; separate columns align module/source/result values. Long selected header labels truncate rather than wrap and displace alignment.
- Build, 19 operation/calendar tests and strict OpenSpec validation passed.

## Personnel profile refinement
- Real logged-in localhost:5173 page verified: profile modal width 980px, height approximately 656px, content area 498px without scrolling for the administrator. Sections show organization, basic and account data; identity is masked by default, login identifier is plain text and footer contains only close/edit.
- Initial credentials are requested only after expanding the compact profile's credential control; creation receipts keep immediate credential delivery. Reset remains explicit in the account section.
- Build, 8 related profile/account tests, diff checks and strict OpenSpec validation passed after this refinement. No production data modified.

- Web complete suite: 150 passed, including existing untracked businessReader tests. Relevant targeted suite: 41 passed; avatar tests rerun after Accept header refinement: 2 passed.
- Production build and lint passed. Existing React effect warnings and ExcelJS large-chunk warning remain; no new lint warnings in the added components.
- Strict OpenSpec validation passed. `git diff --check` passed.
- Tested the actual production bundle against an isolated in-memory HTTP fixture on localhost:5180; no cloud personnel, permissions, credentials or avatars were mutated.
- UI: cards preserved; compact table shows organization paths, supervisors, roles and separate states. Reset changed 4/5 in-service results to 5/5 all-person results including departed staff. Direct/inclusive counts were 1/3 for a department, excluding departed staff and retaining disabled in-service staff.
- UI: in-service disabled account opens explicit confirmation, restores via PATCH and updates state; departed accounts have no restoration action.
- UI: manager departure first opened succession; replacement used versions 0/0, subsequent independently confirmed departure used returned version 1. Manager job adjustment also opened succession and continued to a separate job form with the former manager as employee and the new supervisor selected.
- UI: selected repository logo PNG, decoded/downscaled to 512px, saved via PUT; after full reload both header/account images loaded (natural size 512x512) via authenticated GET.

## Contract and deployment limits
- Backend has no explicit manual disable-reason field. Web shows departure-related reason when known and `未记录` otherwise, never inventing a temporary-disable explanation. Capturing a specific manual reason requires a separately agreed backend contract.
- Restoration uses existing versioned user PATCH, preserves roles/profile/identity and never changes the password. Unknown employment state and departed users are ineligible. Actual server authorization and responsibility checks remain authoritative.
- Avatar uses existing `/api/v1/users/{id}/avatar` GET/PUT; backend data is persistent. The isolated UI test proves the client flow, not the production server deployment. Cloud end-to-end verification still requires a valid authorized session and the deployed avatar endpoints.
- No commit, PR update or cloud deployment performed for this change. Earlier uncommitted log-export fix and three pre-existing untracked business module files were preserved.

## Selected account delivery refinement
- Full suite: 153 passed outside the sandbox (local HTTP proxy tests require loopback access). Build and lint passed, with existing effect/large-chunk warnings; strict OpenSpec validation and diff whitespace checks passed.
- Available credentials appear by default, with search, explicit checkbox selection, current-result select-all, masked passwords, collapsed unavailable accounts and a truthful empty state. Removed the empty delivery textarea.
- Single/batch creation results export only the confirmed created IDs. Copy/export rereads the backend and rejects the entire selection if any selected credential is unavailable; tests verify fresh values, no unrelated accounts and actual workbook rows.
- Real logged-in localhost:5173 page: 0 available accounts, 33 unavailable accounts collapsed; no cloud mutation or real credential export performed. Fixed form-style collisions after visual inspection.
- Final production bundle tested against isolated in-memory localhost:5180 fixture: two available accounts, one unavailable; searching successor@local and selecting all yielded exactly one selected account; export completed with `已导出 1 人`, controls unlocked. An earlier test was interrupted by rebuilding assets during the fixture session; restarted against the final build and repeated successfully.
- First-login mandatory password change still requires backend and Android support. Export/copy does not claim the employee has received the account. No automatic password resets, messaging, commit or deployment performed.
