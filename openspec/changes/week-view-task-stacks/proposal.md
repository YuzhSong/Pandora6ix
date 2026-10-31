## Why

周视图按小时定位和叠放导致任务互相遮挡，用户需要按天紧凑浏览有序任务卡片。

## What Changes

- 周视图七列按天垂直排列等高卡片，移除小时线、小时轴和按时长拉伸。
- 按原始开始日期与时刻升序，同起点按总持续时长降序，最后按 ID 稳定排序。
- 保留标题、开始时间、标签色及直接打开任务；全天与跨天任务在覆盖日期展示。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `calendar-view`: 周视图按天堆栈与任务排序要求。

## Impact

Android CalendarScreen、CalendarLayout 与回归测试。
