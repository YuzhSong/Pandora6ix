## Why

日视图只展示一天，却沿用周视图的错位叠放，重叠任务会互相遮挡。

## What Changes

- 日视图重叠任务按列左右并排，同组均分宽度；独立时段恢复全宽。
- 保持开始与结束时刻在时间线上的垂直位置，复用空闲列。
- 日视图点击卡片直接选中该任务；周视图保留原布局和选择流程。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `calendar-view`: 增加日视图重叠任务并列展示要求。

## Impact

Android CalendarLayout、TimeGrid 及布局测试；不改任务数据和 API。
