## Why

预设重复频率不应始终展开自定义间隔；连续跨天任务完整覆盖的日期应与全天任务统一展示。

## What Changes

- 增加独立自定义选项，仅选中时显示间隔和单位，预设使用间隔 1。
- 周视图顶部横条纳入完整覆盖日期，首尾部分时段仍显示每日卡片，避免同日重复。

## Capabilities

### New Capabilities

- `schedule-display-refinements`: 预设与自定义交互、完整日横条展示。

### Modified Capabilities

无。

## Impact

Android 任务表单、草稿与周视图布局，不改变后端时间语义和任务数据。
