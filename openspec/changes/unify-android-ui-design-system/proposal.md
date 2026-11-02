## Why

2026-10-31 的审计（`docs/UI_AUDIT_REPORT.md`）和 Design System 1.0（`docs/ANDROID_UI_DESIGN_SYSTEM.md`）已经确定 Android 的颜色、字体、间距、圆角和组件规范，但代码仍是双轨：固定 Coral 的 Material 配色与 `LocalAppTheme` 并存，白字按钮对比度不足（默认 2.40:1），46 种字体表达式、16 种圆角、24 种间距散落在页面里。规范只写在 docs 中不会约束后续开发，本变更把它真正落到 Compose 工程。

## What Changes

按 `docs/UI_REFACTOR_ROADMAP.md` 分四个阶段推进，在同一变更内跟踪进度：

- **P0 设计基础层**：新增 `ui/theme/Color.kt`、`Type.kt`、`Dimens.kt`、`Shape.kt`；`Theme.kt` 改为由所选主题 key 同时生成 `LocalAppTheme`、语义色 Token 和 Material3 ColorScheme/Typography/Shapes；旧别名（Cream、WarmOrange、ThemeButton 等）保留为 deprecated 转接并指向语义 Token；新增 `PandoraButton`（Primary/Secondary/Outline/Text/Destructive，含 loading）与 `PandoraPanel`；导图和视图两个页面作为试点。
- **P1 公共组件**：统一 Dialog/AlertDialog、BottomSheet、TextField、Tag/Status Chip、Tab、列表行、IconButton、Snackbar/提示、底部导航与 TopAppBar 的外观，集中状态与优先级颜色映射。
- **P2 页面迁移**：导图二级、视图/任务、任务汇总/组织/成员、日志、AI 地图、我的/设置/审批/标签管理逐页改用 Token 与公共组件；日历、甘特式连续条和组织树保留各自的信息结构。
- **P3 验收**：重跑静态盘点并区分硬编码、Token 定义、主题映射和业务色；五主题对比度、构建、单元测试、模拟器截图（在环境允许时）；更新三份 UI 文档并新增 `docs/UI_REFACTOR_ACCEPTANCE_REPORT.md`。

不新增、删除或调整底部五项导航（导图、视图、日志、AI地图、我的），保留五套主题 key、名称和按账号保存。

## Capabilities

### New Capabilities
- `android-design-system-implementation`：Android 客户端以统一的语义 Token、共享组件和五主题适配器渲染 UI，且不改变业务行为。

### Modified Capabilities

无。业务规范（任务、日志、审批、汇总、日历）的要求保持不变。

## Impact

- 代码：仅 `android/app/src/main/java/com/pandora6ix/app/ui/**`、`MainActivity.kt` 及 Android 单元测试；`data/` 层只允许为 UI 复用调整展示映射，不改变 Repository 语义。
- 不涉及：后端 API、数据库、权限、认证、任务派发/审批/验收流程、日志记录与查询规则、推送。
- 文档：`docs/UI_AUDIT_REPORT.md`、`docs/ANDROID_UI_DESIGN_SYSTEM.md`、`docs/UI_REFACTOR_ROADMAP.md`，新增验收报告。
- 无新依赖。
