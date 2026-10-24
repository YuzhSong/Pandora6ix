# Validation — 2026-10-23

## 2026-10-24 refinement
- Founder/admin supervisor controls and current profile rows are hidden; submitted supervisor is normalized to null. Admin job adjustment action is removed, with an additional submit guard. Batch founder role changes/imports clear hidden supervisor values. Supervisor policy tests cover administrator accounts with combined roles.
- Removed implementation-oriented UI explanation paragraphs, including current-name resolution notes, while retaining validation, failure feedback and consequential-action reminders. Rendering limitations remain in developer documentation.
- Personnel cards/details and organization members now use a shared red status badge immediately beside the person's name. DEPARTED takes precedence over DISABLED to avoid redundant labels. Browser checked a real disabled account: badge inside the name row, computed red `rgb(180, 35, 50)`. Production build passed after the update.
- Log positions now use the snapshot's canonical roles once, omitting duplicate free-text job titles from the comparison. Original job-title text remains available in expandable raw metadata; missing historical roles are explicitly marked rather than inferred from current permissions.
- Organization references show the full ancestor path; personnel references show organization path, name and login. Cycles and unavailable ancestors are handled without looping. Five presentation tests pass, including department/team/person disambiguation and historical role changes.
- Organization membership helper excludes departed members and retains disabled in-service members without deleting history.
- Snapshot presentation uses Chinese fields/states, current organization/person names, raw identity metadata and unchanged raw comparison equality.
- Placeholder snapshot version 0 is hidden when neither snapshot reports a positive version; the current record at placeholder zero is not shown as its own history. Other unnumbered historical records remain readable by time.
- Modal width capped at 1120px and 90dvh with compact 9px table padding. Browser verified departure snapshot Chinese display, resolved names, and absence of placeholder version labels on the real page. Earlier wheel fix retained.
- Focused suite: 17 tests passed; additional version-placeholder case passed in the four-test presentation suite. Production build and lint passed (existing warnings remain); strict OpenSpec validation passed.
- Backend coordination is documented in `web/BACKEND_HANDOFF_20261024.md`; authentication contract remains unchanged pending account-namespace decision.

- Web production build passed. Lint completed with existing warnings and no errors.
- Web suite: 129/131 passed in sandbox; both local proxy failures passed when rerun outside sandbox. New feedback test passed separately.
- Backend: `BusinessApiIntegrationTests#employeeCanTransferAcrossTeamsAndBack+jobChangeKeepsAccountAndProfileAndValidatesReporting`: 2 tests, zero failures/errors. A preloaded Byte Buddy agent avoids sandbox Windows attach-pipe failures.
- Regression transfers an EMPLOYEE between two teams and changes supervisor, transfers back, preserves login/profile/password hash, revokes the prior session and writes two history records.
- OpenSpec strict validation and git diff whitespace check passed.
- Read-only production `/deployment.json`: dev SHA `1ae38f7b6c2844180040234fefff7fb2f54851b6`, run 16 attempt 1. PersonnelLifecycleService and migrations have no diff against local HEAD for this deployed revision.

## Outstanding production diagnosis

Screenshot corresponds to backend `DATA_CONFLICT` (a caught DataIntegrityViolationException), not a known field validation or hierarchy policy error. The exact database constraint is not exposed by the API. Current local migrations and valid team/supervisor records do not reproduce it. Do not claim the production problem is fixed or bypass constraints with a different write endpoint.

Need the server-side exception around the failed job-change request, with secrets and personal identifiers redacted, or an existing authorized server connection to inspect it. No production personnel records were changed, and no release was pushed/deployed.
