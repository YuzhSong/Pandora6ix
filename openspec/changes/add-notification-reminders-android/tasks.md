# Tasks: add-notification-reminders-android

## 1. 设置页

- [x] 1.1 「通知提醒」提升为一级分区（与通用/数据/关于同级）：主开关 + 三个同级分项，行样式与「通用」一致、相邻行有分隔线
- [x] 1.2 「提前提醒时间」作为「截止时间临近」的缩进子项：预设 1/2/6/12/24 小时 + 小时/分钟自定义（1 分钟 – 72 小时，越界对话框内提示）
- [x] 1.3 主开关**默认开启**；关闭时只保留主开关行，不展示分项也无说明文字
- [x] 1.4 `NotificationPrefs` 本地持久化（按账号隔离），我的页显示开启/关闭

## 2. 提醒与权限

- [x] 2.1 `NotificationHelper`：通知通道、权限检查、发通知
- [x] 2.2 Manifest 声明 `POST_NOTIFICATIONS`；提醒开启且未申请过时申请一次（默认开启 → 登录后申请），记录 `permissionRequested` 避免重复打扰
- [x] 2.3 `TaskReminder`：任务快照比对，派生新任务/截止/状态变化三类事件并去重
- [x] 2.4 `ReminderDialog` 页内弹窗逐条展示；有权限时同时发系统通知
- [x] 2.5 主开关默认开启（用户实测默认关闭导致"完全没有提醒"）；前台轮询间隔 15 分钟 → 60 秒
- [ ] 2.6 后台提醒（应用不在前台也能收到系统通知）：需要 WorkManager + 新依赖，尚未实现

## 3. 日志时间

- [x] 3.1 新增 `data/BusinessTime.kt`：UTC → 公司业务时区换算
- [x] 3.2 日志条目时间改用 `businessTimeLabel(recordedAt, businessTimezone)`

## 4. 验证

- [x] 4.1 `:app:compileDebugKotlin` 通过
- [ ] 4.2 真机验收：分区与子开关、自定义提前量、开启时权限请求、弹窗与去重、日志时间与现实一致
- [ ] 4.3 后端 `/notifications` 就绪后改为消费该接口（见 add-notification-backend 4.3）
