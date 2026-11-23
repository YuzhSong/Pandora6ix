# Tasks

## 1. 后端

- [x] 1.1 `ScopeService` 新增 `activeBusinessUsers(actor)`：返回本企业 ACTIVE 且拥有业务角色的用户 ID 集合
- [x] 1.2 `ScopeService.taskRecipients` 改为 `activeBusinessUsers + 创建者本人`（无条件含本人，允许纯自派），`actor.manager()` 放宽为 `actor.business()`
- [x] 1.3 派发候选使用 `scope=enterprise` 与业务权限，通讯录保留默认管理范围和管理权限。两种范围均保留搜索、分页及部门元数据
- [x] 1.4 `IdentityService` 的 `createDelegatedTasks` 能力位放宽为 `businessRank() != 99`
- [x] 1.5 `TaskReviewService` 移除创建者职级 >= 3 的审批前置拒绝，接收人授权统一由 `taskRecipients` 判定

## 2. Android

- [x] 2.1 `TaskEntrySheet.selfRecipient` 从 `/me` 的 `organization`/`department` 补全组织字段，本人显示在真实组织分组下

## 3. 测试

- [x] 3.1 更新 `recipientsIncludeAllActiveBusinessMembersAcrossDepartments`：跨部门/跨层级候选人与派发任务应成功，员工可访问候选接口
- [x] 3.2 更新纯自派与员工派发断言（原 403 → 201）
- [x] 3.3 关键测试先行通过（自派/员工派发/候选访问/禁用账号 4 例）；全量回归进行中

## 4. 验证

- [x] 4.1 `openspec validate allow-task-assignment-to-all-staff --strict` 通过
