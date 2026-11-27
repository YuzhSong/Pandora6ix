## Why

任务创建的 @ 选人面板沿用接口的姓名顺序。部门长和组长可能排在普通成员之后。

## What Changes

- 展开部门时，按业务角色由高到低显示直属成员。展开小组时，组长排在普通组员前面。
- 选人候选返回可用于排序的业务角色等级。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `android-recipient-hierarchy` 的组内成员排序。
- `backend-task-workflow` 的候选角色等级。

## Impact

涉及 Android 任务选人面板、现有候选接口及其 OpenAPI 契约。
