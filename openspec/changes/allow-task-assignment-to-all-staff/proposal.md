# Proposal

## Why

当前创建/编辑派发任务（DELEGATED）时，接收人只能选择职级低于自己且在自己组织子树内的成员（`ScopeService.managed`），且普通员工没有派发资格、不允许纯自派。用户要求：任何人都可以给全体在职人员分配任务，也可以只给自己派发。

## What Changes

- 派发任务的接收人可选范围放宽为**全企业在职业务用户**（FOUNDER/DEPARTMENT_MANAGER/TEAM_LEAD/EMPLOYEE，status=ACTIVE），不再按职级和组织子树过滤；创建者本人始终可作为接收人（含只派给自己）。
- 派发资格放宽：任何业务用户（含 EMPLOYEE）都可创建派发任务；`/me` 的 `createDelegatedTasks` 能力位同步放开。
- 员工创建的派发任务仍走直属上级审批，审批校验不再因创建者是员工而拒绝。
- `/tasks/recipient-candidates` 候选列表返回全企业在职业务用户（仍排除本人，本人由客户端从 `/me` 资料补全），任何业务用户可访问。
- Android 选人面板将"本人"候选从 `/me` 的 `organization`/`department` 补全组织字段，分组标题显示真实组织名而非「未分配组织」。
- 日志范围、成员通讯录、审批人与提交人关系等其他使用 `managed` 的授权场景保持不变。

## Capabilities

### New Capabilities

### Modified Capabilities

- `backend-task-workflow`: DELEGATED 任务接收人范围放宽为全企业在职业务用户，任何业务用户（含员工）可派发、可纯自派。
- `home-dashboard`: 无变更（本条原属另一变更）。

## Impact

影响后端 `ScopeService`、`TaskService`、`TaskReviewService`、`IdentityService` 及相关集成测试；影响 Android `TaskEntrySheet` 本人候选的组织字段补全。不改动数据库结构。已存在的任务与分配不受影响。非业务用户（纯 ADMIN）仍不能派发或被派发。
