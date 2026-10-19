## Why

Android 端「设置 → 通知提醒」（新任务 / 截止时间临近 / 任务状态变化）目前是纯客户端实现：登录后每 15 分钟拉 `GET /tasks?scope=mine` 与本地快照比对派生提醒，偏好与已提醒状态存本地 SharedPreferences。它没有服务端事实来源：App 未打开时不产生提醒、换设备后偏好与去重状态丢失、截止时间判定由客户端自行按业务时区换算，容易与服务端口径不一致。

文档 V1.3 `FR-NOTIFY-01`（P1）要求「系统可提供新任务及任务截止时间提醒」，`US29` 要求「已完成任务不得继续产生截止提醒」。后端目前没有任何通知数据与接口（`backend/openapi.json` 只有 `/me`、`/tasks`、`/calendar`、`/work-logs`、`/statistics` 等），因此提醒无法在服务端沉淀、也无法被 Web 或其他端复用。本变更在后端补齐通知的数据、生成与读取接口。

按 `FR-NOTIFY-02`，导图消息栏与消息服务本轮仍只预留、不实现；本变更不做推送通道与消息中心界面。

## What Changes

- 新增 `notification` 表（Flyway `V4__notifications.sql`）：企业隔离、本人可见、`dedupe_key` 唯一索引承担去重。
- 任务审批批准并激活执行关系时，在同一事务内为各接收人生成 `NEW_TASK` 通知；个人任务只给本人。
- 验收决定（ACCEPT/REJECT）、个人完成、进度更新等改变执行状态或审批状态的写操作，为相关人生成 `STATUS` 通知。
- 新增截止时间扫描：按企业业务时区计算 `(业务现在, 业务现在 + 提前量]` 窗口，为未完成任务生成 `DEADLINE` 通知；提前量档位化以保证去重；已完成任务不再产生截止提醒（US29）。
- 新增接口：`GET /notifications`（游标分页）、`GET /notifications/unread-count`、`POST /notifications/{id}/read`（幂等）。
- `openapi.json` 同步；集成测试覆盖生成、去重、时区边界、跨企业隔离、已完成任务不提醒。
- 明确不做：FCM / 厂商推送与 `POST /devices`、提醒偏好跨设备同步（2.6.9 设置本地保存）、消息中心界面。

## Capabilities

### New Capabilities
- `backend-notifications`: 服务端通知记录的生成规则（审批生效、执行状态变化、截止时间扫描）、查询与已读接口，以及去重、业务时区与可见性约束。

### Modified Capabilities
<!-- 任务审批/验收的核心业务行为不变，通知只是派生记录，因此不修改其需求 -->

## Impact

- backend：`db/migration/V4__notifications.sql`、`NotificationService`（生成与扫描）、`NotificationController`、`TaskService` / `TaskReviewService` 写路径埋点。
- `backend/openapi.json`：三个通知接口与字段。
- 集成测试：`BusinessApiIntegrationTests` 风格补充。
- Android 端后续改为轮询 `/notifications` 替代本地快照比对（另开 change `add-notification-reminders-android`）。
