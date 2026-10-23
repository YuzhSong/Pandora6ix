## Why

两处实际使用问题：

1. 编辑窗口按 `work_date` 判定（"只有今天的日志能改"），于是**当天补交的昨天/前天日志一提交就锁死**——刚写完就发现错字也改不了，用户只能留着错误内容。合理规则应是"创建当天内可改"，与补交的是哪一天无关。
2. 日志页筛选只有「全部日期」下拉，列出的是最近 31 天内出现过的日期文字，无法跨月查找历史日志，也没有"哪天写过、哪天没写"的直观视图。

## What Changes

- **编辑/删除窗口改为按创建时间（`published_at`）判断**，用公司业务时区换算成日期：当天创建的日志（含当天补交的往期日志）当天内可改可删；跨天即只读。`allowedActions` 与 `checkEditWindow` 用同一规则，错误信息改为 `Only journals created today can be edited or deleted.`
- **新增 `GET /work-logs/dates`**：返回本人全部有日志的业务日期（不受列表接口 367 天窗口限制），并附 `businessToday`，供客户端日历着色与月份区间计算。
- **日志日期选择器改为微信式全屏日历**：从最早有日志的月份连续排到当前月份（中间无日志的月份也显示），有日志黑色、无日志灰色、今天绿色；点左上角年月可展开年/月并跳转到该月；「全部日期」可清除筛选。
- 个人十大重要事项编辑界面补「正式事项」标题，与「候选事项」对称（并入 `polish-personal-and-journal-uis`）。

## Impact

- 后端：`JournalService`（`createdToday` / `checkEditWindow` / `journalView.allowedActions` / 新增 `dates`）、`JournalController`、`openapi.json`（60 operations）
- Android：`LogsScreen` 编辑窗口判断与文案、`JournalRepository.dates()`、新增 `ui/logs/JournalDatePicker.kt`、`PersonalImportantEditor`
- 测试：新增 `journalCreatedOnEarlierDayIsReadOnly`、`workLogDatesListEveryJournalDateForTheOwner`；改写 `journalsCreatedTodayStayEditableEvenForBackfilledDates`、`journalEditWindowUsesCreationDateInCompanyTimezone`、`journalEditWindowAndEntryDeletion`
