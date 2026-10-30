## Why

重复任务只应创建时审批一次；编辑必须由用户选择本次或本次及未来范围，任务表单应减少解释性小字。

## What Changes

- 重复创建仅产生首条审批，一次批准/拒绝整组，保留各次完成状态。
- 新增有版本和权限校验的本次/本次及未来详情及状态修改接口，事务原子提交。
- Android 保存后选择范围，清理创建/详情/进度的冗余说明。

## Capabilities

### New Capabilities

- `series-approval-scope`: 重复组单次审批及范围编辑。

### Modified Capabilities

无。

## Impact

后端创建/审批/范围接口及 V13 原始发生日期迁移；Android 编辑器、详情和完成操作；OpenAPI。承接 recurring-task-schedules 并替代逐次审批/仅单次编辑设计。
