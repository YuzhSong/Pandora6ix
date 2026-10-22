## Why

审批详情现在是「一整张卡片里堆 `InfoRow`」：标题、类型、重要程度、日期、说明、执行人平铺在一起，扫读成本高；更关键的是**人员只有 ID**——请求人（提交人）压根没显示，执行人只显示「接收人 N 人」，审批人也不知道是谁。审批人做决定时无法一眼确认"谁申请的、派给谁"。

后端 `TaskReviewService.reviewView()` 只回传 `submittedByUserId` / `reviewerUserId` 与快照里的 `recipientIds`（都是 UUID），客户端无法凭 ID 呈现姓名。

## What Changes

- **后端**：`reviewView()` 增加展示名——`submittedByName`（请求人）、`reviewerName`（审批人）、`recipients[]`（`{id, displayName}`，取任务当前执行关系）。姓名用一次全量 `app_user` 索引（`nameIndex`）解析，避免逐条审批再查用户表；`list` / `detail` 共用同一份索引。契约同步：`scripts/generate-openapi.py` 的 `TaskReview` schema 新增三个字段并重生成 `openapi.json`。
- **Android**：审批详情改成分区卡片结构——任务信息（标题 + 决策徽章 + 类型 chip + 彩色星级重要程度 + 说明）、时间安排（日期范围 / 全天或 HH:mm–HH:mm）、**人员**（请求人 / 审批人 / 执行人，均为"首字圆形头像 + 姓名"）、标签（胶囊 chip，主标签标注）、审批结果（状态 + 审批人 + 批注 + 决定时间）。
- 队列卡片顺带显示「类型 · 请求人 姓名」，待我审批时不必点进去就知道是谁提的。
- 姓名缺失（旧数据、已停用账号）时回退显示 `成员 <ID 后四位>`；后端未返回 `recipients` 时沿用快照口径（「本人执行」/「接收人 N 人」）。

## Capabilities

### New Capabilities
- `review-detail-presentation`: 审批详情的分区展示与人员姓名呈现（请求人/审批人/执行人）。

### Modified Capabilities
（无）

## Impact

- 后端：`service/TaskReviewService.java`、`scripts/generate-openapi.py`、`openapi.json`、`src/test/.../BusinessApiIntegrationTests.java`（新增 `reviewDetailExposesRequestorAndRecipientNames`）。
- Android：`data/TaskRepository.kt`（`TaskReviewDto` 新增字段 + `ReviewActorDto`）、`ui/review/ReviewScreen.kt`（分区卡片、人员组件、队列卡片请求人）。
- 兼容性：新增字段均为可选，旧客户端 `ignoreUnknownKeys` 不受影响；旧数据无姓名时客户端回退显示 ID 后四位。
