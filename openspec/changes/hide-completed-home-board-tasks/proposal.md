## Why

首页任务看板目前展示所有派发给我的任务，已完成任务仍占据列表。用户要求首页隐藏已完成任务，以聚焦待处理工作。

## What Changes

- 首页任务看板及展开列表排除当前用户执行状态为 COMPLETED 的任务。
- 首次加载和后续分页采用同一过滤规则；过滤后空页继续读取后续页，避免错误显示空态。
- 保留待验收等其他状态，保持公司任务列表与详情查询不变。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `home-dashboard`: 首页任务看板按当前执行人的完成状态过滤。

## Impact

Android 首页、任务看板分页辅助逻辑及对应单元测试；复用现有任务 API，无后端变更。
