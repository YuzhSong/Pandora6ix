## Why
PR #30 changes task entry, permission-driven navigation and calendar interactions without an OpenSpec record. Its self-plus-subordinates selection currently fails backend authorization, and the rebase must preserve #28's journal screens and reminder permission behavior.

## What Changes
- Record and validate capability-gated entries, submitted-review defaults, supervisor display, optional tags, calendar navigation and settings/detail polish in PR #30.
- Infer personal tasks from self-only selection and delegated tasks from selection containing other recipients. Permit an authorized manager to participate alongside authorized subordinates, with unchanged tenant, status and supervisor approval checks.
- Preserve #28's journal detail route, calendar picker, notification permission marker and removed home settings shortcut.
- Add regression coverage for mixed recipients and run independent module checks before sequential merges.

## Capabilities
### New Capabilities
- `android-capability-task-entry`: Capability-driven Android task entry and matching backend recipient authorization.
### Modified Capabilities
None.

## Impact
Android UI/models, existing backend task/review authorization and tests. No new APIs, tables, providers or credentials.
