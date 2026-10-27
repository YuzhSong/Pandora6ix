# 任务流程与操作日志接入说明

对应 OpenSpec：`complete-task-workflow-audit`。以下路径均以 `/api/v1` 为前缀，响应数据位于现有 `data` 包装内。接口沿用登录令牌、企业隔离和乐观锁；遇到 409 应重新读取详情，由用户确认后重试。

## 1. 修改已生效任务

创建者调用原有 `PATCH /tasks/{taskId}`，传完整可编辑内容及当前任务 `version`：

```json
{
  "expectedVersion": 1,
  "title": "调整后的任务标题",
  "description": "调整后的任务说明",
  "priorityLevel": 3,
  "timeKind": "ALL_DAY",
  "startDate": "2026-10-27",
  "endDate": "2026-10-28",
  "tagIds": [],
  "primaryTagId": null
}
```

- 普通创建者：返回的标题、说明等仍是原生效内容，`reviewStatus` 仍为 `APPROVED`；`version` 增加，并返回 `pendingEditReviewId`。新内容在对应审批的 `submittedSnapshot` 中。
- 当前直属上级通过原有 `GET /task-reviews?scope=pending` 和 `GET /task-reviews/{reviewId}` 查看。新增 `reviewKind` 为 `CREATE` 或 `EDIT`；`effectiveTaskReviewStatus` 为任务实际审批状态。待审批修改申请的 `reviewStatus` 为 `PENDING`，方便旧客户端展示待审批状态。
- 上级调用原有 `POST /task-reviews/{reviewId}/decision`，例如 `{"expectedVersion":0,"decision":"APPROVE","comment":"同意"}`。这里的版本取审批记录自身的 `version`。通过后原子替换任务内容；驳回保留原内容、任务继续生效。
- 新修改申请会使旧申请失效，旧申请不能再审批。人员离职、直属上级或授权范围变化后，审批会重新校验；无效申请不能生效。
- 创始人修改自己创建的任务直接生效。只允许创建者修改；执行人的进度更新走下述接口。修改申请不重置执行人的进度、验收请求或完成状态。
- `GET /task-reviews/pending-count` 返回 `{"count":1}`，含当前有效的创建及修改申请。

`allowedActions` 新增 `EDIT_TASK`；原有 `EDIT_PENDING` 保留。任务修改接口不改派执行人。

## 2. 文字进度

执行人可调用原有 `POST /task-assignments/{assignmentId}/progress-records`：

```json
{"expectedVersion":1,"note":"已完成接口开发，正在联调"}
```

`progressPercent` 可省略，省略后保留原百分比；文字内容保存到 `progressRecords`，不要求进度审批，不覆盖任务创建者的 `description`。兼容旧客户端传 0–100 的百分比。百分比和非空文字至少提供一项。

## 3. 工作状态与完成验收

新增 `PATCH /task-assignments/{assignmentId}/status`：

```json
{"expectedVersion":2,"workStatus":"IN_PROGRESS","note":"正在联调"}
```

`expectedVersion` 取执行关系的版本，不能使用任务或审批版本。状态值为 `NOT_STARTED`（未开始）、`IN_PROGRESS`（进行中）、`COMPLETED`（完成申请）；只有实际执行人可更新已生效且可执行的关系。

响应为 `{"assignment":{...},"completionRequest":null}`。派发任务选择 `COMPLETED` 时，响应包含新建的 `completionRequest`，执行状态为 `AWAITING_ACCEPTANCE`，工作状态仍为 `IN_PROGRESS`。创建者作为派发上级，通过原有接口验收：

```text
POST /task-completion-requests/{requestId}/decision
{"expectedVersion":0,"decision":"ACCEPT","comment":"已确认完成"}
```

验收版本取完成申请自身的 `version`；`REJECT` 表示驳回并恢复进行中。验收通过后才设置 `executionStatus=COMPLETED`、`workStatus=COMPLETED`。待验收期间不能再次改状态或提交进度，完成后不能自行重新打开。旧个人任务的本人完成接口保持兼容。

任务详情中每条执行关系新增 `workStatus`、`allowedWorkStatuses`。后者为空表示当前不能通过新接口改状态。新状态接口、原进度/完成申请接口支持原有 `Idempotency-Key`，网络重试应复用相同键和内容。

**Android 接入注意：**当前旧页面仍有通过 `progressPercent >= 100` 判断完成的逻辑，接入时应改为依据 `executionStatus` 判断验收完成，并单独展示 `AWAITING_ACCEPTANCE`。任务详情需要展示 `progressRecords.note`，并接入修改申请和三态选择。本次只更新共享请求来源头，没有修改这些页面；Android 真机验收需客户端接入后完成。

## 4. 操作日志

复用管理员 `GET /admin/operation-logs` 及其详情接口，无新查询接口：

- 成功记录覆盖任务新增/修改、修改申请、创建及修改审批、文字进度、工作状态、完成申请/验收，以及日报新增/修改/删除。成功记录与业务同事务，审计写入失败会回滚业务。
- 对象类型包含 `TASK`、`TASK_REVIEW`、`TASK_ASSIGNMENT`、`WORK_LOG`，模块为 `TASK` 或 `WORK_LOG`。同企业、同对象类型和 ID 的成功记录组成历史，返回真实版本；无法提供的一侧为 `null`。
- 已登录请求失败时另记 `FAILURE`，只保存安全错误码、HTTP 状态、请求关联 ID、操作者及目标标识；没有前后快照或已提交版本。匿名请求不归属任何企业。
- 成功记录保存当时的操作者、创建人/执行人/日报作者名称；未来改名不影响已有快照。不会保存密码、令牌、身份证号或手机号字段。
- Web 共用客户端发送 `X-Client-Source: WEB`，Android 共用客户端发送 `ANDROID`。新任务/日报记录缺省为 `SYSTEM`。来源头仅用于展示，权限始终依据登录身份。

本轮不增加任务删除接口。日报删除保留删除前快照；未实现的任务删除不会因本次变更变为可用。旧数据的缺失历史不会补造。服务端 V11 为增量迁移，保留已有任务、审批和完成状态。
