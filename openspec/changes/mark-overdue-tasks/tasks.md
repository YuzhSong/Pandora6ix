## 1. Backend

- [x] 1.1 `NotificationService.scanDeadlines` 新增逾期分支：截止时刻已过 ≤24h、执行关系 ACTIVE 且任务未完成（非 AWAITING_ACCEPTANCE/COMPLETED/驳回）时，按 `taskId|OVERDUE|<dueDate>` 向每位当前接收人发「任务已逾期」消息（kind 沿用 DEADLINE 满足表 CHECK，event_type=OVERDUE）
- [x] 1.2 `BusinessApiIntegrationTests` 新增测试：截止过期后扫描产生逾期消息、重复扫描不重复、提交完成后不再产生；顺延截止后按新日期重新武装由去重键构造保证（未做集成测试）

## 2. Android

- [x] 2.1 新增逾期判定纯函数（业务时区口径，`AWAITING_ACCEPTANCE`/`COMPLETED` 不算逾期）并编写单元测试
- [x] 2.2 首页任务看板卡片显示「已逾期」标识（`MainActivity.kt` 面板状态，错误色）
- [x] 2.3 日历视图条目显示「已逾期 · 」前缀（`CalendarScreen.kt`，按业务时区判定）
- [x] 2.4 公司概览任务汇总表「完成情况」列显示「已逾期」（`TaskSummaryTable.kt` / `SummaryComponents.kt`，错误色调）

## 3. Validation

- [x] 3.1 后端 `mvnw test` 全量通过（177 tests）
- [x] 3.2 `openspec validate mark-overdue-tasks --strict` 通过
- [ ] 3.3 Android 单元测试在 IDE（JDK 21）中运行 `OverdueMarkerTest`（本机 Gradle 因 JBR 缺少 jlink 无法执行 javaCompile，`compileDebugKotlin` 已通过）
