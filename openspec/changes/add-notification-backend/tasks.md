# Tasks: add-notification-backend

## 1. 数据

- [x] 1.1 新增 `V4__notifications.sql`：notification 表（企业隔离、dedupe_key 唯一、due_at、read_at、索引）

## 2. 服务

- [x] 2.1 `NotificationService`：去重写入（存在性检查 + 唯一索引）、list / unreadCount / read
- [x] 2.2 审批 APPROVE 激活执行关系后生成 NEW_TASK（TaskReviewService.decide）
- [x] 2.3 progress / complete / requestCompletion / decide 生成 STATUS（TaskService 四个写路径）
- [x] 2.4 `scanDeadlines()`：业务时区窗口、未完成执行关系、24h 最大档位去重、跳过已完成与已驳回
- [x] 2.5 `Pandora6ixBackendApplication` 开启 `@EnableScheduling`

## 3. 接口

- [x] 3.1 `NotificationController`：`GET /notifications`、`GET /notifications/unread-count`、`POST /notifications/{id}/read`
- [x] 3.2 `openapi.json` 同步三个接口与字段（脚本生成 + `--check` 校验通过）

## 4. 验证

- [x] 4.1 `BusinessApiIntegrationTests`：新任务通知、去重、状态通知、截止扫描、已完成不提醒、跨用户 404、未读数与已读（53 个用例全绿）
- [x] 4.2 `mvnw test` 通过；`openspec validate add-notification-backend --strict`
- [ ] 4.3 Android 改为读 `/notifications`（另开 change `add-notification-reminders-android`，本变更不做）
