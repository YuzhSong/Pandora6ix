## Why

The Week view has an unnecessary explanatory label, and Month rows currently grow with their content. The review needs a predictable weekly rhythm with only three visible task bars and a clear Month-to-Week drill-down.

## What Changes

- Remove the Week view “任务横条” label.
- Give every Month week a fixed equal height, display up to three bars, and summarize the rest as `+N条计划`.
- Make Month interactions open the selected week; preserve Week interactions opening Day view.

## Capabilities

### New Capabilities
- `month-week-drilldown`: Fixed Month week rows and Month-to-Week navigation.

### Modified Capabilities

## Impact

- Android Compose calendar rendering and ViewScreen mode transition only.
