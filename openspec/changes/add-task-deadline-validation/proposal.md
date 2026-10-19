## Why

添加任务时可以填出已经过去的截止时间：表单只校验「结束晚于开始」，不校验是否还在未来；而且开始/结束时间固定默认 09:00 / 10:00，与公司业务时区的当前时间无关，用户看到的时间与现实不一致。服务端按企业业务时区判定任务时间，客户端用设备时区与固定默认值，两边口径不一致。

另外提交失败时提示不可读：后端 422 的字段校验消息是英文原文（如 `must not be null`），409 的流程策略拦截统一显示「流程策略未确认，请联系管理员」，用户无法知道是时间不递增，还是账号没有配置直属上级。

## What Changes

- 指定时间任务的开始/结束**默认取当前业务时刻**（开始=现在，结束=现在 +1 小时，最晚 23:59），不再固定 09:00 / 10:00。
- 提交前校验截止时间：全天任务结束日期不得早于业务今天；指定时间任务结束时刻必须严格晚于业务当前时刻。不通过时提示「截止时间不能早于当前时间（公司时间 HH:mm）」。
- 业务时区相关的「今天 / 现在」统一走 `data/BusinessTime.kt`，与日志时间、提醒判定同一口径；未配置时区时回退 UTC。
- 错误提示本地化：422 按字段给出中文（`请选择结束日期` 等，未知字段保留字段名便于排查），并按服务端 message 转换业务原因；409 `WORKFLOW_POLICY_UNCONFIRMED` 按原因细分（未配置直属上级 / 上级不是有效审批人）。

## Capabilities

### New Capabilities
- `task-entry-validation`: 添加任务表单的时间校验、默认值口径与失败提示。

### Modified Capabilities
<!-- 既有能力的需求不变：仅在客户端补校验与提示 -->

## Impact

- Android：`ui/calendar/TaskEntrySheet.kt`（默认值 + 校验 + 当前公司时间）、`ui/calendar/CalendarScreen.kt`（透传 businessTimezone）、`MainActivity.kt`（传入当前用户业务时区）、`data/BusinessTime.kt`（业务时区 now / today / 截止判定）、`data/ApiClient.kt`（错误提示本地化）。
- 后端不变；服务端仍为最终校验方（不递增时间、直属上级配置仍由后端拦截）。跨天支持见变更 `add-cross-day-task-scheduling`。
