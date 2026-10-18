# Design

## Context
Directory creation, editing and user assignment already serialize on the enterprise row. Organization parent and user organization references have foreign keys. Web holds a server snapshot and renders personnel filters from its units.

## Goals / Non-Goals
Goals: delete unused leaves with authorization, optimistic version protection and explicit user confirmation.
Non-goals: cascading removal, automatic personnel transfer, audit-storage decisions, root/company removal.

## Decisions
- Use DELETE with validated `{expectedVersion}` and 204; this preserves existing versioned-command conventions without a new action endpoint.
- Lock the enterprise in the existing transaction before checking tenant, version, root, children and all assigned users. Retain foreign keys as final protection. Return ORG_ROOT_DELETE_FORBIDDEN, ORG_HAS_CHILDREN or ORG_HAS_MEMBERS with 409.
- Update the Web snapshot only after 204, invalidate in-flight older reads, select the parent, and reuse the snapshot for all filters. Do not require a second read to acknowledge a successful delete: a failed follow-up read must not imply that deletion failed.
- Show blockers before confirmation; server checks remain authoritative for concurrent changes. The modal cannot close while submitting; navigation and refresh follow the existing edit-dialog guard.

## Risks / Trade-offs
- Network failure after commit → retain the visible unit and tell the user to refresh before retrying; repeated delete returns 404, never an invented success.
- Older deployed backend lacks endpoint → display an explicit deployment message; local integration uses an isolated updated backend.

## Migration Plan
No database migration. Deploy backend before Web; rolling back Web removes the action but does not recreate deleted empty units. Tests create disposable organizations only.
