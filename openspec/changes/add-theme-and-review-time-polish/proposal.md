## Why

切换主题只改背景：卡片、边框、按钮、图标强调色仍是固定的暖米/橙色，换到薄荷、晴空等主题时组件与背景明显割裂。视图页的固定返回按钮文案仍是「回到今天」，但它在年视图下定位的是今年一月，与实际行为不符。审批中心「我提交的」「审批详情」把服务端 UTC 时间戳直接按字符串切片展示，显示的时间比真实提交时间早一个时区。

## What Changes

- 主题从「三色背景」扩展为成套派生色：背景 / 背景深 / 仪表盘底色 / 卡片底色 / 卡片嵌套层 / 描边分隔线 / 强调色 / 实心按钮底色。卡片、分隔线、边框、按钮与图标强调色 SHALL 随主题切换，且与背景**同色系但深浅不同**（不与背景同色，保证层次可见）。
- 既有调用点无需改名：`ui/theme/Theme.kt` 把 `Cream` / `CreamDeep` / `WarmCard` / `WarmDashboard` / `WarmOrange` 改为读取当前主题的可组合属性，并新增 `HairLine`（描边与分隔线）、`ThemeButton`（实心按钮底色）。
- 视图页固定返回按钮文案改为「回到今年」。
- 审批中心与审批详情的提交/决定时间 SHALL 按公司业务时区展示（复用 `businessDateTimeLabel`），不再是 UTC 时间戳切片。

## Capabilities

### New Capabilities
- `review-time-display`: 审批中心提交/决定时间按公司业务时区展示。

### Modified Capabilities
- `app-settings`: 切换主题时卡片、边框、按钮与强调色一并跟随。
- `calendar-view`: 固定返回按钮文案改为「回到今年」。

## Impact

- Android：`ui/theme/Theme.kt`（成套派生色 + 可组合属性）、`MainActivity.kt`（主题定义上移、分隔线/边框/按钮改用派生色、审批中心传入业务时区）、`ui/calendar/CalendarScreen.kt`（文案）、`ui/review/ReviewScreen.kt`（业务时区时间）、`ui/calendar/TaskEntrySheet.kt`、`ui/tags/TagManageScreen.kt`（描边与按钮改用派生色）。
- 品牌固定色（图表、标签配色）与文字色 `Ink` 不变，保证可读性。
- 后端不变。
