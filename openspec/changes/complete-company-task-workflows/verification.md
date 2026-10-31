# Verification

## Item 1 — explicit company-wide tasks

- V14 adds a non-null company_wide flag defaulting to false, preserving legacy privacy. Optional companyWide command property is backward compatible with old clients; only FOUNDER may publish delegated public tasks. Recurring occurrences retain the explicit flag.
- Company panel queries only approved public delegated tasks in the authenticated enterprise. Same-enterprise public detail reads do not grant assignment/history/action access to nonparticipants.
- Android founder delegated form exposes the switch. Its value participates in saved drafts and unsaved-change checks. TaskDto keeps positional constructor compatibility.
- Android assembleDebug and testDebugUnitTest passed after final client changes: 104 tests, zero failures. Log: C:/Users/34755/AppData/Local/Temp/pandora-company-tasks-android.log.
- Backend companyPanelContainsOnlyExplicitPublicTasksAndDoesNotGrantActions passed using real isolated enterprise initialization and login: identical public list for founder/manager/lead/employees, private task exclusion, read-only outsider, rejected publication exclusion, forbidden lower-role/public-personal publication, and cross-enterprise list/detail isolation. Log: C:/Users/34755/AppData/Local/Temp/pandora-company-tasks-backend.log.
- OpenAPI generator/check (69 operations), whitespace and strict OpenSpec checks passed. No cloud deployment or production test task creation performed. Publication-switch interaction has been source-reviewed and compiled, not yet exercised with a founder emulator account.

## Item 2 — full task editing

- Details now use a white segmented information panel with top-right edit and a separate execution panel. Real current-user capabilities are passed through both calendar and summary entry points.
- TaskPatch supports proposed recipientIds. V15 current_member retains removed assignments and history; reads and direct assignment mutations reject retired membership. Approval validates proposed recipients against the creator's current management scope.
- Repeated range editing submits superior-reviewed proposals per occurrence instead of applying changes immediately. Current occurrence versions and date anchors remain checked.
- Android assemble and 104 existing unit tests passed for the initial layout/recipient edits. A subsequent small pending-edit status display still needs a final build.
- Two new integration flows passed, then seven focused new/existing approval regression tests passed with zero failures. Logs: C:/Users/34755/AppData/Local/Temp/pandora-edit-backend.log and pandora-edit-regressions.log.
- Title, description, time/date, priority, tags/primary tag, recipients, derived personal/delegated kind and founder publication visibility now submit through the complete form. Existing primary-tag ordering is retained. Repeated occurrences support single/future time/content changes through the established range form; creation recurrence-pattern controls remain creation-only, rather than silently accepting unapplied changes.
- Repeated-edit regression expectations now require superior approval. Sixteen edit-related integration tests passed, covering new participant/kind changes, publication denial, range approvals, unchanged effective content, rejection, stale versions and approval races. A further focused test confirms a retired member cannot directly PATCH its old assignment even with the current version. Logs: pandora-edit-regressions.log (16 passed) and pandora-edit-retired.log (1 passed).
- Final Android assemble/unit run passed (104 tests) after adding the pending-edit indicator, shared company-task detail routing and stable edit retry keys. Log: pandora-edit-android.log. Contracts and strict OpenSpec checks passed.
- Installed latest APK on emulator-5554. Inspected the two-panel detail using real existing tasks; an approved creator-owned task shows the top-right Edit action. Opening the editor exposes the prefilled form and recipients; closed without submitting any production mutation. Screenshots: C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/task-detail-two-panels-after.png and task-editor-after.png.
- End-to-end edit mutation/approval was verified against the isolated backend test database. The cloud backend has not been deployed; no production edit submission was attempted.
