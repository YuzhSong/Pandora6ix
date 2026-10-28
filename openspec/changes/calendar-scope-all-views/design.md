## Context

CalendarScreen gates scope controls on YEAR. MainActivity already reloads on taskScope changes, but day/week/month call /calendar without any scope support. The existing /tasks endpoint supports mine, managed, and created.

## Goals / Non-Goals

**Goals:** All modes show authorized scope choices and load matching tasks; retain selection across mode changes.

**Non-Goals:** New endpoints, backend permission changes, or changes to calendar period definitions.

## Decisions

Remove the year-only UI condition and keep the existing review capability gate. For day/week/month, retain /calendar for mine and use its server-provided rangeStart/rangeEnd to query /tasks for other scopes. This preserves business timezone and period boundaries without duplicating date calculations. Follow task pagination for these scoped queries. Retain year loading and 403 managed-to-mine fallback. Keep scope labels single-line in a horizontal scroll row for narrow screens.

## Risks / Trade-offs

Non-mine views need an additional request → reuse the server calendar range for correctness. Permission may change after rendering → retain server-side authorization and existing fallback. Compile validation requires access to the Gradle user cache, previously declined; report this limit without repeating that request.
