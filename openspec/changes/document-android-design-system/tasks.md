## 1. Audit

- [x] 1.1 Inventory all existing Android destinations, overlays and component families against source.
- [x] 1.2 Generate reproducible visual declaration inventories and review historical requirements.

## 2. Documentation

- [x] 2.1 Write the audit report with counts, evidence, top issues and validation limits.
- [x] 2.2 Specify exact design tokens and component-to-Compose mappings.
- [x] 2.3 Write the staged roadmap and link documents from docs index.

## 3. Verification

- [x] 3.1 Reproduce inventories, check links, validate OpenSpec strictly and verify documentation-only diff.

## Verification

- Inventory regenerated twice with identical hashes; 53 Kotlin files, 83 Composable declarations, 25 base background Hex values, 54 background expressions, 10 explicit ButtonDefaults argument sets, 46 Text size/weight expressions, 16 radius argument sets, 24 numeric spacing values.
- All local links in the three new docs resolve. Five target primary colors independently pass 4.5:1 white-text contrast (4.86/4.95/5.35/5.43/5.80).
- `openspec validate document-android-design-system --strict` and `git diff --check` pass.
- No runtime files changed; no new APK build, device screenshot matrix, performance profile or TalkBack run is claimed.
