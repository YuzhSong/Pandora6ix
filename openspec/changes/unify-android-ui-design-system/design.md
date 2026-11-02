## Context

Android 端是单 Activity 的 Compose 应用。主题现状：`ui/theme/Theme.kt` 定义 `AppTheme`（background/backgroundDeep/dashboard/card/cardDeep/line/accent/button 八个字段）和五套主题，页面通过 `Cream`、`WarmCard`、`WarmOrange`、`ThemeButton`、`HairLine` 等 Composable 别名取色；同时 `PandoraTheme` 固定一套 Coral 的 Material `lightColorScheme`，不随主题变化。字体、间距、圆角没有集中定义；`MainActivity.kt`（约 3000 行）承载导图、日志、AI 地图、我的、设置和详情，日历/汇总/审批/标签在 `ui/` 子包。

约束：底部五项导航、五套主题 key 与按账号保存、全部业务流程不变；不新增依赖；每阶段独立本地提交，不推送。

## Goals / Non-Goals

**Goals:**
- 一个主题 key → 一份 `PandoraColorTokens` → 同时驱动 `LocalAppTheme`、语义 Token 和 Material3。
- 八级 Typography、4dp 间距、圆角 Token 进入代码，并映射到 Material3，使默认组件自动合规。
- 公共组件（按钮、面板、对话框、BottomSheet、输入框、Chip、Tab、列表行）统一外观；页面逐步改用。
- 状态/优先级颜色集中到一个映射函数。

**Non-Goals:**
- 不做暗色模式，不改导航结构或路由，不拆分 `MainActivity.kt` 的业务结构。
- 不改日历的区间/重叠算法和几何（日 60dp/小时、周 56dp、月条 18dp 属于规范内的例外）。
- 不修改服务端标签颜色或新建标签的候选色板。

## Decisions

1. **转接旧别名，而不是一次性全部删除。** `Cream/WarmCard → surface`、`CreamDeep → primaryContainer`、`WarmDashboard → appBackground`、`WarmOrange/ThemeButton → primary`、`HairLine → border`，`CoralDark → error`、`Mint/MintDeep → successContainer/success`、`Peach → errorContainer`。别名标 `@Deprecated` 但保持可编译。这样 P0 就能让全 App 的三层背景和主色统一，P2 再逐页替换调用点。备选方案是一次性改完 300+ 调用点，风险高且不易回退。
2. **`AppTheme` 保留 key/label，颜色改由 `PandoraColorTokens` 提供。** 旧字段（background、dashboard、accent 等）作为计算属性保留并指向新语义，设置页的主题预览继续可用。
3. **Material3 映射按规范第 10 节**：surface 系列全白、surfaceTint 透明、outline=InputBorder、outlineVariant=Border、Shapes small/medium/large/extraLarge=8/12/16/24、Typography 每个槽位引用八级之一。这样 AlertDialog、DropdownMenu、TextButton 等默认实现自动取得白底、24dp 角和正确主色。
4. **组件放在 `ui/components/`，以 `Pandora` 为前缀**：`PandoraButton`（variant 枚举 + loading）、`PandoraPanel`、`PandoraAlertDialog`/`PandoraDialogSurface`、`PandoraBottomSheet`、`PandoraTextField`、`PandoraTagChip`/`PandoraStatusChip`、`PandoraTabRow`、`PandoraListItem`。业务组合（HomePanel、SummaryPanel、JournalCard）保留名称，只改基础包装。
5. **状态映射集中在 `ui/theme/StatusColors.kt`**，纯函数可单测；未知值显示“状态未知” + 中性色。
6. **对比度用单元测试守护**：按 WCAG 相对亮度计算五主题 Primary/白字、正文/背景、状态文字/状态底。

7. **对比度验算后的最小修正**（规范 2 节要求“其他组合须验算”）：Error `#B34E4A` 在 ErrorContainer 上仅 4.12:1 → `#A84844`（4.60:1）；TextTertiary `#76706E` 在薄荷/晴空/紫藤背景上 4.23–4.37:1 → `#6F6967`（最低 4.68:1）；薄荷 Primary `#4C7A5C` 作为文字在薄荷背景上 4.38:1 → `#487657`（4.64:1）。暖米 Primary 文字在 PrimaryContainer 上仅 3.90:1，因此选中淡底（胶囊、今天标记）上的文字统一用 TextPrimary，Primary 只作图标/指示。
8. **系统栏**：targetSdk 35 强制 edge-to-edge，`Theme.Material.Light` 下状态栏图标为白色、在浅色背景上不可见；PandoraTheme 统一设置深色系统栏图标，XML 主题同步 `windowLightStatusBar` 与启动底色。

## Risks / Trade-offs

- [别名转接会同时改变大量页面外观] → P0 后在模拟器五主题检查导图与视图；构建和现有测试必须通过。
- [MainActivity 体量大，逐页替换易误伤业务逻辑] → 只替换样式参数（颜色、字号、形状、内距、容器），不改回调和状态；每阶段跑构建与单测。
- [模拟器截图依赖本机环境] → 能启动则在 warm/mint/sky/sakura/lilac 下截取主要页面；需要登录的页面受测试账号限制时如实记录未覆盖项。
- [日历密集文字] → 只换颜色/字体 Token，保留 10sp `calendarMicro` 例外和全部几何。

## Migration Plan

P0 → P1 → P2 → P3 顺序推进，每阶段一个本地提交，可单独回退。旧别名在 P3 结束时仍保留为 deprecated，只要还有剩余调用点就不删除，并在验收报告中记录剩余数量。

## Open Questions

无阻塞问题。对比度不足或规范无法落地之处在 P3 记录到文档。
