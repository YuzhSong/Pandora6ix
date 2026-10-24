# Validation 2026-10-20

## Scope

Web implementation only, as explicitly selected by the user. No backend changes, database writes, Git push, PR submission or cloud deployment. The contract in web/SYSTEM_OPERATION_LOGS.md is proposed and requires backend agreement and implementation before live recording is possible.

## Automated checks

- `node node_modules/typescript/bin/tsc -b`: passed.
- `node node_modules/vite/bin/vite.js build`: passed; existing lazy ExcelJS chunk exceeds 500 KB warning.
- `node --test tests/*.test.ts`: 120 passed, 0 failed. This working directory includes 8 tests from the user's pre-existing untracked businessReader draft; 112 tests belong to tracked source plus this change. Initial sandbox run failed two local network proxy tests; rerun outside the sandbox passed both and the full suite.
- New `operationHistory.test.ts`: 11 passed. Covers query encoding and Beijing date bounds, missing/invalid endpoints, complete snapshot pagination, loop/duplicate/partial result rejection, cancellation, failure-version rejection, field comparison and secret suppression, deleted-object snapshots and literal human-readable XLSX values.
- Full lint: no errors. Existing files have 5 effect-state warnings including 2 in the unrelated untracked draft. New SystemLogsPage has 4 effect-state warnings for synchronizing query, refresh, export and detail request state; these do not fail lint.
- `openspec validate system-operation-history --strict`: passed.
- `git diff --check`: passed, apart from informational LF-to-CRLF notices.

## Browser checks

Used the actual React component in an isolated ignored `.integration/system-logs-qa.html` entry with explicitly controlled test responses, served only on 127.0.0.1:5176. The production App never imports that entry, intercepts no requests and contains no sample audit events.

- Desktop summary table, module/action/source/result controls, date inputs, loading, empty and missing-endpoint feedback inspected.
- Modification detail: changed-only view shows 2 changed fields; full-field view includes unchanged fields. Multiline business text is rendered literally.
- Historical version picker fetches an earlier creation snapshot and offers return to the original event.
- Deleted work-log detail retains its pre-deletion content without a live-object lookup.
- Escape closes detail and returns focus to the invoking button. Root and body overflow both become hidden while the dialog is open and restore after closure.
- 390 x 844 viewport: page width stays within viewport; tables scroll independently. Temporary viewport override reset after testing.
- Slow old keyword request resolves after newer search: current filtered result remains intact. Refresh retains the search text and matching result.
- Cursor page 2 displays the remaining rows. Export from page 2 downloads all 4 matching rows, verified by reading the generated real XLSX with ExcelJS: 4 data rows, 9 readable columns.
- Local screenshot: web/.integration/system-log-history-20261020.png (test data, not live audit evidence).

## Follow-up

Backend must implement authoritative collection, company-scoped list/detail APIs, retained full snapshots, readable labels, stable export pagination, sensitive-field exclusion and authorization. Real database history, cross-company isolation and Web/Android capture cannot be claimed validated until that backend is available. Non-author review and approved dev deployment remain separate workflow steps.

## Customer refinement validation

The customer's later instruction supersedes the changed-only/all-field toggle described in the earlier browser checks. Details now show both complete versions immediately with changed Unicode text highlighted in separate old/new colors. All unchanged fields remain visible. Secondary metadata is collapsed; historical versions have a separate tab and a readable version list. Advanced filters are collapsed, summaries compacted and pagination widths repaired.

- TypeScript check and Vite production build passed after refinement.
- 15 affected tests passed (11 operation-history plus 4 text-difference tests), including date edits, multiline insertion/deletion, multiple changes, Unicode emoji, literal markup and bounded comparison of long unrelated content. Long complex edits may highlight a larger differing span rather than compute a minimal expensive edit sequence; both full texts are preserved.
- Targeted lint: 0 errors, same 4 request/refresh effect-state warnings. Strict OpenSpec validation and whitespace check passed.
- Browser verified complete default versions, precise colored date and appended text, no visibility-toggle buttons, separate history list, viewing version 1 and return to the current operation. Narrow viewport did not increase document width.
- Complete local application started with the existing real backend JAR and a fresh isolated local database using `node scripts/integrationBackend.ts --preview`; its existing integration checks passed. Logged in through the normal UI. Current complete app URL: http://127.0.0.1:57821/#system-logs, with normal navigation to organizations, personnel and company items.
- The complete application reports the real missing operation-history endpoint, rather than substituting QA records. No backend source or cloud data was modified. The separate QA entry remains solely an ignored development test fixture, and is no longer the user-facing deliverable.
- Screenshots: `.integration/system-log-two-versions-20261020.png` (test records), `.integration/full-workspace-system-logs-20261020.png` (complete real local app, backend endpoint pending).

## Explicit integrated sample preview and controls

The customer subsequently authorized sample logs in the full page. This supersedes the earlier user-facing preview boundary: `http://127.0.0.1:57821/?previewLogs=1#system-logs` now shows a labelled development-only sample dataset. Only log queries are supplied by the sample adapter; all other modules still use the local backend. No API error automatically enables this mode. The application requires DEV, an explicit query flag and a loopback hostname. Vite production output was searched for sample record IDs, sample content and module names; none were present.

- Fixed 20-row pages, no page-size selector (browser DOM count: 0). Tested 27 records as 20 on page 1 and 7 on page 2; fixed pager uses the shared button family, weight and text size without legacy pagination styles.
- Replaced native date inputs with themed popover calendar, date-range selection, Today/7 days/Month presets, draft confirmation and clear. Tested two clicks on the same day, confirmed 5 matching records, arrow-key focus from October 20 to 21 and Escape closure.
- At 390px viewport the document width remained 375px and the calendar width was 340px; popup scroll keeps footer accessible. Restored the normal viewport after inspection.
- Inspected full before/after detail from a sample task inside the full application, including highlighted added text and deadline characters.
- TypeScript and production build passed; 19 affected tests passed (calendar/sample adapter 4, text difference 4, operation history 11). Targeted lint has no errors and the same 4 effect-state warnings; strict OpenSpec validation passed.
- Screenshots: `.integration/full-log-preview-20261020.png` and `.integration/full-log-preview-detail-20261020.png`. Final page is filtered to October 20 (5 records); resetting filters exposes all 27.

## Module badges and directly visible filters

Following the customer's latest instruction, removed More filters and placed all four selects directly beside a shorter search field and date-range control. Complete-app browser inspection confirmed controls remain on one desktop row (top positions within 1px, except the vertically centered reset text). All four selects remain visible on narrow screens with automatic wrapping; at 390px viewport the document remained 375px wide. Tested operation filtering to two modification records and resetting/date filtering back to five records.

Module badges now use distinct blue/task, green/work-log, amber/organization, purple/personnel, rose/account and slate/login colors, with dark-mode variants and readable textual labels. Browser confirmed task, work-log and login colors differ in the live sample dataset. TypeScript check, Vite build, strict OpenSpec validation and whitespace check passed. No new behavior-mirroring unit tests were added for these reversible visual changes.

Screenshot: `.integration/log-module-colors-filters-20261020.png`.

## Typed dates, clearer choices and revised colors

- The latest palette uses blue tasks, purple work logs, warm brown login, cyan personnel, amber organizations and rose accounts; success remains green. This supersedes the earlier green-work-log/slate-login palette.
- Start/end dates are editable text inputs. YYYY-MM-DD, slash and dot separators normalize on blur; impossible dates and reversed ranges prevent applying. Month text accepts e.g. 2024-02 or 2024年2月. Date reset now reads 全部日期 and only clears the time filter.
- Browser verified typing 2024-02 jumps directly to the leap-year month, 2026-02-30 reports an error, reversed ranges disable confirmation, and typing 2026/10/20 normalizes and filters correctly.
- Operation options now identify the object/action (e.g. 停用账号, 通过任务审核) and depend on the module. The wider menu preserves readable labels. Browser verified switching from account login to work logs clears the incompatible action, then shows only 新增工作日报/修改工作日报/删除工作日报.
- TypeScript, production build, targeted lint (no warnings/errors in the directly changed helper/control files), strict OpenSpec and whitespace checks passed. Seven calendar/preview/action-choice tests passed, including invalid dates and contextual options. The existing SystemLogsPage effect-state warnings are unchanged.
- Screenshots: `.integration/log-editable-dates-20261020.png`, `.integration/log-contextual-actions-20261020.png`, `.integration/log-final-colors-20261020.png`.

## PR submission checks

Final PR-scope Web suite passed 123 tests with 0 failures, excluding the unrelated untracked businessReader draft and its 8 tests. Final TypeScript and production build passed. Full workspace lint reported no errors (7 warnings in submitted files and 2 additional warnings in the excluded draft). Strict OpenSpec and staged whitespace checks passed; final production output contained none of the sample log identifiers, content or module names. This batch changes only Web, tests and the associated specification/coordination documents. Backend implementation and non-author review remain pending.
