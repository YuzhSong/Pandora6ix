# Tasks: polish-review-detail-actors

## 1. 后端提供人员档案

- [x] 1.1 `TaskReviewService.userIndex()`：一次查全量 userId → displayName + jobTitle
- [x] 1.2 `reviewView()` 输出 `submittedByName` / `submittedByJobTitle` / `reviewerName` / `reviewerJobTitle` / `recipients[]`
- [x] 1.3 `recipients()` 取任务当前执行关系，姓名/职位缺失给 null
- [x] 1.4 `generate-openapi.py` 的 `TaskReview` schema 同步 + 重生成 `openapi.json`
- [x] 1.5 新增集成测试 `reviewDetailExposesRequestorAndRecipientNames`
- [x] 1.6 `recipients()` 改为取提交快照的 `recipientIds`，快照缺失才回退当前执行关系；新增 `reviewDetailExecutorsCarryNamesAndFollowSubmittedSnapshot`

## 2. Android 详情改版

- [x] 2.1 `TaskReviewDto` 增加五个字段 + `ReviewActorDto`（含 jobTitle）
- [x] 2.2 `SectionCard` 分区：任务信息 / 时间安排 / 人员 / 标签 / 审批结果
- [x] 2.3 人员组件：`RemoteAvatar` / `PersonRow` / `PersonChip`，显示真实姓名 + 职位 + 头像
- [x] 2.4 重要程度复用 `priorityMeta()` 彩色星级
- [x] 2.5 队列卡片显示「类型 · 请求人 姓名」
- [x] 2.6 姓名缺失回退 `成员 <ID 后四位>`；无 recipients 时回退快照口径；头像缺失回退首字
- [x] 2.7 审批中心与详情背景改用 `WarmDashboard`，与设置页同色
- [x] 2.8 标签胶囊改用服务端 `colorHex` 真实颜色（与任务详情、日志一致），不再用主题奶油色

## 3. 验证

- [x] 3.1 后端全量 `mvn test` 93 项通过
- [x] 3.2 `:app:compileDebugKotlin` 通过
- [ ] 3.3 真机验收：分区层次清晰、请求人/审批人/执行人姓名职位与头像正确、背景与设置页一致
