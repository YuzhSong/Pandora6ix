## 1. 后端编辑窗口

- [x] 1.1 `JournalService.createdToday()`：按 `published_at` 换算公司业务时区日期判断创建当天
- [x] 1.2 `journalView.allowedActions` 与 `checkEditWindow` 改用该规则，错误文案改为 `Only journals created today can be edited or deleted.`
- [x] 1.3 更新受影响的测试：当天补交可改、跨天只读、时区边界、`journalEditWindowAndEntryDeletion`

## 2. 后端日期集合接口

- [x] 2.1 `GET /work-logs/dates` 返回本人全部有日志日期 + `businessToday`
- [x] 2.2 `generate-openapi.py` 增加 `JournalDates` 与路由（顺带补上 dev 缺失的 `/task-reviews/pending-count`），重生成 `openapi.json`（60 operations）
- [x] 2.3 新增测试 `workLogDatesListEveryJournalDateForTheOwner`

## 3. Android 日志页

- [x] 3.1 `JournalRepository.dates()` + `JournalDatesDto`
- [x] 3.2 `LogsScreen` 编辑窗口判断改用 `allowedActions`（不再看 `workDate == today`），文案同步
- [x] 3.3 新增 `ui/logs/JournalDatePicker.kt`：全屏、月份连续、黑/灰/绿着色、年月跳转
- [x] 3.4 用全屏日历替换「全部日期」下拉；筛选键改为日期（iso）而非展示文案

## 4. 个人事项编辑界面

- [x] 4.1 加「正式事项」标题，与「候选事项」对称（并入 `polish-personal-and-journal-uis`）

## 5. 验证

- [x] 5.1 后端全量 `mvn test` 97 项通过
- [x] 5.2 `:app:compileDebugKotlin` 通过
- [ ] 5.3 真机验收：当天补交的日志当天可改；日期选择器跨月连续、着色正确、年月跳转正常
