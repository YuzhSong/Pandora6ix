# Tasks: add-theme-and-review-time-polish

## 1. 主题派生色

- [x] 1.1 `Theme.kt` 扩展 `AppTheme`（卡片 / 描边 / 强调 / 按钮）并补齐五套配色
- [x] 1.2 既有颜色名改为读取当前主题的可组合属性，新增 `HairLine` / `ThemeButton`
- [x] 1.3 分隔线、描边、实心按钮改用派生色
- [x] 1.4 主题定义从 `MainActivity` 上移到 `Theme.kt`
- [x] 1.5 深色按钮文字/图标统一白色：新增 `OnThemeButton`，12 处 `ThemeButton` 调用点补 `contentColor`

## 2. 文案与时间

- [x] 2.1 视图页固定返回按钮文案改为「回到今年」
- [x] 2.2 审批中心提交/决定时间按公司业务时区展示（业务时区透传到列表与详情）

## 3. 验证

- [x] 3.1 `:app:compileDebugKotlin` 通过
- [ ] 3.2 真机验收：五套主题下卡片/边框/按钮与背景层次清晰；审批时间等于真实提交时间
