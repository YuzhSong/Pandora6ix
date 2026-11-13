## Context

- 现状：`NotificationService.scanDeadlines`（15 分钟调度）只对「未来 24h 内」的截止时刻生成提醒，`remaining <= 0` 直接跳过；过期后无任何消息。客户端 `TaskReminder` 同样只提醒未来截止。
- 任务状态机不感知逾期：`work_status`/`execution_status`/`completion_status` 均不随截止时间变化，本变更维持这一设计。
- 通知去重：`(enterprise_id,user_id,dedupe_key)` 唯一索引 + 写入前存在性检查，重复触发天然幂等。

## Goals / Non-Goals

- Goals：逾期一次性消息；客户端只读「已逾期」标识；与截止提醒共用业务时区口径。
- Non-Goals：不改状态机、不限制逾期任务操作、不做逾期统计/自动顺延、不新增数据库对象。

## Decisions

1. **逾期消息并入 `scanDeadlines` 同一调度**：在现有扫描循环里对「截止时刻已过且未完成、且在 24h 窗口内」的执行关系发消息，避免第二个调度器与重复查询。
2. **去重键 `taskId|OVERDUE|<dueDate>`**：含截止日期，顺延截止后新日期重新武装；复用既有唯一索引，无需迁移。
3. **窗口取「已过期 ≤ 24h」**：与截止提醒的 24h 档位对称，首次部署不会把历史逾期任务全部轰炸一遍；历史逾期任务只靠客户端标识呈现。
4. **客户端判定纯展示层**：新增纯函数 `isTaskOverdue(endDate, endTime, completionStatus, timezone, nowMillis)`，放在 data 层（BusinessTime 旁），看板卡片、日历条目、汇总表三处复用；不进任务模型字段，避免与日历分页 DTO 耦合。
5. **完成/待验收不标逾期**：`completionStatus` 为 `AWAITING_ACCEPTANCE`/`COMPLETED` 时不显示，避免验收中的任务被误催办。

## Risks / Trade-offs

- 24h 窗口意味着服务端停机超过 24h 会漏发个别逾期消息 → 接受（客户端标识不依赖消息）。
- 客户端判定依赖设备时钟 → 与现有截止提醒口径一致，接受。

## Migration Plan

无迁移。上线即生效；历史逾期任务不补发消息。

## Open Questions

无。
