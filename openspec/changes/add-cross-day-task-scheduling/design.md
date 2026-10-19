## 决策与范围

需求文档 V1.3 的 Q-04 涉及三项：同刻起止、跨日定时端点、企业业务时区。本轮定案的是**前两项的口径**：

| Q-04 子项 | 结论 |
|---|---|
| 跨日 TIMED 端点 | **放开**：区间由 `startDate+startTime` → `endDate+endTime` 表达 |
| 同刻 / 不递增（同日） | **不允许**：`endTime <= startTime` 按 422 拒绝 |
| 企业业务时区 | 不变：由管理端显式配置，日志/提醒/截止判定均按业务时区 |

业务时区这一维度仍是已实现但由配置决定的能力，不随本次改动变化。

## 为什么不用改表结构

原本 `task` 表已经有 `start_date / end_date / time_kind / start_minute / end_minute`：

```
task(start_date, end_date, time_kind, start_minute, end_minute)
10-18, 10-19, TIMED, 1350(22:30), 135(02:15)   ← 天然可表达 22:30 → 次日 02:15
```

即 start 侧与 end 侧各带自己的日期与分钟，组合起来就是明确的时间区间；不需要 `start_at/end_at` 时间戳列。日历端 `CalendarLayout.timeRangeOnDate()` 也早已按「首日 `startMinute`→24:00、中间日全天、末日 0→`endMinute`」计算每日时段，`TaskItem(start, end, startMinute, endMinute)` 的连续日期条渲染（`layoutContinuousRows`）对多天任务同样成立——所以数据库侧与移动端都不需要结构性改动。

`NotificationService.dueInstant()` 取 `end_date + end_minute`（ALL_DAY 按当日 23:59）换算业务时区截止时刻，跨天后自动落在最后一天的正确时刻，无需修改。

## 后端改动（`TaskService.time()`）

```java
if (end.isBefore(start)) throw BusinessException.invalid("endDate must not precede startDate.");
...
if (start.equals(end) && to <= from)
    throw BusinessException.invalid("TIMED end must be after start on the same day.");
return new int[] {from, to};
```

只从 `!start.equals(end) || to <= from → 409 policy` 改为「同日才校验正时长」的 422：

- 跨天不再进 Unconfirmed 分支 → 状态码由 409 变 201/200
- 同日不递增从「政策未确认」变为「明确非法输入」→ 409 `WORKFLOW_POLICY_UNCONFIRMED` 变 422 `VALIDATION_FAILED`

## Android 改动

- 表单校验 `validTaskInterval()` 本就允许跨天（`end > start || endMinute > startMinute`），日历渲染亦支持，故**不改逻辑**。
- `ApiClient.friendlyMessage()`：删除「暂不支持跨天任务」的 Q-04 映射（后端不再返回），新增 `VALIDATION_FAILED` 分支——字段级错误仍优先按 `fieldErrors` 映射，其次按服务端 message 关键词转换（同日不递增 / 日期倒置 / HH:mm 格式 / ALL_DAY 不带时刻）。
- 「补充今日工作」按钮颜色与视图页「回到今年」统一为实心 `ThemeButton`。

## 验证

- `mvn -q -pl backend test`（含 `BusinessApiIntegrationTests`）：跨天 TIMED 期望 201、同日同刻/不递增期望 422、ALL_DAY 跨多天落库校验。
- `:app:compileDebugKotlin` 通过。
