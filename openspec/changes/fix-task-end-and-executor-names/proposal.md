## Why

两个使用中的问题：新建任务时把开始时间调到结束时间之后，结束时间不会跟着动，提交时才提示错误；任务汇总列表的执行人头像对不在本人下属名单里的成员显示“成”（来自占位名“成员 xxxx”），而不是真实姓名首字。

## What Changes

- 新建/编辑任务时，修改开始日期或时间后若结束不再晚于开始，结束按原时长自动顺延（无有效时长时按 1 小时）；用户已设好的更晚结束不变。重复任务的每次时段限制在同一天内。
- 任务汇总的执行人头像与名称优先使用任务执行记录中的真实姓名（`recipientName`），找不到时才回退到下属名单与占位名。

## Capabilities

### New Capabilities
- `task-entry-end-follows-start`：任务录入时结束时间跟随开始时间的规则。
- `summary-executor-names`：任务汇总中执行人姓名的取值来源。

### Modified Capabilities

无。任务时间校验、提交与汇总统计规则不变。

## Impact

仅 Android：`ui/calendar/CalendarLayout.kt`、`TaskEntrySheet.kt`、`ui/summary/SummaryData.kt`、`TaskSummaryFlow.kt` 与单元测试。
