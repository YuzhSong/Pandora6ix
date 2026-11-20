# Proposal

## Why

首页"事件纪"板块的展开浮层会显示已加载的全部条目（首屏 20 条）并支持"加载更多"，用户要求"事件纪"最多显示十条。

## What Changes

- 首页"事件纪"面板保持最多显示 10 条（现状已满足）。
- "事件纪"展开浮层只显示前 10 条，不再提供"加载更多"，副标题条数与实际显示一致。
- 其他面板（派发任务、个人重要事项等）行为不变。

## Capabilities

### New Capabilities

### Modified Capabilities

- `home-dashboard`: 明确"事件纪"展开浮层在分页数据可用时也最多显示 10 条且不提供加载更多。

## Impact

仅影响 Android 端首页事件纪浮层的展示逻辑（`MainActivity`），不改动后端 `/dashboard` 接口与其他面板。
