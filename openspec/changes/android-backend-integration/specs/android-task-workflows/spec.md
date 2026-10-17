## ADDED Requirements


### Requirement: 任务创建与真实接收人
创建任务 SHALL 调用 `GET /tasks/recipient-candidates`（分页检索）获取真实 userId 候选并 `POST /tasks`：PERSONAL 锁定本人，DELEGATED 提交管理范围内 recipientIds 与 tagIds/primaryTagId；返回 taskId/reviewId/PENDING，展示为待审申请，不当作正式派发。

#### Scenario: 派发待审
- **WHEN** 管理者提交派发任务
- **THEN** 界面显示待审批状态，接收人在批准前看不到正式任务

### Requirement: 直属上级审批
审核人 SHALL 经 `GET /task-reviews?scope=pending|created` 与 `GET /task-reviews/{id}`（不可变快照）查看申请，经 `POST /task-reviews/{id}/decision` 提交 APPROVE/REJECT 与 review.expectedVersion；并发冲突按 409 处理，不自动换人或批准。

#### Scenario: 批准
- **WHEN** 指定审核人批准当前有效快照
- **THEN** 任务关系激活，从返回的最新 task/assignment 版本继续后续操作

### Requirement: 进度与完成分离
进度 SHALL 调用 `POST /task-assignments/{id}/progress-records`（assignment.expectedVersion，100% 不等于完成）；PERSONAL 完成调用 `POST /tasks/{id}/complete`（expectedAssignmentVersion）；DELEGATED 由接收人 `POST /task-assignments/{id}/completion-requests` 申请、创建者 `POST /task-completion-requests/{id}/decision` 逐人 ACCEPT/REJECT。

#### Scenario: 进度 100%
- **WHEN** 接收人提交 100% 进度
- **THEN** 关系仍为 ACTIVE，不产生完成事实

#### Scenario: 逐人验收
- **WHEN** 多接收人任务中一人申请完成
- **THEN** 创建者仅决定该 requestId 对应接收人的结果，其他接收人不受影响

### Requirement: 操作按授权显示
可执行按钮 SHALL 依据任务详情 allowedActions 与对应授权 assignmentId 显示；按钮隐藏不替代服务端授权，403 不重试绕过。

#### Scenario: 无权操作
- **WHEN** 当前用户对某关系无 COMPLETE_PERSONAL/REQUEST_COMPLETION
- **THEN** 不显示相应按钮，直接调用也按 403 处理
