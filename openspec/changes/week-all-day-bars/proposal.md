## Why

全天任务在每日卡片中重复显示，不便看出跨日连续范围。

## What Changes

- 周视图顶部使用连续横条展示全天任务，按日期跨度对齐。
- 重叠全天任务分行，跨周裁剪；下方每日列表只显示定时任务。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `calendar-view`: 全天任务顶部连续横条。

## Impact

Android 周视图布局与测试，承接 week-view-task-stacks。
