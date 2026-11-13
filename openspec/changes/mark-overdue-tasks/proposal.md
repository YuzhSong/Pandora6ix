## Why

任务截止时间已过时系统既不提示也不标记：US29 的截止提醒只覆盖截止前 24 小时，过期后服务端与客户端都静默，看板、日历、汇总表中逾期任务与正常任务无差别展示，管理者难以一眼识别需要催办或顺延的任务。

## What Changes

- 服务端截止扫描新增逾期消息：任务截止时刻过后仍未完成时，向执行人发送一条「任务已逾期」消息；同一任务同一截止日期只发一条（去重键含截止日期，顺延截止时间后重新武装）；已进入验收/已完成/已驳回的任务不发。
- Android 首页任务看板、日历视图条目、公司概览任务汇总表为「未完成且截止时刻已过」的任务显示红色「已逾期」标识；判断基于公司业务时区。
- 不改变任务状态机：逾期不自动完成、不自动驳回、不限制完成/验收操作，顺延与关闭仍由创建者通过既有编辑审批决定。

## Capabilities

### New Capabilities

- `task-overdue-marking`: 逾期任务的识别口径、一次性逾期消息与客户端「已逾期」标识。

### Modified Capabilities

无。

## Impact

- backend：`NotificationService` 扫描逻辑（新增逾期分支，复用通知去重索引）；`BusinessApiIntegrationTests` 新增逾期消息测试。
- android：`HomeTaskBoard.kt`、`CalendarLayout.kt`、`TaskSummaryTable.kt` 展示层标识；逾期判定辅助函数与单元测试。
- 无新增业务接口、无数据库迁移（复用 notification 表与去重唯一索引）；不部署生产环境。
