## Why

从首页信封消息列表打开任务或审批详情后，返回会直接显示首页。首页组件退出时其局部消息开关丢失，破坏连续阅读消息的既有约定。

## What Changes

- 在首页之外保存按账号隔离的消息列表打开状态，详情返回后恢复列表。
- 显式关闭列表、系统返回关闭列表或点击底栏导航时清除打开状态。
- 推送到达或普通任务入口不会自动打开消息列表。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `notification-return-navigation`: 明确详情返回与底栏导航的状态边界，修复既有返回行为。

## Impact

仅 Android `PandoraApp` 与 `HomeScreen` 的 UI 状态归属，无业务 API、权限或消息目标解析变更。
