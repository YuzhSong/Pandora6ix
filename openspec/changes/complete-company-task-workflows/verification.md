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

## Item 3 — completion acceptance

- Execution panel matches each assignment to its pending completion request and exposes acceptance/rejection only when that assignment has DECIDE_COMPLETION from the server. Uses request.version and separate stable idempotency keys for ACCEPT/REJECT, disables actions while running, reports failure and refreshes authoritative detail after success/conflict.
- Existing backend and repository APIs already implement the acceptance state machine; reused them without adding a business endpoint.
- Android assembleDebug and testDebugUnitTest passed (104 tests). Log: C:/Users/34755/AppData/Local/Temp/pandora-completion-android.log.
- Five integration lifecycle tests passed: realDelegatedApprovalAndPerRecipientAcceptanceDoNotCompleteOtherRecipients, delegatedCompletionRequiresCreatorAcceptancePerRecipient, rejectedAcceptanceReactivatesAssignmentWithoutRewritingHistory, progressAndAcceptanceNotifyCreatorAndRecipientWithoutDuplicates and textProgressAndWorkStatusPreserveSuperiorAcceptance. They verify authorized per-recipient acceptance, rejection/reactivation/history, unauthorized and repeated decisions, and notification deduplication. Log: C:/Users/34755/AppData/Local/Temp/pandora-completion-backend.log.
- Latest APK installed. Visual traversal was interrupted by concurrent editor activity on the shared emulator; stopped navigation rather than changing the user's form. Buttons are source-reviewed and compiled, but their rendered state has not yet been inspected. No real production acceptance/rejection was performed; lifecycle mutations were tested in the isolated integration database.

## Item 4 — rejected publication resubmission

- Original submitters see “重新编辑并发布” below a rejected CREATE result. EDIT rejections and other users do not get this action. Snapshot parsing validates the original immutable command and prefills dates/times, content, recipients, priority, currently available tags (primary first), recurrence when present and publication visibility.
- The form uses the new-task creation path with a new idempotency key and a review-specific resubmit draft file. It neither PATCHes the old task nor overwrites the regular task draft. Missing/malformed snapshots show an actionable error and review reload. Current backend recipient/tag and deadline checks still apply at publication.
- Two Android tests cover snapshot field preservation, primary-tag order and rejection of another user's/non-rejected/EDIT/missing-snapshot input. Final Android assemble plus all 106 tests passed. Log: C:/Users/34755/AppData/Local/Temp/pandora-resubmit-android.log.
- Backend rejectedPublicationCanBeCopiedToNewTaskWithoutChangingOldReview passed: distinct task/review IDs, duplicate retry returning the same new task, byte-equivalent old review data, preserved old rejected task/result, denied unrelated review read and successful new approval. Log: C:/Users/34755/AppData/Local/Temp/pandora-resubmit-backend.log.
- UI flow has been source-reviewed and compiled; shared emulator editor activity prevents safe visual traversal at this point. No production resubmission was performed. Historical snapshots without a recurrence rule can only prefill their recorded occurrence content; no rule is guessed from a display label.

## Item 5 — profile action ordering

- ProfileScreen now composes personal information, approval entry, the settings group (including the existing authorized tag-management action), then organization information. Removed both About Pandora title and generated explanatory paragraph. Existing permissions, badges and callbacks are unchanged.
- Source ordering and whitespace inspected; Android assembleDebug and all 106 unit tests passed. Log: C:/Users/34755/AppData/Local/Temp/pandora-profile-order.log. Strict OpenSpec validation passed. No new mirror test added for this pure layout move. Rendered verification remains in the final UI audit to avoid interrupting shared-emulator editing.
