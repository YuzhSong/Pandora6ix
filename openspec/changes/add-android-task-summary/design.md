## Context

The calendar starts with a mode selector, not a separate title. MainActivity owns the five navigation tabs and existing task/journal details. CalendarRepository.tasks already supports scope and cursor; TaskRepository.recipientCandidates currently aggregates all candidate pages. JournalRepository supports mine/managed pagination. ScopeService enforces management scope on the server.

## Goals / Non-Goals

**Goals:** Small entry in the mode row; exactly three summary panels; real paginated tasks/journals, direct reports, authorized organization hierarchy and member details; preserved navigation state and existing theme.

**Non-Goals:** Backend changes, admin directory access, KPIs, charts, contacts tab, new permissions, synthetic people or tasks, calendar redesign.

## Decisions

- Add a paginated candidate method to TaskRepository and keep its existing aggregate method compatible. Summary initially loads one page; more pages are explicit. DIRECT comes only from relationship=DIRECT. Reuse recipientOrganizationTree, which groups only server-returned department/team IDs; never infer reporting relationships from names or department membership.
- Use CalendarRepository.tasks with mine/created/managed unchanged. managed includes subordinate creators OR authorized executors; explain this in the panel. Member responsible tasks filter authorizedAssignments by recipientUserId after server filtering; authorship is not responsibility.
- Use JournalRepository.journals(managed) for managers and mine for other business accounts. Reuse loaded scoped pages when browsing members; explicit pagination continues searching when a member has no match yet. No per-member request fanout. No DashboardRepository call because its statistics and other panels are irrelevant.
- Extract existing RemoteAvatar into a shared component and reuse priorityMeta, tag colors, review/execution states and existing detail composables. Own feature navigation/pagers outside destination composition; save calendar and feature destination UI state with SaveableStateHolder, including lazy scrolling and filters.
- Expose API errors with retry and preserve successful independent panels. UI eligibility uses /me capabilities; every request still uses existing authenticated ApiClient and server scope. Reset feature state on account changes.

## Risks / Trade-offs

- Candidate API excludes inactive members and omits phone/rank and supervisor IDs. Show unavailable contact/rank text, expose authorized department/team expansion, and explicitly document that recursive reporting lines await backend support. Do not use /admin/users or /admin/org-units.
- Candidate and member-filtered collections may be incomplete until additional pages load. Keep next-page controls and an explicit partial-result hint, including empty filtered pages. Never label a partial page a complete absence.
- No avatar/name fields exist in task assignments or journals. Resolve names from /me and authorized candidates; use an explicit member-ID label otherwise, with avatar fetched by its actual user ID and the existing initial fallback.
- Navigation/state, permission and cursor filtering require focused unit checks and Android build/lint; physical device validation depends on an available device/account.

## Migration Plan

Client-only change. Build an unsigned debug APK; rollback by reverting the feature commit. No deployment or data migration.

## Open Questions

Backend follow-up: ordinary scoped member profile (phone/rank) and reporting chain, and optional recipient/author filters to avoid paging unrelated authorized records in member detail. These are documented gaps, not additions in this Android change.
