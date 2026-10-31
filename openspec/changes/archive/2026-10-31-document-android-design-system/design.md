## Context

This is a documentation-only audit of the current backend-syz Android baseline (8123047). Existing five themes, fixed Material colors and local styling coexist. The supplied brief and latest session decisions take priority over older requirements.

## Goals / Non-Goals

**Goals:** reproducible declaration inventory, full route/state coverage, exact target tokens, accessible component contracts, staged engineering mapping.

**Non-Goals:** changing runtime UI, themes, permissions, data, navigation or adding pages; claiming static analysis is screenshot validation.

## Decisions

- Scan all main Kotlin files, report literal/call-site counts separately from runtime colors. Keep dynamic tag data unbounded and outside the UI palette budget.
- Preserve warm identity and all five existing theme choices. Use three surface roles, a shared readable primary and fixed white overlays; theme variants are explicit, not page-local palettes.
- Provide three main Chinese docs and small reproducible CSV/JSON evidence. Keep future implementation mappings marked unimplemented.
- Existing user-requested panels and inner item boundaries override generic advice against nested cards. Use subtle borders without decorative shadows.

## Risks / Trade-offs

- Static calls do not resolve Material defaults or dynamic colors → declare limits, retain expressions and source lines.
- Legacy docs include superseded product constraints → record precedence, do not rewrite old requirements silently.
- No all-page device matrix → explicitly list unverified visual/accessibility behavior and require it during migration.
