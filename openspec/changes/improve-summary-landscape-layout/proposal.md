## Why

任务汇总横屏时，固定标题、范围标签和底部主导航占据过多高度，表格只剩很窄的滚动区域，无法有效查看。页面还未消费外层 Scaffold 已应用的系统栏留白，可能重复占位。

## What Changes

- 横屏任务汇总隐藏底部主导航，保留返回及恢复竖屏入口。
- 将横屏标题、范围选择和操作按钮合并为紧凑工具栏，表格使用剩余全部高度。
- 消费已应用的系统栏边距，避免子级重复留白；横屏隐藏额外滑动提示。
- 保留竖屏范围标签、冻结表头/首列、滚动及详情返回状态。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `android-task-summary`: 补充汇总横屏布局及可用表格高度规则。

## Impact

Android 主 Scaffold 的汇总页导航显示、TaskSummaryFlow 布局及 TaskSummaryTable 提示。无接口或数据库变化；不影响其他页面的底部导航。
