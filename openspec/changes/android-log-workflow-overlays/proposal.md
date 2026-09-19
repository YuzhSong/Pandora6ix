## Why

The Logs page needs to communicate daily work, history, task management, and employee permissions for the stakeholder walkthrough. It also needs the same layered interaction language as the Home panels and a notification inbox that can slide over the current page.

## What Changes

- Split Work Logs into Today and History sections with date filtering and five-item pagination/expand behavior.
- Show five tasks initially with an expand action.
- Add permission-aware Task Dispatch and Pending Review entries for ordinary employees.
- Add compact overlay forms for creating today's log and showing permission results.
- Add a top-right mail button that reveals a half-width notification drawer from the right.

## Capabilities

### New Capabilities
- `log-workflow-overlays`: Structured logs, task list, permission states, modal forms, and notification drawer.

### Modified Capabilities

## Impact

- Android Compose Logs screen and mock interaction state only; no persistence or backend.
