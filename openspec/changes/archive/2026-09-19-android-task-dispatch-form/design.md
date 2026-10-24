## Context

The Logs screen already owns the dispatch and new-log overlays. The change should remain local to those composables and reuse existing mock hierarchy data.

## Decisions

- Give the dispatch card a larger fraction of the available height so its title, priority, notes, and recipient controls remain visible together.
- Render priority as five compact stars; selected stars use a muted dark gray and unselected stars use a light gray.
- Keep recipients in a vertically scrollable column with at least three visible rows at the default modal size. Each row toggles a checkbox.
- Apply a shared `OutlinedTextFieldDefaults.colors` palette to all editable fields in both overlays, using gray borders, labels, and cursor rather than coral.
- Keep the submit action disabled until a title and at least one recipient are selected.

## UI Flow

1. Manager opens 任务派发.
2. Manager enters task name and notes, chooses 1–5 stars, and selects one or more managed members.
3. Manager submits the task for upper-level review; this remains mock-only in the prototype.
4. Manager opens today’s log and sees the same neutral input treatment.

## Non-goals

- No persistence of newly dispatched tasks.
- No real approval API or role switching backend.
