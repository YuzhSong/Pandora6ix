## Why

1. 下属日志列表部分条目显示不出人名（退化成「成员 xxxx」占位）。原因是日志列表接口只返回 `authorUserId` 不返回作者姓名，客户端只能用本地组织成员名单兜底；停用账号或不在本地名单中的作者必然解析失败。详情接口早已返回作者资料，列表缺失属于实现遗漏。
2. 任务执行人点「已完成」需要审批后才算完成：执行人不是发起人时由发起人验收；执行人与发起人是同一人（个人任务、自派任务）时由该任务的审批人（创建者的直属上级）审批。当前个人任务完成后直接置为 COMPLETED，没有任何审批，导致个人任务完成缺少把关。

## What Changes

- 日志列表接口（`GET /work-logs`）每条记录补充 `author` 资料（displayName / jobTitle / organizationName / departmentName），与详情接口一致；客户端日志列表优先使用后端姓名，本地名单仅作兜底。
- 个人任务完成（`POST /tasks/{id}/complete`）在存在完成审批人时改为提交完成申请（`AWAITING_ACCEPTANCE` + `task_completion_request`），审批通过后才置为 COMPLETED；没有审批人（创始人 / 无上级）时保留原有的本人直接完成。
- 完成审批人解析：执行人 ≠ 发起人 → 发起人；执行人 = 发起人 → 该任务最近一次任务审批的 reviewer。完成申请的通知对象、验收权限（`DECIDE_COMPLETION`）与 decide 接口校验统一使用该审批人。
- 客户端提示文案同步：完成任务 / 申请验收说明需审批人确认。

## Capabilities

### Modified Capabilities
- `task-workflow-completion`: 完成审批人解析与个人任务完成审批。
- `backend-journals-dashboard`: 日志列表返回作者资料。

## Impact

后端：`JournalService`（列表返回 author，批量取作者资料避免 N+1）、`TaskService`（`complete` / `requestCompletion` / `decide` / `assignmentActions` 统一走 `completionApprover`，抽取 `submitCompletion`）。数据库无需迁移（复用 `task_completion_request` 与 `task_review`）。
Android：`TaskSummaryFlow`（日志列表优先后端作者姓名）、`MainActivity`（日志卡片头像姓名）、`ProgressSheet`（完成/验收提示文案）。动作可见性仍由服务端 `allowedActions` 驱动，无需改动按钮逻辑。
