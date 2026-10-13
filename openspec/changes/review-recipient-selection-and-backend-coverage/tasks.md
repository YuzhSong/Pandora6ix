## 1. Review and integrate

- [x] 1.1 Review the exact PR #8 and PR #9 heads and merge them locally while preserving existing dev fixes.
- [x] 1.2 Exercise @ recipient search, keyboard visibility, single-row toggling, multiple selection, removal, task tags and dirty-exit guards; repair confirmed defects.
- [x] 1.3 Run Android build/unit/lint checks, Web tests/build/lint and backend verify; strictly validate this OpenSpec change.

## 2. Audit backend coverage

- [x] 2.1 Inventory production routes, implementation layers, persistence/security dependencies and the two backend branch heads.
- [x] 2.2 Probe actual Spring health directly and through the Web development proxy, plus representative missing business endpoints.
- [x] 2.3 Write the requirements-linked missing-function inventory, separating Planned, Reserved, Conditional/Pending and client-only responsibilities.

## 3. Publish the reviewed result

- [ ] 3.1 Verify unchanged remote heads, push the integrated dev result and confirm both PRs are merged.

Validation on 2026-10-13: Web 35 tests/build/lint passed with two existing effect warnings; backend mvnw.cmd verify passed 9 tests; Android assembleDebug/testDebugUnitTest/lintDebug passed with 17 tests. Pixel_9a reproduced and verified fixes for candidate clipping, query visibility after selection and hidden-sheet continuation after Android back. Keyboard-open filtering, independent candidate scrolling to the sixth member, single-row toggling, multi-selection/chip removal, dirty-close/back continuation, empty-recipient validation and task save/detail with ordered tags passed. Actual Spring direct/proxied health returned 200 UP; 11 representative business routes returned JSON 404 with requestId. Source links and requirement references in the audit resolve. No GitHub check runs exist on the reviewed heads; physical-device and production acceptance were not performed.
