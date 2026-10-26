## Context
PR #30 unifies recipient selection and derives task kind, while backend delegated validation currently accepts subordinates only. PR #28 also edits MainActivity and notification preferences.

## Goals / Non-Goals
Goals: preserve both PRs' UI behavior and account isolation, and make mixed self/subordinate tasks usable through existing APIs. No new API, schema or approval bypass is introduced.

## Decisions
- Keep #28's separate journal detail route, home header cleanup and permission-request marker. Keep #30's capability-aware calendar, supervisor name and review navigation. Use server capabilities for entry visibility.
- A self-only selection uses PERSONAL. DELEGATED requires at least one other authorized active subordinate, and may also include its creator. Reuse a narrow task-recipient policy in creation, pending edits and approval context validation; do not widen ScopeService.managed, which is also used by unrelated authorization.
- Non-founders still require their designated active supervisor's approval. Founder auto-approval from dev remains intact. Ownership/tenant/status checks are unchanged.
- Preserve optional tags, scoped draft state and complete task detail. Run module builds, real API regressions and device UI smoke checks where available.

## Risks / Trade-offs
- Mixed assignments must pass both submission and approval validation; use the same policy and verify both stages, including out-of-scope and non-manager rejection.
- Calendar/layout changes need rendered checks; build success alone does not establish visual quality. Record unavailable physical-device checks honestly.

## Migration Plan
No migration. Deploy backend authorization alongside the client; older clients remain compatible. No production test tasks or accounts are created.
