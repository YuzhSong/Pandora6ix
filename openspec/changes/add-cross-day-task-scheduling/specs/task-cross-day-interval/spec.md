## ADDED Requirements

### Requirement: 任务时间区间允许跨天

任务的时间区间 SHALL 以 **`startDate + startTime` 为起点、`endDate + endTime` 为终点** 表达，而不是「同一天内的分钟区间」：全天任务与精确到分钟的任务 SHALL 都允许 `endDate > startDate`（跨天）。
仅当 `startDate == endDate` 时 SHALL 要求结束时刻严格晚于开始时刻（拒绝同刻与不递增）；`endDate < startDate` SHALL 始终拒绝。ALL_DAY 任务 SHALL NOT 携带时刻，跨多天由日期区间表达。

#### Scenario: 分钟级任务跨天
- **WHEN** 用户创建开始 `10-18 22:30`、结束 `10-19 02:15` 的任务
- **THEN** 创建成功并进入审批，日历在首日渲染 22:30–24:00、次日渲染 00:00–02:15

#### Scenario: 全天任务跨多天
- **WHEN** 用户创建开始 `10-18`、结束 `10-20` 的全天任务
- **THEN** 创建成功，`start_minute` / `end_minute` 为空，日历显示跨三天的连续日期条

#### Scenario: 同一天仍需正时长
- **WHEN** 用户在同一天填写开始 `09:15`、结束 `09:15` 或结束 `08:00`
- **THEN** 服务端返回 422 `VALIDATION_FAILED`，客户端提示「同一天的任务，结束时间必须晚于开始时间」

#### Scenario: 结束日期倒置
- **WHEN** 用户把结束日期选在开始日期之前
- **THEN** 服务端拒绝（422），并提示「结束日期不能早于开始日期」
