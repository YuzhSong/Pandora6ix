## Why

文档 V1.3 `FR-NOTIFY-01`（P1）与 `US29` 要求系统提供新任务与临近截止时间提醒，且已完成任务不得继续提醒。在此之前 Android 端只有设置里一个没有实际作用的「通知提醒」开关（`app-settings` 仅规定"可切换开启或关闭"），没有任何提醒行为。后端本轮没有消息服务（见 `add-notification-backend` 之前的状态），因此先在客户端以真实任务数据派生提醒，并把开关细化为文档 2.4.2（5）的三类：新任务提醒、截止时间提醒、任务状态变化提醒。

同时修复日志时间展示：服务端时间戳是 UTC，客户端直接切片显示会把 UTC 当成本地时间，与公司业务时区差 8 小时，违反"展示真实记录时间"的既有约定。

## What Changes

- 设置页把「通知提醒」提升为与「通用」「数据」「关于」并列的一级分区：主开关 + 三个子开关 + 提前提醒时间（预设 1/2/6/12/24 小时，可在 1–72 范围内自定义）。
- 提醒**默认开启**（装好即生效）；系统通知权限在提醒开启且未申请过时申请一次（Android 13+ `POST_NOTIFICATIONS`），被拒不影响页内弹窗。
- 主开关关闭时全部静默，不启动任务比对，不展示子开关。
- 任务比对仅在前台按 60 秒间隔运行；退到后台或进程被杀后不产生提醒（无后台调度，见"已知限制"）。
- 提醒以页内弹窗为主（逐条确认），已授予通知权限时同时发系统通知；同一事件只提醒一次。
- 后端暂无通知接口：客户端按固定间隔读取本人任务快照比对派生事件；`add-notification-backend` 完成后改为读 `/notifications`。
- 日志条目时间按公司业务时区换算后展示（UTC → businessTimezone），未配置时区时回退 UTC。

## Capabilities

### New Capabilities
- `notification-reminders`: 客户端提醒的偏好（主开关、三类子开关、提前量与自定义）、产生规则、页内弹窗与系统通知、权限时机与去重。

### Modified Capabilities
- `app-settings`: 「通知提醒开关」需求从"仅可切换开关"改为"默认开启的一级分区，含三个子开关与可自定义的提前量"。

## Impact

- Android：`MainActivity.kt`（设置分区与弹窗、权限申请、轮询）、新增 `NotificationHelper.kt`、`data/NotificationPrefs.kt`、`data/TaskReminder.kt`、`data/BusinessTime.kt`、`AndroidManifest.xml`（`POST_NOTIFICATIONS`）。
- 后端只读接口不变（复用 `GET /tasks?scope=mine` 与 `GET /me` 的 `businessTimezone`）。
- 后续 `add-notification-backend` 落地后，客户端改为消费 `/notifications`，本变更的去重与轮询逻辑随之替换。
