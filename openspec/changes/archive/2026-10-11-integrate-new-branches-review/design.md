## Context

PR #7 already requires editable formal items and candidate exclusion. The implementation stores strings and excludes candidates by content, so an edit disconnects the item from its source. WorkLog already supplies a stable id and session data is held above page navigation.

## Goals / Non-Goals

**Goals:** Preserve log identity and edited text through selection, editing, reordering and candidate round trips; validate the integrated branch and reconcile calendar requirements.

**Non-Goals:** Permanent persistence, real authentication, business APIs, new log parsing or new product behavior beyond the reviewed PR requirements.

## Decisions

- Store each formal item as sourceLogId plus content, preserving identity independently of text. Content-only deduplication fails after edits and incorrectly merges separate logs with equal content.
- Keep a session-level draft map keyed by source id, shared across page visits. Removing an item retains its draft so reselection restores the user's edit. Mutating the published log to store the draft would change unrelated report history.
- Extract candidate filtering and selection/edit operations into pure Kotlin helpers. Filter by current day, user and source id, with the existing ten-day inclusive window and ten-item cap.
- Retain month/year date selection behavior; document the new week-date entry into day mode from PR #7. No additional calendar behavior is introduced.
- Reposition the year grid for any external year change, including an adjacent year. Reuse an index within the current window when possible, keep the latest year callback, clamp leap-day cursors through plusYears, and retain the scroll offset when reanchoring the window.

## Risks / Trade-offs

- Session-only drafts are cleared on application restart → matches the existing prototype data lifecycle.
- A source outside the current ten-day window will not reappear as a candidate → keep the source-window requirement and validate its boundary explicitly.
- Emulator checks cannot substitute for stakeholder device acceptance → record the checks performed and leave any unperformed gestures explicitly pending.
