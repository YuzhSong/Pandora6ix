## 偏好与存储

`NotificationPrefs` 用 SharedPreferences（文件名带 `ownerId`，按账号隔离）保存主开关、三个子开关与提前量（`leadMinutes`，单位分钟），默认值：主开关 false、新任务 true、截止 true、状态变化 false、提前量 1440（24 小时）。同时保存任务状态快照（`任务id|执行关系id` → `执行状态|审批状态`）与已提醒事件键集合，用于识别新增/变化与去重。

设置页展示：三个分项与「通知提醒」同级，复用 `SettingsRow`（圆形图标 + 标题 + 副标题 + 开关），相邻行用 `DashedDivider` 分隔；「提前提醒时间」是「截止时间临近」的缩进子项（`SubValueRow`），点击弹窗含 1/2/6/12/24 小时预设与「小时 + 分钟」自定义输入（总范围 1 分钟 – 72 小时），展示值由 `leadLabel()` 格式化（`24 小时` / `1 小时 30 分钟` / `45 分钟`）。主开关关闭时只保留主开关行，不展示任何说明文字。

## 权限时机

`POST_NOTIFICATIONS` 在 Manifest 声明，但只在设置页主开关由关变开时通过 `ActivityResultContracts.RequestPermission` 申请；轮询启动时不再申请。`NotificationHelper.notify` 在发通知前检查权限，未授权直接跳过（页内弹窗不受影响）。Android 13 以下只看系统通知开关。

## 提醒产生（后端暂无通知接口）

`TaskReminder.collectReminders` 每 15 分钟读取 `GET /tasks?scope=mine`，与快照比对：

- 首次运行（快照为空）只写基线，不产生提醒，避免把存量任务当成新任务；
- 新出现的执行关系且任务已批准 → 新任务提醒；
- 同一执行关系的状态串变化 → 状态变化提醒；
- 任务截止时刻（ALL_DAY 按当日 23:59，TIMED 按 endMinute，均在公司业务时区）落在未来提前量窗口内且执行关系未完成 → 截止提醒（已完成/已取消、已驳回跳过，US29）；
- 事件键写入已提醒集合，保证只提醒一次。

`add-notification-backend` 完成后改为读 `GET /notifications`，用返回的 `dueAt` 按本地提前量决定展示时机，去重由服务端 `dedupe_key` 承担。

## 展示

事件进入队列，`ReminderDialog` 逐条展示：标题为提醒类型，正文含任务名称与事实，按钮「知道了」，确认后展示下一条；同时（有权限时）发一条系统通知。

## 日志时间

服务端时间戳统一为 UTC（`BusinessDb.view()` 输出 `Instant.toString()`）。新增 `data/BusinessTime.kt` 用 `Calendar`（minSdk 24，无 `java.time`）按 `/me` 返回的 `businessTimezone` 换算后格式化 `HH:mm`；时区未配置时回退 UTC，不出现空白或崩溃。

## 验证

Android 端 `:app:compileDebugKotlin` 通过；设置分区、子开关与自定义提前量、权限时机、弹窗与去重需真机验收（`add-notification-backend` 的 4.3 完成后再联调 `/notifications`）。
