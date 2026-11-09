## Why

首页事件纪详情将标题和正文拼成同一段，标题不突出，正文阅读层次不清。用户要求放大详情标题，并让正文另起一行。

## What Changes

- 事件纪详情单独显示 24sp 标题，正文另起一段显示并自动换行。
- 保留正文原有换行、滚动、加载与失败重试行为。
- 其他首页详情保持现有展示。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `home-dashboard`: 调整事件纪单条详情的标题与正文层级。

## Impact

仅涉及 Android `HomeItemDetailScreen` 的展示；无 API、数据结构或依赖变更。
