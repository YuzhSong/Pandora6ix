## Why

接任负责人后，旧的员工角色可能仍留在账号中，导致 Android“我的”页同时显示员工和负责人。

## What Changes

- 负责人交接时，用接任后的组织角色替换接任人之前的组织角色。
- Android“我的”页只显示当前组织角色，并兼容已有的多角色资料。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `personnel-lifecycle` 的负责人交接与当前角色展示。

## Impact

涉及后端负责人交接和 Android“我的”页。历史记录与接口字段沿用现有结构。
