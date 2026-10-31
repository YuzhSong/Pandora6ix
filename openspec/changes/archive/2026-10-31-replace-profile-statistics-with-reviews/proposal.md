## Why

用户要求把“我的”页审批中心移至原任务统计面板的位置，移除不需要的统计内容，让常用审批入口更靠前。

## What Changes

- 将审批中心作为独立面板放在个人资料之后、个人设置之前。
- 删除“我的”页任务统计面板及其专用数据加载。
- 保留审批入口现有能力判断、真实角标和详情导航，删除设置分组内重复入口。

## Capabilities

### New Capabilities

- `profile-review-placement`: “我的”页审批中心独立面板及统计面板移除。

### Modified Capabilities

无。

## Impact

仅 Android MainActivity 的 ProfileScreen、调用参数及专用统计读取。复用现有组件和审批状态；不修改后端统计接口或其他页面。在 Android-syz 分支实施。
