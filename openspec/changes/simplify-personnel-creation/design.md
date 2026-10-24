# Design
## Context
See proposal. The existing API accepts password, jobTitle and roles in one create command. Batch CSV has nine positional columns and account handoff already supports generated strings.
## Goals / Non-Goals
Use actual roles as source of truth. Preserve existing personal data and backend contracts; no data migration or production changes.
## Decisions
Use Web Crypto random bytes with rejection sampling and required character classes for 16-character passwords. Generate once per active internal draft row. Use eight CSV columns without passwords, rename position to business role and accept that header alias; reject unknown role text with row errors. Default blank role to EMPLOYEE. Hide historical job text in personnel views, preserve its stored value during profile editing; new creates use canonical role text. Derive supervisor candidates from each row's selected role. Passwords appear only in successful account delivery.
## Risks / Trade-offs
Historical arbitrary job text in old CSV needs explicit correction to a supported role. Creating managers now uses existing server role/organization constraints, so validate these before submission. Generated passwords must not make blank rows count as people.

## User refinement
Remove initial passwords from single-person forms, batch grids and CSV templates. The CSV now has eight columns; nine-column templates are rejected with a download-new-template message. Internal drafts retain generated passwords privately for stable submission and successful account delivery. Show the actual application connected to the configured cloud backend, rather than the isolated seed preview.
