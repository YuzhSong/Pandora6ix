# Validation — 2026-10-23

- Full Web suite: 135 passed, zero failures, including local proxy tests (run outside sandbox).
- `npm run build` passed; `npm run lint` completed without errors, with existing effect warnings. Existing bundle-size warning remains.
- Component server rendering verified new-person generated/read-only password and four role options, absence of historical arbitrary job text from profile edit/detail, and generated-password/role controls in batch grid. This was a rendering check, not an interactive browser layout check.
- Password tests verify independent 16-character values, backend policy, four character classes, stable row edits and unused blank rows.
- Batch tests verify actual TEAM_LEAD role submission, role/organization and supervisor rejection, CSV compatibility, and existing account handoff/partial failure behavior.
- OpenSpec strict validation and git whitespace check passed.
- New UI uses actual roles for permissions and labels. Historical backend jobTitle values are retained during profile editing but hidden from duplicate UI. CSV imports may preserve provided passwords; omitted values are generated once on import.
- No production records changed; no commits, pushes or deployments made.
