## Why

The bottom navigation currently uses generic Material icons that do not match the approved reference direction. The five destinations need clearer, lightweight outline symbols while keeping the existing navigation behavior unchanged.

## What Changes

- Replace the Home, View, Logs, AI map, and Profile navigation icons with closer semantic outline icons.
- Keep labels, routes, selection color, and navigation behavior unchanged.

## Capabilities

### New Capabilities
- `nav-icon-refresh`: Reference-aligned bottom navigation iconography.

### Modified Capabilities

## Impact

- Android `MainActivity.kt` navigation icon definitions only; no backend, data, or route changes.
