## Why

The prototype is functionally complete but still has density and hierarchy issues: selected navigation icons use the default purple indicator, View controls consume a separate row, the return action is oversized, and Home cards leave unused space while their text rhythm is loose.

## What Changes

- Use warm orange for selected bottom-navigation icons and labels.
- Place the day/week/month mode control in the Work View header's right action area.
- Reduce the fixed return-to-today bookmark to one-fifth screen width and a compact height directly above navigation.
- Tighten Home grid gaps, remove its helper footer, move panels lower/taller, and rebalance title/item typography and row spacing.

## Capabilities

### New Capabilities
- `navigation-density-tuning`: Compact, warm navigation and balanced Home/View hierarchy.

### Modified Capabilities

## Impact

- Android `MainActivity` Compose layout/theme usage only; no new dependencies or data changes.
