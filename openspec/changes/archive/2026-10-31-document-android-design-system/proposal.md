## Why

Android has theme-aware colors alongside fixed Material defaults and many local sizes. A measured audit and explicit design contract are needed before any global UI migration.

## What Changes

- Inventory current pages, components and visual declarations with source evidence and reproducible static counts.
- Document a Pandora design system with exact values and Compose mappings.
- Publish a prioritized migration roadmap. This change is documentation and audit tooling only; no runtime implementation.

## Capabilities

### New Capabilities
- `android-design-system-documentation`: source-backed UI audit, design tokens and migration acceptance rules.

### Modified Capabilities
None. Existing business behavior stays unchanged.

## Impact

Only docs and OpenSpec artifacts; no APIs, repositories, schema, navigation or Android runtime changes. Work stays on backend-syz per the existing session instruction.
