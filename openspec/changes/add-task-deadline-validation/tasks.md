# Tasks: add-task-deadline-validation

## 1. 时间口径

- [x] 1.1 `BusinessTime.kt` 新增 `businessTodayKey` / `businessNowMinute` / `businessNowLabel` / `deadlineNotPast`

## 2. 表单

- [x] 2.1 `TaskEntrySheet` 接收 `businessTimezone`，默认开始/结束取当前业务时刻
- [x] 2.2 「开始与结束」区显示「当前公司时间 HH:mm」
- [x] 2.3 `submit()` 增加截止时间不得早于当前业务时刻的校验
- [x] 2.4 `CalendarScreen` → `MainActivity` 透传当前用户业务时区

## 3. 提示

- [x] 3.1 422 字段校验错误按字段给出中文提示
- [x] 3.2 错误提示按后端 code 细分：422 字段级 + 业务原因（同日不递增 / HH:mm / 日期倒置）、409 流程策略（未配置直属上级 / 上级无效）

## 4. 验证

- [x] 4.1 `:app:compileDebugKotlin` 通过
- [ ] 4.2 真机验收三个场景：过去截止被拦、默认时间等于当前公司时间、失败提示可读
