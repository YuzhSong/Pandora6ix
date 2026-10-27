# Design

## Context

The Web uses a shared authenticated envelope client, React dialogs, themed Select popovers and lazy ExcelJS. No operation-log backend exists. Only Web implementation is authorized; the contract remains a proposal for backend agreement.

## Goals / Non-Goals

Goals: implement a production-oriented read-only client that can connect without redesign when the backend implements the documented contract. Keep snapshots separate from mutable live data.

Non-goals: backend capture/tables, fake runtime data, retention jobs, cloud deployment, task restoration or editing. No passwords/tokens or complete government identity numbers in history.

## Decisions

- Authenticated GET list/detail APIs under /admin/operation-logs. Backend enforces company scope and ADMIN authority. Shared transport clears sessions on 401. Page-local state and abort controllers avoid cross-session data persistence.
- Cursor pages include a server snapshot token and total count. Pagination and complete Excel exports reuse the token so newly appended events do not cause gaps or duplicates. Export summaries only; complete business snapshots are read on demand in details, avoiding unnecessary bulk disclosure.
- Snapshots contain ordered labelled display fields with stable keys and string/null values, not raw database rows. Backend resolves historical person/org labels and enum values. Web compares fields by key and suppresses sensitive keys defensively; backend exclusion is mandatory.
- Detail opens directly on two complete before/after versions. Changed text is highlighted in old/new colors without a changed-only/all-fields toggle. Secondary metadata is collapsed. A separate history tab lists object versions; selecting an event opens its complete snapshot comparison. Deleted objects need no live business lookup.
- The summary list uses compact actor-plus-operation rows with distinct module badge colors. All filters are directly visible; the search field is short and controls share one desktop row when space permits, wrapping on narrow screens. This follows the customer's latest preference, replacing expandable advanced filters. Pagination controls override legacy fixed icon-button widths.
- Abort superseded list/detail requests and guard export completion after unmount. Real API failures never trigger fixtures. The customer's explicit local-preview request permits sample logs inside the full application only behind DEV, loopback hostname and an explicit previewLogs=1 query flag, with a small preview badge. The fixture module is dynamically imported only in development and must be absent from production output.
- Fix list page size at 20 without a selector. Use shared typography for pagination. Replace native date widgets with a themed range popover and keyboard-operable calendar grid; apply draft ranges only on confirmation.
- Date ranges and the month heading accept direct text entry with real-date and range validation; the reset action reads All dates. Operation choices use explicit module-specific phrases and incompatible choices reset when changing modules. Badge colors distinguish work logs (purple) from success (green), and login (warm brown) from tasks (blue).
- Existing screenshot is information architecture only. Keep current cream/yellow theme rather than copy its black background.

## Risks / Trade-offs

- Backend contract not implemented → honest load failure and a coordination checklist; live recording is not claimed complete.
- Full snapshots contain business history → customer approval supersedes earlier metadata-only restriction, but grants no mutation capability and still excludes secrets and identity values.
- Client export loads all summaries into memory → document server export jobs for future large datasets; do not silently cap exports or return partial workbooks.
- Retention and technical logging are backend decisions → document suggested behavior without implementing unapproved infrastructure.
