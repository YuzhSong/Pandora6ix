## 时间口径

服务端判定任务时间用的是企业 `business_timezone`（`/me` 返回），与日志时间、截止时间提醒判定同一口径。客户端此前混用设备时区（`todayDate()`）与固定默认值（09:00 / 10:00），是「显示时间与现实不一致」的根因。

新增 `data/BusinessTime.kt`：

- `businessTodayKey(timezone)`：业务时区当前 `YYYY-MM-DD`；
- `businessNowMinute(timezone)`：业务时区当前分钟数；
- `businessNowLabel(timezone)`：业务时区当前 `HH:mm`，用于表单提示；
- `deadlineNotPast(endKey, endMinute, allDay, timezone)`：全天任务 `endKey >= today`；指定时间任务 `end > today` 或（`end == today` 且 `endMinute > nowMinute`）。

全部走 `Calendar`（minSdk 24 无 `java.time`），未配置时区回退 UTC。

## 表单改动（`ui/calendar/TaskEntrySheet.kt`）

- 新增 `businessTimezone` 参数，由 `CalendarScreen` → `MainActivity` 从 `currentUser.businessTimezone` 传入。
- 默认开始/结束时间由当前业务时刻派生（开始 = now，结束 = now + 60，上限 23:59）；草稿恢复优先。
- `submit()` 在既有「结束晚于开始」校验之后增加截止时间校验。
- `TaskCreateDto.timeKind` 去掉默认值：`ApiClient` 的 `Json` 使用 `encodeDefaults = false`，值等于默认值的字段会被省略；`ALL_DAY` 正好等于原默认值，导致后端 `@NotNull` 校验失败并提示「请选择全天或指定时间」。

## 错误提示（`data/ApiClient.kt`）

- 422 `VALIDATION_FAILED`：字段级错误按 `FieldError.field` 映射中文，未知字段回退 `字段：原文`，保留排查信息；非字段级业务校验按服务端 message 关键词转换（同日不递增、HH:mm 格式、日期倒置、ALL_DAY 不带时刻）。
- 409 `WORKFLOW_POLICY_UNCONFIRMED`：按原因细分（direct-supervisor 未配置上级、reviewer 上级无效）。

> 注：跨天任务原本在这里被列为服务端拒绝项；用户已于 2026-10-18 确认放开，见变更 `add-cross-day-task-scheduling`，同日不递增改为 422 业务校验。

服务端仍是最终校验方，客户端校验只是提前拦住可预知的输入错误。

## 验证

`:app:compileDebugKotlin` 通过；真机按 spec 三个场景验收（过去截止被拦、默认时间=当前公司时间、失败提示可读）。
