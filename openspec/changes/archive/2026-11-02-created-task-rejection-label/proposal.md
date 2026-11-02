## Why

“我创建的”任务中，被驳回的任务需要直接显示明确状态，便于识别审批结果。

## What Changes

- 日历任务展示被驳回标记，汇总任务状态统一显示“被驳回”。
- 依据现有 reviewStatus 判断，不把执行取消等其他状态误标为驳回。

## Capabilities

### New Capabilities
- `created-task-rejection-display`: 创建任务的驳回状态可见。

### Modified Capabilities

无。

## Impact

Android 日历任务及汇总状态文案；无接口和存储变更。
