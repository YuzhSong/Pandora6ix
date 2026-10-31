# Verification

## Item 1 — explicit company-wide tasks

- V14 adds a non-null company_wide flag defaulting to false, preserving legacy privacy. Optional companyWide command property is backward compatible with old clients; only FOUNDER may publish delegated public tasks. Recurring occurrences retain the explicit flag.
- Company panel queries only approved public delegated tasks in the authenticated enterprise. Same-enterprise public detail reads do not grant assignment/history/action access to nonparticipants.
- Android founder delegated form exposes the switch. Its value participates in saved drafts and unsaved-change checks. TaskDto keeps positional constructor compatibility.
- Android assembleDebug and testDebugUnitTest passed after final client changes: 104 tests, zero failures. Log: C:/Users/34755/AppData/Local/Temp/pandora-company-tasks-android.log.
- Backend companyPanelContainsOnlyExplicitPublicTasksAndDoesNotGrantActions passed using real isolated enterprise initialization and login: identical public list for founder/manager/lead/employees, private task exclusion, read-only outsider, rejected publication exclusion, forbidden lower-role/public-personal publication, and cross-enterprise list/detail isolation. Log: C:/Users/34755/AppData/Local/Temp/pandora-company-tasks-backend.log.
- OpenAPI generator/check (69 operations), whitespace and strict OpenSpec checks passed. No cloud deployment or production test task creation performed. Publication-switch interaction has been source-reviewed and compiled, not yet exercised with a founder emulator account.
