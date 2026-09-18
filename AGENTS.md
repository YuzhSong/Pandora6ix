# Pandora6ix development guide

## Scope and guardrails

- Keep `android/`, `web/`, and `backend/` independently buildable.
- Do not add product features, business APIs, database tables, credentials, or infrastructure services without a confirmed requirement and an OpenSpec change.
- Never commit secrets, signing material, or Android `local.properties`.

## OpenSpec workflow

Every non-trivial requirement or behavior change is managed under `openspec/`.

1. Create a kebab-case change with `openspec new change <name>` (or `/opsx:propose` in Codex).
2. Follow `openspec status --change <name> --json` and CLI-provided instructions. Complete proposal, specification deltas, design, and tasks before implementation.
3. Implement approved tasks; run module checks and `openspec validate <name> --strict` before review.
4. Submit a PR linking the issue and OpenSpec change. A non-author reviews ordinary PRs.
5. After merge and validation, run `openspec archive <name>` to update main specs; `--skip-specs` is only for genuine infrastructure, tooling, or document-only changes.

Do not invent OpenSpec commands or hand-create a fake OpenSpec layout. The checked-in `.codex/skills/openspec-*` skills provide Codex entry points.

## Branch conventions

- `main` is the stable integration branch.
- `LoFi` is reserved for Android low-fidelity prototyping; do not create a `prototype/` directory.
- Use focused feature or documentation branches, then a PR into `main`.
- Stakeholders review LoFi work before only reusable code is selectively merged through a PR.
