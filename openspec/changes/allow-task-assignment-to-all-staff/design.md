# Design

## 现状

- `ScopeService.managed(actor)`：仅包含职级低于自己（rank 更大）且（FOUNDER 或组织子树内）的用户，且先调用 `actor.manager()`（EMPLOYEE 返回 403）。
- 任务相关授权全部走 `ScopeService.taskRecipients`，被 `TaskService.createOne`、`TaskService.patch`（编辑校验）、`TaskService.validateRecipients`（审批快照校验）和 `TaskReviewService`（审批前置校验）共用。
- 候选列表 `TaskService.candidates` 以候选集合作为 SQL `IN` 过滤，且要求 `actor.manager()`。
- `/me` 的 `createDelegatedTasks` 能力位为 `businessRank() < 3`，App 据此决定能否派发。
- Android `TaskEntrySheet` 构造"本人"候选时只填 `orgUnitId`，缺组织名称，导致本人落在「未分配组织」分组。

## 方案

在 `ScopeService` 中新增 `activeBusinessUsers(actor)`：查询本企业所有 `status='ACTIVE'` 且具有任一业务角色（FOUNDER/DEPARTMENT_MANAGER/TEAM_LEAD/EMPLOYEE）的用户 ID。

- `taskRecipients(actor, recipients)` 改为：`actor.business()`（业务用户即可，员工可派发）后返回 `activeBusinessUsers + actor.id()`（无条件含本人，允许纯自派）。
- 派发候选显式传入 `scope=enterprise`，范围为全企业在职业务用户并排除本人。通讯录沿用默认的 `managed` 范围及原有管理权限。分页游标绑定到候选范围。
- `IdentityService` 的 `createDelegatedTasks` 能力位改为 `businessRank() != 99`。
- `TaskReviewService` 移除 `creator.businessRank() >= 3` 的审批前置拒绝，接收人授权统一由 `taskRecipients` 判定。
- `managed()` 不动，保证日志 scope、成员通讯录、审批者-提交者关系等既有授权不变。
- Android `selfRecipient` 从 `/me` 的 `organization`（name/kind/parentId）与 `department` 补全 `organizationName/departmentId/departmentName`，派生规则与候选人接口一致（DEPARTMENT→自身、TEAM→父部门、其他→null）。

## 权衡

- 只改 `taskRecipients` 一处即可覆盖创建、编辑、审批三条路径，避免多个调用点遗漏。
- 候选人 `relationship` 字段语义不变（仍按 supervisor 判定 DIRECT），Android 端 `SummaryData.directReports` 过滤 DIRECT 的行为不受影响。
- 拒绝非业务用户（如纯 ADMIN）作为接收人，避免创建无法执行的分配。
- App 端"只给自己"时任务类型推导仍为 PERSONAL（个人任务），DELEGATED 纯自派仅对直接 API 调用方生效，两条路径语义一致。
