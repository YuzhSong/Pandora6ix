## Why

从信封消息列表进入任务详情或审批页后，返回时列表关闭，用户需要反复点击信封才能继续阅读。

## What Changes

- 保留首页消息列表打开状态及可保存的滚动状态，详情返回时直接回到列表。
- 显式关闭消息列表后返回首页；维持消息已读和跳转规则。

## Capabilities

### New Capabilities
- `notification-return-navigation`: 消息目标页面返回原消息列表。

### Modified Capabilities

无。

## Impact

Android 首页导航与消息抽屉状态管理，无接口或数据库变更。
