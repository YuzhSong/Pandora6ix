## 数据

新增 Flyway `V4__notifications.sql`：`notification(id, enterprise_id, user_id, kind, task_id, assignment_id, title, body, dedupe_key, due_at, created_at, read_at)`，主键 `id`，企业外键与 `UNIQUE(enterprise_id, user_id, dedupe_key)` 承担去重，`ix_notification_user(enterprise_id,user_id,created_at DESC)` 支撑列表与未读数。`due_at` 仅 `DEADLINE` 使用，供客户端按自己的提前量阈值决定是否展示（阈值仍按 2.6.9 存本地，服务端不做偏好同步）。

写入统一走 `INSERT INTO notification(...) SELECT ... WHERE NOT EXISTS (SELECT 1 FROM notification WHERE enterprise_id=? AND user_id=? AND dedupe_key=?)`：并发或重复触发只产生一条，不抛异常、不破坏外层任务事务。

## 生成

- **NEW_TASK**：`TaskReviewService.decide` APPROVE 激活执行关系后，为每条被激活的 `task_assignment` 的接收人写一条，去重键 `taskId|assignmentId|NEW_TASK`；REJECT 不写。
- **STATUS**：`TaskService.progress`（去重键含 `progressPercent`，同进度不重复）、`TaskService.complete`（个人完成，通知本人）、`TaskService.requestCompletion`（申请验收，通知创建人）、`TaskService.decide`（ACCEPT/REJECT，通知接收人与创建人）。
- 通知与业务写操作在同一 `@Transactional` 内落库：业务失败回滚则通知一并回滚。

## 截止时间扫描

`NotificationService.scanDeadlines()` 由 `@Scheduled` 每 15 分钟触发（`@EnableScheduling` 加在 `Pandora6ixBackendApplication`），同时保留 public 方法供测试直接调用。

- 逐企业读取 `enterprise.business_timezone`（未配置则跳过，不因此失败）；`now = Instant.now()`。
- 候选任务：未被驳回、`end_date` 在业务时区「今天-1」到「今天+2」之间（避免全表扫描）。
- 截止时刻：`LocalDate(end_date)` + `time_kind='TIMED' ? end_minute : 1439` 在该业务时区换算为 `Instant`。
- 条件：`0 < due - now <= 24h` 且存在 `execution_status NOT IN ('COMPLETED')` 的执行关系 → 为每个未完成接收人写 `DEADLINE`，去重键 `taskId|DEADLINE`，`due_at = due`。
  - 服务端只按最大档位 24 小时生成一次，客户端按自己的阈值（含自定义小时数）用 `due_at` 决定是否弹窗，避免任意阈值导致去重键爆炸。
  - 已完成/已取消的执行关系、已驳回任务、已过期的截止不生成（US29）。

## 接口

`NotificationController`（`/api/v1`）：

- `GET /notifications?cursor=&limit=`：复用 `Pages.sqlPage`，游标绑定 `enterpriseId:userId:notifications`，按 `created_at,id` 倒序。
- `GET /notifications/unread-count`：返回 `unreadCount`。
- `POST /notifications/{id}/read`：先按 `(enterprise_id,id)` 查，非接收人返回 404（`BusinessException.missing()`），已读重复调用返回同一视图。

响应字段沿用 `BusinessDb.view()` 的驼峰规则（`readAt`、`dedupeKey` 等），时间戳与既有接口一致返回 `Instant.toString()`（UTC）。

## 不做

推送通道（FCM/厂商）、`POST /devices`、偏好同步、消息中心界面（FR-NOTIFY-02 本轮不实现）。因此 App 关闭期间不产生系统推送，下次打开通过 `/notifications` 补齐。

## 验证

新增 `NotificationApiTests`（`@SpringBootTest` + MockMvc，沿用 `BusinessApiIntegrationTests` 的企业初始化/登录夹具风格）：批准派发任务产生 NEW_TASK、重复触发不重复、验收产生 STATUS、扫描对临期未完成任务产生 DEADLINE、已完成/已驳回不产生、他人读取 404、未读数随已读递减、跨企业不可见。
