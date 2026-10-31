## Why

The summary currently expands its task list and direct reports inline, and its theme-tinted surfaces do not clearly separate the three functions. The user clarified that this is an overview with bounded previews, not a full task center.

## What Changes

- Give each of the three overview panels a single white Material surface, subtle existing-theme border and spacing.
- Keep the three task filters inside the task panel, showing at most three matching tasks.
- Add one task-only destination reached through “查看全部任务”, retaining the selected scope and existing pagination/details.
- Limit direct reports to four preview people and journals to three entries; full organization and journal browsing remain behind “查看全部”.
- Apply visible panel/row boundaries throughout this flow, including separate personal-information, responsible-task and recent-journal panels in member detail. Member overview lists also use three-item previews and filtered full-list destinations.

## Capabilities

### New Capabilities
- `summary-panel-previews`: Bounded, visually separated summary previews with in-panel task filtering and full-list navigation.

### Modified Capabilities
None. The original task-summary capability is still an unarchived change; this follow-up refines its overview presentation.

## Impact

Android TaskSummaryFlow presentation/navigation and authorized member-contact loading, plus OpenSpec artifacts. Add `GET /users/{id}/contact` for an active member in the caller's existing management scope, returning only id and phoneNumber from app_user. Preserve the candidate-list privacy contract and all existing APIs. No database schema, statistics, fake data, navigation-tab changes or edits to unrelated team behavior.

## Confirmed follow-up

- Continue all current work on the user-named `Android-syz` branch.
- Reduce task-row padding, line heights and chip spacing while preserving readable fields and item boundaries.
- Remove normal-state explanatory captions below task filters and organization headings.
- Replace the member phone placeholder with authorized real contact data; distinguish missing numbers from read failures and allow retry.
