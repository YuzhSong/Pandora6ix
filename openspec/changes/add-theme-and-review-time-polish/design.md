## 主题派生色

原来 `AppTheme` 只有 `background / backgroundDeep / dashboard` 三色，卡片、边框、按钮、强调色是硬编码的品牌色（暖米 + 橙），换主题后组件与背景割裂。

改造集中在 `ui/theme/Theme.kt`：

- `AppTheme` 扩展为 10 个字段：`background / backgroundDeep / dashboard / card / cardDeep / line / accent / button`；五套主题各自给出同色系、不同深浅的取值（如薄荷：背景 `#E8F4EC`、卡片 `#F4FBF7`、卡片嵌套 `#DCEDE3`、描边 `#A9C7B6`、强调 `#6F9E85`、按钮 `#4C7A5C`）。
- 把既有颜色名改成**读取当前主题的可组合属性**，这样上百处调用点无需改名即可跟随主题：

```kotlin
val Cream: Color @Composable @ReadOnlyComposable get() = LocalAppTheme.current.card
val CreamDeep: Color @Composable @ReadOnlyComposable get() = LocalAppTheme.current.cardDeep
val WarmCard: Color @Composable @ReadOnlyComposable get() = LocalAppTheme.current.card
val WarmDashboard: Color @Composable @ReadOnlyComposable get() = LocalAppTheme.current.dashboard
val WarmOrange: Color @Composable @ReadOnlyComposable get() = LocalAppTheme.current.accent
val HairLine: Color @Composable @ReadOnlyComposable get() = LocalAppTheme.current.line
val ThemeButton: Color @Composable @ReadOnlyComposable get() = LocalAppTheme.current.button
```

- 描边/分隔线：`DashedDivider(...)`、`BorderStroke(1.dp, ...)` 与选中态描边统一改用 `HairLine`；实心按钮 `buttonColors(containerColor = ...)` 改用 `ThemeButton`（比背景深、保证白字可读）。
- 品牌色（`Coral` / `Peach` / `Mint` / `Lilac` / `Sky`）与文字色 `Ink` 保持固定，避免图表与正文对比度受影响。

注意：可组合属性只能在 `@Composable` 上下文读取。`HomeScreen.openItem` 是普通局部函数，改为在外层取 `LocalAppTheme.current.accent` 再传入，避免在 lambda 中调用可组合函数。

## 审批时间

后端返回的 `submittedAt` / `decidedAt` 是 UTC ISO 时间戳，原实现 `timeLabel()` 直接 `substring(5,10)` + `substring(11,16)`，等于展示 UTC 时间。改为复用 `data/BusinessTime.kt` 的 `businessDateTimeLabel(iso, timezone)`（UTC 毫秒 → 业务时区 `MM-dd HH:mm`）；业务时区由 `MainActivity` 把 `currentUser.businessTimezone` 传入 `ReviewScreen`，再透传到列表卡片与详情。

## 其他

- 视图页固定返回按钮文案「回到今天」→「回到今年」（年视图定位到今年一月，文案与行为一致）。
- 实心主题按钮（`ThemeButton`）的**文字与图标统一为白色**：新增 `ui/theme/Theme.kt` 的 `OnThemeButton`（固定 `Color.White`），所有 `ButtonDefaults.buttonColors(containerColor = ThemeButton)` 调用点补 `contentColor = OnThemeButton`——详见变更 `add-cross-day-task-scheduling` 的其他说明。深色按钮底色随主题切换（比背景深），白字在所有主题下均可读。
- `:app:compileDebugKotlin` 通过。
