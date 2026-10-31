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
- Android assemble and 104 existing unit tests passed for the initial layout/recipient edits. The subsequent pending-edit indicator is included in the final passing build recorded below.
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
- Latest APK installed. Initial traversal failed because the UI harness could reuse a stale dump; this was subsequently corrected. The rendered per-recipient acceptance buttons and real names were inspected in inline-execution-names-final.png. No real production acceptance/rejection was performed; lifecycle mutations were tested in the isolated integration database.

## Item 4 — rejected publication resubmission

- Original submitters see “重新编辑并发布” below a rejected CREATE result. EDIT rejections and other users do not get this action. Snapshot parsing validates the original immutable command and prefills dates/times, content, recipients, priority, currently available tags (primary first), recurrence when present and publication visibility.
- The form uses the new-task creation path with a new idempotency key and a review-specific resubmit draft file. It neither PATCHes the old task nor overwrites the regular task draft. Missing/malformed snapshots show an actionable error and review reload. Current backend recipient/tag and deadline checks still apply at publication.
- Two Android tests cover snapshot field preservation, primary-tag order and rejection of another user's/non-rejected/EDIT/missing-snapshot input. Final Android assemble plus all 106 tests passed. Log: C:/Users/34755/AppData/Local/Temp/pandora-resubmit-android.log.
- Backend rejectedPublicationCanBeCopiedToNewTaskWithoutChangingOldReview passed: distinct task/review IDs, duplicate retry returning the same new task, byte-equivalent old review data, preserved old rejected task/result, denied unrelated review read and successful new approval. Log: C:/Users/34755/AppData/Local/Temp/pandora-resubmit-backend.log.
- The rendered rejected-result action and prefilled new-task form were subsequently inspected (rejected-result-resubmit-followup.png and rejected-prefilled-form-followup.png). No production resubmission was performed. Historical snapshots without a recurrence rule can only prefill their recorded occurrence content; no rule is guessed from a display label.

## Item 5 — profile action ordering

- ProfileScreen now composes personal information, approval entry, the settings group (including the existing authorized tag-management action), then organization information. Removed both About Pandora title and generated explanatory paragraph. Existing permissions, badges and callbacks are unchanged.
- Source ordering and whitespace inspected; Android assembleDebug and all 106 unit tests passed. Log: C:/Users/34755/AppData/Local/Temp/pandora-profile-order.log. Strict OpenSpec validation passed. No new mirror test added for this pure layout move. Rendered ordering was subsequently checked in profile-order-verified.png.

## Item 6 — shared persistent avatars

- Confirmed the existing V7 avatar_image column and existing authenticated upload/read API; no new table or duplicate avatar column. Camera completion previously only saved a local path; it now uploads too. Gallery and camera failures report unsynchronized state, with cancellation propagated. Local preferences are keyed to the actual account.
- Shared UserRepository caches positive images for 30 seconds and absence for 10 seconds, with a 128-entry bound and expired-entry cleanup. Upload serializes with reads for the same user, replaces cache immediately and publishes a revision. RemoteAvatar observes revisions and refreshes visible images every 15 seconds, retaining the displayed image during transient failures.
- Profile reads the real server avatar with existing local/palette fallback. Selecting a default palette avatar uploads the rendered selection too, avoiding a remote old photograph overriding the chosen default. Journal cards/details and task execution rows now use RemoteAvatar; summary/member/approval components already used it and inherit refresh behavior.
- AvatarCacheTest verifies absence expiry, upload replacement, positive expiry and independent repository/cache instances. Android final assemble and all 108 tests passed. Log: C:/Users/34755/AppData/Local/Temp/pandora-avatar-android.log.
- Enhanced avatarUploadsAndServesRealImageBytes passed with a complete decodable 1x1 PNG (also independently decoded locally): database column equality, identical manager read, fresh-session read, denied other-user write, missing-image 404 and invalid-URL rejection. Log: C:/Users/34755/AppData/Local/Temp/pandora-avatar-backend.log.
- Server persistence/cross-user reads are dynamically verified in the isolated integration database. Android bitmap display paths are source-reviewed and compiled; camera/gallery interaction and cross-device rendered refresh remain for the final UI audit. No production avatar was overwritten for testing.

## Item 7 — persisted per-account theme

- Root cause: the selected key was only rememberSaveable("warm"), with no persistent store. ThemePreferences now reads/writes account-specific keys in private pandora_appearance SharedPreferences; account changes reinitialize state from that account's saved value. Unsupported keys and signed-out state fall back to warm.
- Two tests verify restoration through a recreated preferences adapter, independent accounts, signed-out default, unsupported stored values and rejected invalid writes. Android final assemble and all 110 tests passed. Log: C:/Users/34755/AppData/Local/Temp/pandora-theme-android.log.
- With explicit user authorization to use/restart the emulator, selected mint through actual settings UI, force-stopped the app and relaunched it. Confirmed rendered mint home screen and persisted per-account mint value in the actual private XML. Restored the original warm selection through UI afterward. Actual account switching was unit-tested, not exercised by logging out the user's session.
- Screenshots: C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/theme-mint-before-restart.png and theme-mint-after-restart.png. The latter was visually inspected. The initially misnamed profile-actions-final.png is a summary screenshot and is not profile-order evidence; profile-order-verified.png was captured with profile order checked.

## Confirmed UI follow-up

- Root cause of ID suffixes in the editor: only ID values were prefilled and candidate names loaded only on an @ search. Assignment responses now include recipientName through an enterprise-scoped join inside existing task authorization. Editor and execution panel seed server names and resolve missing legacy names through authorized bounded candidate pages; the deployed older API still works. Confirmed real names 宋雨铮 and 熊骏逸 in both editor chips and execution rows without opening @.
- Material surface-container roles are white with transparent tint; explicit task/progress/calendar sheets and custom journal/personal-item overlays also use white. Tag and time dialogs were visually inspected with white backgrounds. Date/exit dialogs inherit the same shared roles; no individual production date or task data was changed.
- Both primary and secondary editor tags are filled with the same slight strengthening derived from their stored hue; picker swatches are larger and bordered. Task/review detail chips preserve exact stored color, sort the primary first, and use a 1dp Material outlineVariant neutral border. No stored tag hex values were overwritten.
- Removed the description dialog and its launcher. State controls, authorized acceptance controls and all available textual progress history are inline within the execution panel. Record entries have individual boundaries and timestamps. Existing permission, version, retry and acceptance callbacks remain.
- Approval list, submitted queue and detail now use the profile's themed background/card colors with a neutral border. Semantic status badges retain their status colors. Inspected actual list and detail screenshots.
- Android final assembleDebug and all 110 tests passed: pandora-ui-followup-android.log. Complete backend package passed with all 148 tests: pandora-final-backend.log. The initial full run exposed a migration-count assertion expecting 3 rather than the current 5; updated it and added assertions that legacy tasks remain private and old assignments remain current. Focused name/privacy/migration checks also passed (3 tests): pandora-ui-followup-backend.log. Web dependency installation from lockfile, 153 tests and production build passed; OpenAPI (69 operations), strict OpenSpec and whitespace checks passed.
- Harness correction: startup uiautomator failures could leave an old XML file, explaining some earlier aborted traversals attributed to concurrent activity. The local helper now deletes its own old dump and retries until a fresh parseable tree is available. Earlier failed traversals are not counted as successful checks. Subsequent execution-name checks used fresh dumps.
- Valid local screenshots under C:/Users/34755/AppData/Local/Pandora/summary-panels-20261031/: recipient-names-followup.png, white-tag-picker-followup.png, white-time-picker-followup.png, inline-execution-names-final.png, review-center-followup.png, review-submitted-followup.png, review-detail-followup.png. Also verified the rejected-result resubmission button opens a prefilled new-task form and closed without publishing: rejected-result-resubmit-followup.png and rejected-prefilled-form-followup.png.
- Latest APK installed for testing. Backend changes remain local and were not deployed to the cloud. No production task acceptance, edit or resubmission was executed.

## Final requirement-by-requirement audit — 2026-10-31

Previous goal turn produced the completed UI audit documentation and local commit; this continuation adds device-level avatar evidence and completes integration housekeeping.

| Requirement | Authoritative evidence | Result |
|---|---|---|
| Remove prior related self-created branches | Verified complete-history bundle with 12 refs, and git show-ref confirms all 12 task/integration refs absent | Done; other historical project worktrees preserved |
| Android-syz PR merged to dev | GitHub REST PR 46: closed, merged=true, merge SHA 47361b8c6351e5dba5fcbc8f1a98346c2a455d77; user authorized review exception | Done |
| backend-syz updated from remote dev | Fresh git fetch; origin/dev is the merge SHA above; merge-base --is-ancestor succeeds | Done; no new branch |
| 1. Company panel public scope | ed31828; V14/default-private, founder-only publication; companyPanelContainsOnlyExplicitPublicTasksAndDoesNotGrantActions | Done, isolated enterprise/role tests |
| 2. Full editable task information and approval | e4141e9; TaskPatch/TaskEntrySheet fields, current-member history, proposal-before-effective updates, single/future occurrence review tests, white information/execution panels | Done; recurrence-rule creation is not silently presented as an editable occurrence field |
| 3. Per-recipient completion review | 9cac0ad; five lifecycle tests, matching pending requests/version/actions; actual rendered acceptance/rejection controls inspected | Done |
| 4. New publication from rejected result | f290017; immutable snapshot prefill, distinct task/review IDs, retry idempotency and unchanged old result tested; rendered prefilled form inspected | Done |
| 5. Profile ordering and About removal | 315485e; ProfileScreen order and profile-order-verified.png | Done |
| 6. Persistent shared photos | 7277fbc; existing avatar_image persistence and manager/fresh-session byte reads tested; actual Android transport/decoder/Compose/cache refresh test added | Done; system camera/gallery picker itself is not automated |
| 7. Theme restoration | 23fbb0c; account-separated preferences tests, real force-stop/relaunch mint comparison, warm restored | Done |
| Separate local commit per numbered task before next | git log confirms the seven SHAs above in order | Done; no backend branch push or merge requested |
| Merge housekeeping | Real OpenSpec CLI archived the three PR46 changes and synced main specs; strict validation passes | Done, local archival commit |

### Device avatar regression

`AvatarDeviceTest.uploadRefreshAndFreshReaderRenderTheSharedPhoto` uses an isolated host fixture, actual Android JPEG compression and Bitmap decoding, and actual Compose pixel assertions. It verifies 512x320 scaling to 256x160; a separately authenticated reader; immediate upload revision/cache replacement; already-visible independent reader refresh after the 30-second positive-cache TTL; and bytes read by a new repository. It writes no production avatar, task, journal or credentials. Real backend storage/authorization remain independently verified by integration tests; the fixture does not purport to validate server authorization.

### Test-runner side effect and correction

The initial connectedDebugAndroidTest runner passed but its default UTP cleanup uninstalled the real debug app. UTP logs confirm uninstall_after_test=true. This cleared local session/preferences/drafts; there was no app-data backup to restore the original session. The app was reinstalled, and the prior known warm theme is the default. The user must log in again; local drafts are not claimed recovered. The user was informed as soon as the side effect was confirmed. Remote business data and avatars were never changed.

The retained regression is now wired exclusively to disposable avatarQa (application ID com.pandora6ix.app.avatarqa, label Pandora Avatar QA). This prevents UTP cleanup from removing the real app. Fixture sessions live in the test APK, are cleared in finally, and the temporary fixture service is stopped after testing. Run instructions are in android/test-tools/README.md.

### Delivery and validation boundary

Android assemble/unit tests: 110 passed; backend full package: 148 passed; web: 153 passed and production build passed (existing large-chunk advisory only). OpenAPI includes 69 operations. Final device-test outcome and package-preservation check are appended after the isolated run completes. All backend tests use isolated test storage. Camera/gallery chooser UI and the full global UI matrix are not claimed tested. The seven backend/Android functional commits and subsequent fixes remain local on backend-syz; production backend deployment is outside this request and has not occurred. New UI Design System is documentation only and has not been bulk applied.

Final isolated run: `:app:assembleDebug :app:testDebugUnitTest :app:connectedAvatarQaAndroidTest` passed (110 unit tests + 1 device regression, 0 failures). Log: C:/Users/34755/AppData/Local/Temp/pandora-avatar-device-test.log. After cleanup, `pm list packages pandora` contains only com.pandora6ix.app; real app installation remains, disposable QA and test APKs are removed. Real MainActivity was relaunched to the login screen. No claim is made that its cleared original session/drafts were restored. OpenAPI 69 operations, workflow and all three synced specs validate strictly.
