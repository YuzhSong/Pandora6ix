## Why

Reviewing PR #6 and PR #7 for integration into dev revealed that personal-item candidates are excluded by editable text. Editing a selected item therefore exposes its original log again and permits duplicate selection. The new approved week-to-day navigation also needs to supersede the older general date-selection constraint.

## What Changes

- Retain source-log identity through personal-item edits, ordering, removal and reselection within the existing application session.
- Preserve edited text when moving an item back to candidates and selecting it again.
- Reconcile the integration calendar requirement with PR #7's explicit week-date-to-day behavior.
- Correct year-grid return-to-today positioning after scrolling into an adjacent year.
- Review the combined modules, execute available checks and document device-validation limits before pushing dev.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `personal-important-items`: Candidate exclusion and reselection follow stable source identity rather than edited text.
- `dev-integration-review`: Week date selection opens day mode; month and year date selection retain their mode.

## Impact

Android session state and personal-item helpers/tests, and OpenSpec integration requirements. No new dependency, backend endpoint, credential, database table or service. User authorization: review and merge the new branches into dev.
