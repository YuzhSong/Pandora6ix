## Why

导图（首页）头部右侧同时放了「消息」和「设置」两个圆形按钮。设置入口在「我的」页已有「全部设置 / 消息通知」两行入口，首页再放一个属于重复入口，也让头部看起来拥挤。用户要求去掉首页的设置按钮。

## What Changes

- 首页头部只保留日期标题与「消息」按钮，删除「设置」按钮及其与消息按钮之间的间距
- `HomeScreen` 不再需要 `onOpenSettings` 参数，调用处相应移除命名参数
- 设置页仍可从「我的」页进入（「全部设置」「消息通知」两行），功能不丢失

## Impact

- Android：`MainActivity.HomeScreen` 头部 Row、`Route.HOME` 调用处
- 无后端改动；`openspec/specs/home-dashboard` 新增一条约束（首页 SHALL NOT 提供设置入口）
