# Proposal

## Why

The customer approved database-backed operation history with full business snapshots and an expandable summary-first Web interface on 2026-10-20. The current system-log page is only a pending-decision placeholder.

## What Changes

- Replace the placeholder with searchable, filterable, paginated operation records and Excel export.
- Display readable before/after changes and complete historical snapshots in a detail dialog, including deleted objects.
- Keep the existing visual theme and shared controls, with no unsolicited explanatory prose.
- Define a proposed backend contract; per explicit user instruction this change implements Web only, without backend tables, collection, deployment or fabricated production records.

## Capabilities

### New Capabilities

- `web-operation-history`: Read-only operation audit browsing, historical snapshot comparison and filtered Excel export.

### Modified Capabilities

None. This adds the previously unimplemented history page without business mutation controls.

## Impact

Web components, API adapter, data formatting, tests and backend coordination document. Existing authentication and company isolation remain the responsibility of the actual backend. Full history viewing follows the latest customer approval and supersedes the attachment's earlier metadata-only description; passwords, tokens and sensitive identity values remain excluded. No new dependencies or backend modifications.
