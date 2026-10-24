## Context

PR #4 changes log/IME behavior and the calendar; PR #5 changes home editing/settings; backend-xjy adds error handling and domain ports. The requested target is dev. V1.3 requires explicit calendar modes, no automatic date drill-down and one daily report. All Android data remains a prototype in memory.

## Goals / Non-Goals

**Goals:** Preserve both contributions, fix review regressions, keep independent module builds and record reproducible validation.

**Non-Goals:** Real authentication, persistence, business APIs, database tables or deployment changes.

## Decisions

- Merge source histories on a temporary integration branch and resolve the import conflict by retaining both imports needed by PR #5. Update dev only after validation; avoid rewriting source branches.
- Hoist mutable personal items, published logs and notification state to PandoraApp. Page-local remember loses state when AnimatedContent disposes a page; disk storage exceeds prototype scope.
- Initialize a single mutable log collection with mock history and update today's existing record in place. Combining a second list with MockData.logs duplicates today's seeded record.
- Restore day and the mode dropdown alongside year/week/month. Explicit pinch gestures still select adjacent modes; task taps invoke onOpen and date taps only change the cursor. Overflow opens a list in the current mode.
- Extend Spring's ResponseEntityExceptionHandler and override its shared response boundary, retaining status/headers while producing ApiError. A catch-all alone turns framework client errors into 500; enumerating all MVC exceptions would miss future cases.

## Risks / Trade-offs

- Prototype state resets at process exit -> document the memory-only boundary.
- Touch, large-font and IME behavior require device validation -> record performed checks and any unavailable emulator coverage honestly.
- Missing provider CI connection -> rely on inspected source and local module checks; do not claim provider checks passed.
- Remote branch may advance during review -> fetch and verify source/target heads before pushing; use a normal fast-forward push.

## Migration Plan

Commit repairs after integration, validate, fast-forward local dev and push dev. No runtime migration is needed; rollback uses normal Git revert commits.
