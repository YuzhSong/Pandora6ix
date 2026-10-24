# Android 当前后端契约 V0.2

2026-10-17 兼容增量：最新人员字段、接收人 departmentId/departmentName、公司日期日报改删及补交只读规则见 [联调交接](CLIENT_HANDOFF_20261017.md)。日报新增 businessToday/allowedActions；纯 ADMIN 不获得标签或私有业务权限。新后端变更经审查入 dev 后生效，下面保留 2026-10-15 基线说明。

2026-10-15，`backend-syz` 基于 `dev ec288d4`。以当前已合入 dev 的 Android-lj / Android-swj 源码和用户本轮十模块清单为准。需求 V1.3 / API0.1 是历史基线；用户已明确确认本轮开放直属上级审批后端，覆盖旧文档中“审批全部 Reserved”的限制。消息 inbox / 已读仍未开放。业务接口的机器可读定义为 [openapi.json](openapi.json)。本轮只改后端，不代表 Android 已接入网络。

## 十模块入口

下表均以 `/api/v1` 为前缀；所有私人读取与写入从 Bearer 会话取得企业和本人身份。

| 模块 | 接口与对接要点 |
|---|---|
| 登录与我的 | `POST /auth/login`，请求 enterpriseDomain/loginName/password；`GET /me` 返回姓名、jobTitle、department、organization、enterprise、businessTimezone、roles/capabilities；`POST /me/password` 传 currentPassword/newPassword，204 后当前会话失效；`POST /auth/logout` 204。过期/撤销返回 401，没有 refresh-token 接口 |
| 导图四面板 | `GET /dashboard` 的 companyImportantItems/companyTasks/personalImportantEntries/workEntries；更多按面板 moreUrl/nextCursor 拉取。全文分别 `GET /company-important-items/{id}`、`GET /tasks/{id}`、`GET /work-log-entries/{entryId}`。整篇日报另读 `/work-logs/{journalId}`，不能把 entryId 当 journalId |
| 任务录入 | `GET /tasks/recipient-candidates?q=&cursor=&limit=` 返回真实 id/displayName/organizationName/relationship；`POST /tasks` 使用 PERSONAL/DELEGATED。个人只归本人；派发 recipientIds 为管理范围内真实用户 ID，可多人。返回 taskId/reviewId/PENDING，审批通过前不进入接收人正式任务 |
| 日周月视图 | `GET /calendar?mode=day\|week\|month&anchorDate=YYYY-MM-DD&tagIds=...&cursor=&limit=`；采用返回的 rangeStart/rangeEnd/today/businessTimezone，不按手机时区重算业务日。`tasks/nextCursor/totalCount`；AND 标签；日历不含日志。一般任务列表 `/tasks?scope=mine\|managed\|created&from=&to=`。年模式不增加新枚举，可按既有日期区间查询任务 |
| 日报 | `GET/POST /work-logs`、`GET/PATCH /work-logs/{id}`；一篇 entries 多条，发布用 clientEntryKey；修改用 expectedVersion/upsertEntries。已有条目用 entryId，补充用新 clientEntryKey；遗漏的旧条目保留。重复同日 409 带 existingJournalId；编辑冲突 409 不覆盖 |
| 个人重要事项 | `GET /me/important-entry-candidates` 为 D～D-9 的工作条目候选；`GET/PUT /me/important-entries` 用 entryIds 与 expectedVersion，最多十条；`[]` 是持久化的主动清空。初始版本 0；requiresSelection/hasManualSelection 区分未选择和已清空；不是整篇日志或内容副本 |
| 任务进度 | `POST /task-assignments/{assignmentId}/progress-records` 传 expectedVersion/progressPercent/note，使用执行关系版本。100 仍为 ACTIVE，不等于完成；详情返回 progressRecords 历史 |
| 完成与验收 | PERSONAL：`POST /tasks/{id}/complete`，expectedAssignmentVersion。DELEGATED：接收人 `POST /task-assignments/{id}/completion-requests`，创建者 `POST /task-completion-requests/{id}/decision` 使用 ACCEPT/REJECT/request.expectedVersion；只改变对应接收人的执行关系 |
| 企业标签 | `GET /tags`，按 id/name/colorHex/version 保存；task.tagIds 和独立 primaryTagId，displayColor 为主标签颜色。业务创始人/部门负责人可创建，员工/团队长选用；ADMIN 管理不赋予业务审批权。日志标签按整篇，不按条目染色 |
| 本人统计 | `GET /statistics` 或 dashboard.statistics：total/completed/uncompleted，basis=OWN_APPROVED_ASSIGNMENTS；只按真实 COMPLETED 事实计数，待审/驳回不算正式任务，进度 100 不算完成 |

## 直属上级审批

### 列表与详情

- `GET /task-reviews?scope=pending&limit=20&cursor=...`：本人作为指定审核人的当前待审记录。员工不可查询审核队列；待审任务修改后的旧 attempt 不再进入队列。
- `GET /task-reviews?scope=created&limit=20&cursor=...`：本人提交的所有历史 attempt，含旧快照、审批结果和批注。
- `GET /task-reviews/{reviewId}`：只允许指定审核人、提交者读取；其他人或跨企业统一 404。即使知道 taskId、是更高层经理或 ADMIN，也不自动获得该审核快照。

详情包含 id/reviewId/taskId/reviewerUserId/submittedByUserId、taskVersion、currentTaskVersion、attemptNo、submittedSnapshot、submittedAt、decision、version、decidedByUserId/decisionNote/decidedAt、isCurrent、allowedActions。submittedSnapshot 为提交时不可变内容；不能用当前任务正文替换旧快照。isCurrent 表示最新 attempt，已决定的最新 attempt 仍为 true，但不再有审批操作。

本轮之后的新建和待审编辑快照统一使用提交字段，包含 recipientIds/tagIds/primaryTagId，不夹带旧 reviewId 或界面 allowedActions。旧数据库已有快照不重写；旧版编辑快照可能包含 authorizedAssignments，可从其中读取历史 recipientUserId。

当前待审且权限/人员范围仍有效时，指定审核人得到 `allowedActions=["APPROVE","REJECT"]`；创建者、旧 attempt、已决定记录得到空数组。组织或账号变化可能让队列中记录暂时不可操作，应重新读取并由创建者更正待审申请，不自动换审核人或批准。

### 提交决定

```http
POST /api/v1/task-reviews/{reviewId}/decision
Authorization: Bearer <accessToken>
Idempotency-Key: <this-action-key>
Content-Type: application/json
```

```json
{"expectedVersion":0,"decision":"APPROVE","comment":"审核通过"}
```

decision 只接受 `APPROVE/REJECT`；这与完成验收的 `ACCEPT/REJECT` 是不同命令。expectedVersion 使用审核记录 version，不是 taskVersion 或 assignment.version。comment 可省略。

成功 200：`data={review,task}`。同一事务保存决定、审核人/时间/批注，更新 task.reviewStatus 与 task.version；APPROVE 同时将所有 PENDING_APPROVAL 关系激活为 ACTIVE，填写 activatedAt 并增加 assignment.version。必须从返回 task 或接收人的最新 task 详情读取新版本，再提交进度/完成。REJECT 保留 PENDING_APPROVAL 关系，不派发；重提规则仍待确认。

同一 Idempotency-Key + 同一命令重放原结果；修改命令不能复用旧键。并发决定只允许一个成功。新任务不是自动批准；无有效上级仍返回 WORKFLOW_POLICY_UNCONFIRMED，创始人不能自审。

| 错误 | 客户端处理 |
|---|---|
| 401 | 清理失效会话，回真实登录 |
| 404 RESOURCE_NOT_FOUND | 不存在/不具备审核参与者权限，不通过其他路由绕过 |
| 409 VERSION_CONFLICT | 记录版本已变化，重新读取；不要覆盖 |
| 409 REVIEW_NOT_PENDING | 旧快照、任务已决定或不是当前有效版本；打开最新申请/结果 |
| 409 REVIEW_CONTEXT_CHANGED | 当前直属上级、业务角色、接收人状态或管理范围变了；不批准、不部分激活 |
| 409 IDEMPOTENCY_CONFLICT | 同键命令不同；保留原动作结果，为新动作使用新键 |
| 422 | expectedVersion、decision 等字段无效，显示字段错误 |

## 安卓模型映射

`/me.capabilities.createPersonalTasks/createDelegatedTasks/reviewTasks` 是角色层面资格，替换 DemoRole.canCreateTasks；不能继续把所有员工的个人任务入口禁用。业务命令仍校验配置的审核人和范围，能力值不代表绕过 Q-01。ADMIN-only 三个值均 false。

`Task.authorizedAssignments[]` 保存 assignmentId（字段 id）、recipientUserId、executionStatus、progressPercent、completionSource、version 及 allowedActions。多人分别显示，不合成一个中文状态：

- ACTIVE 本人关系：UPDATE_PROGRESS 以及 COMPLETE_PERSONAL 或 REQUEST_COMPLETION。
- 创建者看到 AWAITING_ACCEPTANCE 的那一条关系：DECIDE_COMPLETION。
- 待审、驳回、完成或无该动作权限的关系：空数组。

Task.allowedActions 保留聚合操作集合，具体执行必须再对应到授权 assignmentId；任务的最新 reviewId 在审批后仍保留，可查询结果。任务列表为摘要，description 全文在详情。

标签按真实字符串 ID，不以名称当键。日志工作条目用稳定 entryId，重要事项选择只写 ID 集合。PERSONAL/DELEGATED、reviewStatus、executionStatus、progressPercent、completionSource 分别建模；不要用单个模拟 status 字符串代替。

## 本地验证与边界

```powershell
cd D:\Code\Pandora6ix\backend
$env:JAVA_HOME = 'D:\Downloads\JDK'
.\mvnw.cmd package
python scripts/generate-openapi.py --check
python scripts/android-smoke.py
```

smoke 使用随机账号和独立 `target/android-smoke/<uuid>` 文件数据库，三个后端进程依次启动：V1 写入 → V2 自动升级和完整业务流程 → V2 重启持久化。所有批准通过公开审核接口完成，没有 SQL 批准夹具；结束只停止它自己启动的进程。运行日志不输出密码或 Bearer。

Android 仍需自己实现网络权限、HTTP/JSON、Repository/ViewModel、加载/重试、Bearer 安全保存、账号隔离、表单与审批交互。P1/P2、inbox、无上级、自审、驳回重提、获批内容编辑、跨日/同刻 TIMED、服务端草稿、条目自动补位/移除等未决规则未开放。没有新增 Android/Web 功能、PR 或生产部署。
