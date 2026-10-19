## Diagnosis and scope

Latest dev at synchronization/submission: `4c4b0328b91eff3ad2af266f08e55ef9e04874a8`. Fast-forwarded the existing backend-syz branch from `39dc7c2` before implementation; no user files in the primary worktree were changed.

The failure had three causes: UserPatch lacked loginName and its SQL did not update login_name; AdminEditor disabled the existing-account input; the HTTP repository explicitly rejected changed names. The user confirmed including Web with the backend fix. Unchanged historical null profiles also had native required flags that blocked unrelated edits; creation/profile-change validation remains enforced.

## Automated verification

| Check | Result |
| --- | --- |
| `backend/mvnw.cmd -f backend/pom.xml -B -ntp package` with Java 21 | 66 tests, zero failures/errors/skips; final packaged jar produced |
| Seven new backend HTTP tests | Actual rename/login; unchanged hash, profile, roles, task/journal references and existing sessions; nullable legacy updates; invalid/duplicate/stale input; tenant/ADMIN boundaries; admin self-rename/last-admin protection; concurrent version and duplicate-name races |
| `npm test` | All 70 Web tests passed |
| `npm run lint` | Passed; two pre-existing App.tsx set-state-in-effect warnings, no new warning or error |
| `npm run build` | Passed TypeScript and Vite production build |
| `npm run test:integration` with PANDORA_INTEGRATION_JAR pointing to the new artifact | Passed against a separate Spring process, fresh file H2 and Vite; includes rename, old-name rejection, same-password new-name login, duplicate rejection and reload persistence |
| `python backend/scripts/generate-openapi.py --check` | 46 existing operations verified; optional nullable loginName added to UserPatch |
| `openspec validate admin-edit-user-login-name --strict` and `git diff --check` | Passed |

## Browser QA

Target flow: local administrator login → search employee → edit login name → reject an occupied name while keeping the draft → save a free name → reload and authenticate with the same password.

Environment: fresh, disposable company and synthetic people in a loopback-only backend file database. Final run used `http://127.0.0.1:55574`, Playwright 1.62.1, existing Chromium revision 1223, desktop 1365×900 and narrow 390×844 viewports. Browser plugin/skill was not available, so regular Playwright was used; the installed browser executable was selected explicitly instead of downloading a missing newer revision. No browser dependencies were added to the project.

| Browser check | Result |
| --- | --- |
| Page identity / meaningful render | Correct localhost application and people page rendered |
| Framework overlay / console | No overlay, page exception or unexpected console error; the intentionally induced HTTP 409 was accounted for |
| Duplicate interaction | Chinese LOGIN_NAME_TAKEN message visible, modal remains open, entered name retained |
| Save and reload | Both desktop and narrow-screen UI submitted successful PATCH requests and displayed the persisted new login name after reload |
| Authentication / session | Old employee login returned 401; new name with original password succeeded as the same ID; self-renamed ADMIN retained its existing session |
| Historical profile | ADMIN with null gender/identity/phone could rename without forced unrelated completion |
| Whitespace handling | The editor rejected a padded login name before sending a PATCH; backend also covers blank, padded and Unicode whitespace names |
| Visual evidence | Desktop duplicate-name and saved-state screenshots plus narrow editor and saved-state screenshots inspected/captured |

QA script, synthetic runtime data, `verification.json` and screenshots remain outside Git in `%LOCALAPPDATA%/Pandora/login-name-qa/`: `duplicate-name.png`, `renamed-employee.png`, `mobile-editor.png`, `mobile-saved.png`. Backend/browser processes were stopped after each run. No cloud employee login names, passwords or business data were changed.

## Delivery boundary

Submitted [PR #19](https://github.com/YuzhSong/Pandora6ix/pull/19), backend-syz → dev, for non-author review. No automatic merge, deployment or OpenSpec archive before merge. This PR permits manual administrator-maintained names; it does not generate pinyin or bulk-rename existing cloud accounts.

## Post-merge integration review (2026-10-18)

The user subsequently requested review and merge of PRs 17, 18 and 19 into dev and explicitly excluded PR 20. PR 19 merged as `862c068e14990eaac93b14765fcf701dc69bd29e`; reviewed PR 17 merged as `5dde68f43a5771ea329868d8497f514fe84ca104`.

Combined PR 17/18/19 code passed 72 backend tests and packaging, 70 Web tests, Web lint/build, 49-operation OpenAPI drift verification, and a fresh file-H2 Spring/Vite/Web integration run including rename persistence and old/new login behavior. PR 17 Android passed 30 unit tests and debug APK assembly. Deployment receiver tests passed eight applicable Windows cases with three Linux-only cases skipped locally; Linux CI executes them during deployment.

`openspec archive admin-edit-user-login-name --yes` synchronized both directory requirements and preserved all eight completed tasks. All 18 main specifications pass strict validation. The archive and cross-day automated-check completion notes are carried by PR 18; no runtime code is changed in that follow-up. PR 17's physical-device acceptance and future notification-API consumption remain pending in their active changes.

Existing checks report two App.tsx lint warnings and a high-severity source-map-js 1.2.1 advisory in the Vite/PostCSS development dependency chain; these PRs do not modify dependency manifests or locks. No dependency upgrade is included in this integration.
