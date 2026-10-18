## Context

The current backend returns authorized, paginated recipient candidates with departmentId/departmentName and orgUnitId/organizationName. A team member's organization ID differs from their parent department ID; a department's direct member has equal IDs. The user wants organizational levels to be navigable rather than a flat name-based grouping. No changes to role-based candidate authorization are required.

## Goals / Non-Goals

**Goals:** Department → team → person navigation, direct department members, stable organization identity, distinct same-name organizations, default collapsed browsing, expanded search paths and retained selections.

**Non-Goals:** New backend endpoints, changes to management scope, selecting entire groups, editing organization structures or changing journal rules.

## Decisions

- Build an immutable organization tree in a pure Kotlin helper from existing RecipientDto fields. Department nodes use department IDs; team nodes use orgUnitId and nest under the known department. Store direct department members on the department node. Preserve server result order and deduplicate repeated recipient IDs.
- Use IDs for expansion state and presentation labels only for text. When department metadata is absent, show the known organization as a terminal group; do not fabricate parentage by matching display names. Missing all organization IDs uses an explicitly unassigned group.
- Render each group as an accessible expand/collapse header with a descendant count. In browsing mode all levels start collapsed. Searching expands every matching path. Changing the query does not clear selected IDs; selecting a candidate retains the open picker and clears the query as in PR #16.
- Retain repository pagination and the backend's candidate scope. Grouping adds no candidates, role permissions or separate request. Load errors clear stale candidates and show a retryable message rather than reporting a successful empty search.

## Risks / Trade-offs

- Older responses lack enough information to reconstruct a department/team chain → show only known organization nodes while preserving unique IDs.
- Two distinct organizations may share labels → keep distinct IDs and independent expansion state, even when display text is identical.
- Android UI automation is unavailable in this session → verify pure grouping scenarios, compile the actual Compose UI and run the module's existing unit tests; document the visual verification limit.

## Migration Plan

Update Android source and OpenSpec on the head of existing PR #16, build/test and refresh its title/body to reflect final behavior. Leave both pending PRs unmerged. No backend redeployment or data migration is necessary.
