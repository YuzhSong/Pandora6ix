# Pandora Android UI 统一化改造验收报告

2026-11-03，分支 `Android-syz`，OpenSpec 变更 `unify-android-ui-design-system`。依据 [Design System](ANDROID_UI_DESIGN_SYSTEM.md)、[审计基线](UI_AUDIT_REPORT.md) 与 [改造路线](UI_REFACTOR_ROADMAP.md)。本报告只记录实际执行过的检查，未执行的项目单独列出，不视为通过。

## 1. 阶段与提交

| 阶段 | 内容 | 本地提交 | 验证 |
|---|---|---|---|
| P0 | Color/Type/Dimens/Shape/StatusColors Token；同一主题 key 生成 LocalAppTheme 与 Material3；PandoraButton/Panel/Chips/OverlaySurface/Divider；导图、视图试点；深色系统栏图标 | `ff9c5a1` | 构建、122 项单测、OpenSpec 严格校验、五主题×导图/视图与日周月年截图 |
| P1 | PandoraAlertDialog/Dialog/BottomSheet/DropdownMenu/TextField/TopBar/TabRow/ListItem/Toast/IconButton；全部 AlertDialog、OutlinedTextField、ModalBottomSheet、DropdownMenu 改用统一组件；主要按钮改 PandoraButton；40% 遮罩 | `c765e62` | 同上；对话框、Sheet、Tab 截图，紫藤下弹窗取色为纯白 |
| P2 | 其余页面字体/颜色/圆角/间距 Token 化；卡片 0 阴影 + 1dp 边框；虚线改 Divider；Tag/Status Chip；直属四人等分；一级页面统一标题行 | `b80527f` | 同上；五个一级页面与主要二级页截图 |
| P3 | 重跑静态盘点并分类；删除旧颜色别名；补齐残留硬编码；360/393dp 与大字号、IME 检查；本报告与文档更新 | 见本次提交 | 见第 4 节 |

## 2. 改造前后静态统计

统计来自 `python docs/tools/android_ui_inventory.py`（P3 已更新为读取 `ui/theme/Color.kt` 的五主题 Token，并对字面量颜色分类）。均为源码静态声明，不代表运行时像素；文件数从 53 增至 65，是因为新增了 Token 与组件文件。

| 项目 | 基线（10-31） | 现在 | 说明 |
|---|---:|---:|---|
| 基础背景 Hex（五主题角色去重 + 白） | 25 | 16 | 每主题保留 AppBackground/Surface/PrimaryContainer 三个角色，删除 backgroundDeep/dashboard 大面积角色；ElevatedSurface 五主题同为白色 |
| 背景赋值表达式 | 54 | 36 | 剩余均为语义 Token、头像/标签业务色或 Transparent |
| 按钮显式配色参数组合 | 10 | 6 | 1 组为 PandoraButton 内部实现，其余为 TextButton/日历入口使用的 Token |
| Text 字号/字重表达式组合 | 46 | 8 | 393 处 Text 中 281 处使用八级 `PandoraTypography`，其余继承 Material 映射；剩余 2 处字面量字号均为随头像尺寸计算的首字 |
| 圆角参数组合 | 16 | 11 | 其中 6 组是 Token 定义本身，另有 3 组为日历例外（4dp 时间块/3dp 月条）和半胶囊“回到今天”入口 |
| 间距数值 | 24 | 6 | 0、1、2 及 68（设置子项与标题文字对齐：16+40+12）、80（周视图底部为 FAB 留白）、96（排序/保存按钮最小宽度） |

字面量颜色（共 103 处出现）按来源分类：

| 分类 | 处数 | 说明 |
|---|---:|---|
| token_definition | 51 | `ui/theme` 中的五主题与语义 Token |
| business_palette | 20 | 头像占位色板、新建标签候选色板（规范 2.3 业务例外，未改动） |
| transparent | 18 | `Color.Transparent` |
| component | 3 | 组件内白色文字判断、按钮透明底等 |
| page_hardcoded | 11 | 8 处为未接入界面的 `mock/MockData.kt`；1 处为 `NotificationHelper` 的整数掩码 `0x7fffffff`（误报）；2 处为头像首字白色（头像业务例外） |

旧颜色别名（Cream、CreamDeep、WarmCard、WarmDashboard、WarmOrange、HairLine、ThemeButton、OnThemeButton、Coral、CoralDark、Mint、MintDeep、Peach、Lilac、Sky、Ink）在 P3 已全部删除，`LocalAppTheme.current.*` 直接取色在页面中为 0。

## 3. 规范落地与修正

- **颜色**：五主题共享同一语义结构，`PandoraTheme(theme)` 同时提供 `LocalAppTheme`、`LocalPandoraColors` 与 Material3 ColorScheme，不再固定 Coral 配色。
- **对比度修正**（单元测试 `DesignTokensTest` 发现规范原值不足，按最小幅度调整并写回规范）：Error `#B34E4A`→`#A84844`（ErrorContainer 上 4.12→4.60:1）；TextTertiary `#76706E`→`#6F6967`（薄荷/晴空/紫藤背景上最低 4.23→4.68:1）；薄荷 Primary `#4C7A5C`→`#487657`（作文字时 4.38→4.64:1）。暖米 Primary 文字在 PrimaryContainer 上仅 3.90:1，因此选中淡底上的文字统一使用 TextPrimary。
- **字体**：八级 + `calendarMicro`（10/14sp，仅年概览日期、月任务条、周卡片时间、+N）。
- **按钮**：Primary/Secondary/Outline/Text/Destructive/DestructiveFilled 六种变体，加载时保留宽度并显示“保存中…”类文案；批准不再用绿色，驳回/不通过统一为 Destructive。
- **弹窗**：所有 AlertDialog 白底、24dp 圆角、TitleMedium 标题、BodyLarge 正文、取消在前确认在后，危险确认为 Error；主题 `backgroundDimAmount=0.4`；历史日期全屏选择器改为白色浮层并自理系统栏。
- **系统栏**：targetSdk 35 强制 edge-to-edge，原先浅色背景上状态栏图标为白色，现统一为深色图标。
- **布局**：直属四人改为按面板宽度等分（360dp 屏可完整显示四人）；一级页面标题统一为 64dp、TitleLarge、与内容同一 16dp 左对齐线。
- **保留**：五项底部导航与顺序、五主题 key/名称/按账号保存、全部业务流程与回调、日历几何（日 60dp/小时、周 56dp、月条 18dp）、跨日连续与重叠排布、汇总三面板与预览上限。

## 4. 验证结果

| 检查 | 结果 | 说明 |
|---|---|---|
| `./gradlew.bat :app:assembleDebug :app:testDebugUnitTest` | 通过 | 122 项单测，0 失败（含新增 12 项 Token/对比度/状态映射测试） |
| `openspec validate unify-android-ui-design-system --strict` | 通过 | |
| `git diff --check` | 通过 | 每阶段提交前执行 |
| 五主题 | 通过（截图） | 暖米/薄荷/晴空/樱花/紫藤 × 导图、视图；另抽查晴空“我的/标签管理/审批中心”、樱花审批详情、紫藤日志编辑与主题弹窗、薄荷设置 |
| 屏幕宽度 | 通过（截图） | 默认 411dp；通过临时调整模拟器显示密度检查 360dp（导图、视图、我的、日志、任务汇总、新建任务表单）和 393dp（导图、设置），检查后已恢复默认密度 |
| 字体放大 | 部分通过 | 使用应用内“字号大小=大”检查设置、导图、任务汇总、周视图，无裁切；系统级 1.3/2.0 字体缩放未执行 |
| 键盘 | 通过（截图） | 日志编辑浮层在键盘弹出时上移，发布/取消可见 |
| 二级页面跳转 | 通过（截图） | 任务汇总、任务详情、日志详情、设置、标签管理、审批中心/详情、主题与字号弹窗、标签筛选弹窗、新建任务 Sheet |

截图环境：Pixel_9a 模拟器（1080×2424，420dpi），连接在本机临时启动的隔离后端（scratchpad 内独立 H2 库，随机生成的测试企业与账号），QA 包临时把 `API_BASE_URL` 指向 `10.0.2.2:8080`，构建后立即还原 `build.gradle.kts`，未提交。截图保存在会话 scratchpad，未提交到仓库。

## 5. 未执行或遗留

- 系统字体 1.3/2.0、TalkBack、横屏、三键导航、600dp 宽屏未在本轮执行。
- 发现两处**改造前即存在**的问题，按“不改变业务逻辑”约束未修改：
  1. 审批中心列表页没有系统返回处理，按返回键会直接退出应用（审批详情页有处理）。
  2. 审批详情的快照字段为 JSON null 时显示字符串“null”（`parseSnapshot` 用 `jsonPrimitive.content` 读取 JsonNull）。
- `mock/MockData.kt` 的示例标签色未接入界面，保留原样。
- 日历时间块 4dp、月条 3dp 圆角与 `calendarMicro` 为登记在案的日历例外。

## 6. 人工验收清单

1. 五个一级页面：导图、视图、日志、AI地图、我的 —— 标题同高同字号、左边距一致、底部导航五项顺序不变。
2. 主题切换：设置 → 切换主题，五套逐一切换，确认背景、卡片、主按钮、选中态、底部导航一起变化，弹窗始终白色；退出重进仍保持所选主题。
3. 按钮颜色：提交/保存/发布为主色实底白字；取消为淡底；删除/驳回/不通过为红色描边或红字。
4. 页面背景与卡片：卡片均为 1dp 细边框、无阴影；任务/日志条目边界清晰。
5. 字号与排版：页面标题 24、分区/弹窗标题 20、卡片标题 18、正文 16/14、时间 12。
6. 卡片与弹窗：标签筛选、删除确认、退出确认、修改密码、日期滚轮、新建任务 Sheet、执行进度 Sheet。
7. 日历：日/周/月/年切换、捏合缩放、滑动切换、跨日连续条、+N 下钻、回到今天、现在线（蓝色）。
8. 任务与日志：新建/编辑任务、标签主标签前置、审批通过/驳回、执行进度与验收；日志新增/编辑/删除/历史日期/详情。
9. 窄屏：360dp 设备上任务汇总“直属下属”四人可见，长名字省略。
10. 页面导航：各二级页面返回关系与改造前一致（注意第 5 节已知问题 1）。
