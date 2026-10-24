## Why

需求文档 V1.3 把「跨日定时端点」列入未决项 Q-04（第 1357、1585、1643 行），此前实现按「未确认」处理：后端 `TaskService.time()` 对 `startDate != endDate` 的 TIMED 任务直接抛 409 `WORKFLOW_POLICY_UNCONFIRMED`（"Cross-day ... await Q-04 confirmation."），移动端也有对应的「暂不支持跨天任务」提示。

**用户已于 2026-10-18 决策：全天任务与精确到分钟的任务都要能跨天选择。** 这是 Q-04 中「跨日」维度的定案（同刻/不递增的规则同时明确：同日仍需结束晚于开始）。数据模型本来就能表达跨天——`start_date + start_minute` 是区间起点、`end_date + end_minute` 是区间终点，日历也早已按「首日 startMinute→24:00、中间整天、末日 0→endMinute」渲染连续日期条，因此不需要改表结构，只需放开服务端口径并同步各处提示与文档。

## What Changes

- 后端：任务时间区间的**起点是 `startDate+startTime`、终点是 `endDate+endTime`**，跨天合法（`22:30 → 次日 02:15` 允许）；同日仍必须「结束时刻晚于开始时刻」，违反由 409 政策拒绝改为 422 `VALIDATION_FAILED`。
- Android：原本的表单校验（`validTaskInterval`）与日历渲染已支持跨天，本次只更新错误提示口径——去掉「暂不支持跨天任务」文案，改为按服务端 code/message 细分（同一天结束需晚于开始、HH:mm 格式、日期倒置、ALL_DAY 不带时刻）。
- 文档：`backend/README.md` 与 `backend/ANDROID_INTEGRATION.md` 的 Q-04 行更新为「跨天已确认放开」，同日不递增按 422 拒绝。

## Capabilities

### New Capabilities
- `task-cross-day-interval`: 任务时间区间允许跨天（全天与分钟任务），同日须正时长。

### Modified Capabilities
（无：主规格中暂无对应 capability，跨天支持作为新能力登记）

## Impact

- 后端：`service/TaskService.java`（`time()` 口径与错误类型）、`src/test/.../BusinessApiIntegrationTests.java`（原「跨天 409」断言改为「跨天 201」+「同日不递增 422」，并新增全天跨多天用例）。
- Android：`data/ApiClient.kt`（错误提示按 VALIDATION_FAILED / WORKFLOW_POLICY_UNCONFIRMED 细分）。
- 数据库结构不变；截止提醒 `NotificationService.dueInstant` 本就取 `end_date + end_minute`，跨天后截止时刻自动正确。
- 文档：`backend/README.md`、`backend/ANDROID_INTEGRATION.md`。
