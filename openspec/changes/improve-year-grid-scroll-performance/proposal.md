## Why

用户反馈年视图滑动有掉帧感。连接手机的 debug 基线八次固定滑动采样 314 帧，帧耗时 P50/P95/P99 为 17/65/97ms，GPU P95 为 5ms。月份卡片为每个日期创建独立 Text 与布局，存在可减少的文本布局开销；这些数据不等价于 release 性能结论。

## What Changes

- 年视图日期数字改为月份级缓存绘制，减少独立 Text 布局节点。
- 保留每个日期的点击、完整读屏日期、今日强调和任务圆点。
- 保留懒加载、跨年标题、窗口回中和导航行为，并复测相同滑动动作。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `calendar-view`: 年视图轻量渲染且保持日期交互和辅助访问。

## Impact

仅 Android 年视图月份渲染和验证，不更改业务 API、任务数据或日/月/周视图。
