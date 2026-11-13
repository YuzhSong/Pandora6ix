## Why

创建审批已驳回的任务仍显示在通讯录下属任务、视图和任务汇总中，造成无效任务混入日常工作。用户明确要求仅在审批中心“我提交的”保留驳回历史。

## What Changes

- 在 Android 普通任务数据入口统一排除 reviewStatus 为 REJECTED 的任务，覆盖全部任务范围、视图及分页。
- 审批历史仍保留驳回记录、详情与重新提交入口。
- 验收申请被驳回但创建审批已通过的任务继续正常显示。
- 替换既有要求在日历和汇总标注驳回任务的展示规则。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `created-task-rejection-display`: 将普通列表显示驳回标签改为隐藏已驳回任务，仅在本人审批历史保留。

## Impact

Android CalendarRepository 及共用任务可见性规则、回归测试。无需更改后端接口、数据库或部署；保留服务端分页游标，不删除任务及审批历史。
