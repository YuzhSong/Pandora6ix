# Pandora Android UI 全量审计报告

审计日期：2026-10-31。基线：`backend-syz` / `8123047`。本报告先记录现状；目标规则见 [Design System](ANDROID_UI_DESIGN_SYSTEM.md)，实施顺序见 [路线](UI_REFACTOR_ROADMAP.md)。本次仅变更文档与静态审计工具。

## 1. 范围、证据与统计口径

扫描 `android/app/src/main` 全部 53 个 Kotlin 文件，以及资源 XML、主题配置、需求文档和最近 12 个提交。逐类检查 16 个组件家族、83 个 `@Composable` 函数声明（包含颜色辅助函数、Modifier 和主题入口，**不等于 83 个独立视觉组件**）。导航清单有 27 个页面/可独立进入的业务状态；日周月年分别计数，审批队列合计一个页面，共用详情只计一次；弹窗另外登记。

可复现命令：在仓库根目录执行 `python docs/tools/android_ui_inventory.py`。数据位于 [summary.json](ui-audit-data/summary.json) 和同目录 CSV；CSV 的 `file,line` 是当前代码定位，颜色/字号/尺寸表达式保留原文。统计是静态声明，不能推导为所有屏幕最终像素。

| 项目 | 本次结果 | 口径与限制 |
|---|---:|---|
| 页面/业务状态 | 27 | 下表逐项列出，不虚构资料编辑等不存在页面 |
| 组件家族 / Composable 声明 | 16 / 83 | 前者人工分类，后者可复现枚举 |
| 基础背景实际 Hex | 25 | 五套主题的 background/backgroundDeep/dashboard/card/cardDeep 去重为 24，加固定白色；含选中容器、导航，排除遮罩/标签/头像/透明叠色 |
| 背景赋值表达式 | 54 / 122 处 | `.background` 与显式 containerColor/backgroundColor；包含动态颜色，不能叫 54 种 Hex |
| 按钮显式配色参数组合 | 10 / 39 处 | 包含同色不同写法、文字/描边按钮；未指定时还有 Material 默认；真实标签按钮颜色来自服务端，数量无固定上限 |
| Text 字号/字重表达式组合 | 46 / 477 处 | 含 inherited、条件和动态字号，非 46 套已解析 Typography |
| 圆角参数组合 | 16 / 89 处 | 14 种 uniform dp 值 + 50% + 非对称 17dp；另有 CircleShape 和 Material 默认 |
| 间距数值 | 24 | padding/PaddingValues/spacedBy 中的不同 dp 字面量，含 0；108 种完整参数组合、327 处调用；不把所有 width/height 当间距 |

资源检查：`res/values/themes.xml:3` 仅指定系统 Light NoActionBar；没有独立 `colors.xml` 调色板。启动图标等 drawable/vector 颜色属于资源资产，不计入业务背景数量。没有 Type.kt、Shape.kt、Dimens.kt，也没有自定义 Typography/字体家族；Material 默认字体与局部 Text 参数并存。

**视觉验证边界**：本次为全工程代码审计，没有重新完成 27 个页面在五种主题、字体缩放、不同宽度和 TalkBack 下的截图矩阵。对溢出、遮挡、焦点顺序、性能仅列待验证风险，不将其写成已经复现的缺陷。用户给出的旧截图属于历史症状，不能证明当前基线仍呈现相同效果。

## 2. 产品依据与优先级

1. 本次任务及当前会话最新明确要求优先：白色模态浮层；主次标签均可见；主标签优先；任务、日志、人员有清晰面板和条目分界；我的审批/设置位于组织信息之前；五项导航固定。
2. `docs/PRODUCT.md`：员工任务/日志、管理授权、紧凑预览与真实数据。
3. `docs/requirements/Pandora_产品需求与设计文档_20261009确认修订版_V1.3.md:90–102`：日报条目、共用标签、主题偏好；历史版本中审批入口预留、本人派发等描述已被后续实现与确认覆盖，不能据此恢复旧行为。
4. OpenSpec：`add-android-task-summary`、`refine-task-summary-previews`、`replace-profile-statistics-with-reviews`、`complete-company-task-workflows`、`add-theme-and-review-time-polish`。
5. 提交：`fde25f3` 汇总初版、`d0d129f` 面板/审批入口、`315485e` 我的排序、`23fbb0c` 主题持久化、`8123047` 姓名/标签/弹窗/内联执行。这里只检查当前会话与仓库记录，未声称访问其他 ChatGPT 历史。

保留导图四板块；汇总三面板，任务/日志最多预览三条、直属四人；任务面板 Tab 仅筛选本面板，全部入口进入专用列表。保留月→周→日、跨日期连续段、重叠分行和回到今天。AI地图目前占位，不增设业务功能或 KPI。

## 3. 页面清单与跳转

下表路径均相对 `android/app/src/main/java/com/pandora6ix/app/`。`M` = MainActivity.kt，`C` = ui/calendar/CalendarScreen.kt，`S` = ui/summary/TaskSummaryFlow.kt，`R` = ui/review/ReviewScreen.kt。

| # | 页面/状态 | 类型与入口 | 主要组件/代码 | 不一致与优化建议 |
|---:|---|---|---|---|
| 1 | 登录 | 会话入口 | M:779 输入框/提交 | 28dp 边距、50dp按钮、14dp圆角独立定义；保留身份流程，统一表单 |
| 2 | 导图 | 一级 HOME | M:820 HomePanel | dashboard 黄底、22dp角、2dp彩边；板块保留，迁移容器 Token |
| 3 | 导图板块展开 | 导图内覆盖状态 | M:963 Card/LazyColumn | 90%×82%、10dp阴影；统一 overlay 限宽与安全区 |
| 4 | 导图事项/条目详情 | 二级 | M:1370 HomeItemDetailScreen | 4dp elevation、局部色；统一详情段落 |
| 5 | 个人重要事项选择 | 导图编辑浮层 | M:1122 PersonalImportantEditor | 12dp阴影与自定义遮罩；保留勾选上限与保存语义 |
| 6 | 日视图 | 一级视图模式 | C:466 TimeGrid | hourHeight=60dp，40dp刻度栏；允许时间栅格例外 |
| 7 | 周视图 | 同页模式 | C:466/583 | hourHeight=56dp、11sp文字；统一标签/状态，保留重叠算法 |
| 8 | 月视图 | 同页模式 | C:321/372 | 左右8dp、18dp任务段、3dp角；列为日历例外，不套普通卡片尺寸 |
| 9 | 年视图 | 同页模式 | C:708 YearGrid | 8/9sp文字与20dp日期格；有阅读/点击风险，需缩放与语义验收 |
| 10 | 任务详情 | 共用二级/三级 | M:2341 DetailScreen | 白16dp角面板，内部记录10dp角、主题彩边；统一边框，保留内联执行 |
| 11 | 任务新建/派发 | BottomSheet | ui/calendar/TaskEntrySheet.kt:66 | 全屏表单、标签/接收人/日期选择；统一输入与底部动作 |
| 12 | 任务编辑 | 复用同表单 | TaskEntrySheet editingTask | 同新建但审批/重复范围不同；样式共用，权限不改 |
| 13 | 任务汇总 | 视图标题右侧 | S:42 | 22sp标题、16dp面板距、10dp内部距；迁移统一层级 |
| 14 | 汇总任务全部 | 汇总/成员详情 | S:161 page=tasks | 专用任务列表；保持作用域，不添加全局Tab页 |
| 15 | 组织架构 | 直属更多 | S:430 | 深度×12dp缩进；深层可能挤占文本，需窄屏检查 |
| 16 | 成员详情 | 下属/组织成员 | S:183 page=member | 信息/任务/日志三面板；职级缺字段时诚实占位，不编造 |
| 17 | 汇总日志全部 | 汇总/成员详情 | S:171 page=journals | 同屏列表复用 JournalRow，统一密度与边界 |
| 18 | 今日工作日志 | 一级 LOGS 默认状态 | M:1444 LogsScreen | 卡片/标签/动作部分本地定义；统一正文/表单 |
| 19 | 历史日志 | LOGS 内状态 | M:1444 + JournalDatePicker | 历史选择全屏 Dialog 用 dashboard；统一模态底色 |
| 20 | 日志编辑/发布 | 日志内覆盖编辑 | M:1610附近编辑状态、JournalEntryField | 自定义全高Card/白底；统一IME/按钮，保留发布确认 |
| 21 | 日志详情 | 多入口共用 | M:1954 JournalDetailScreen | JournalSection与任务详情重复容器；统一基础Panel |
| 22 | AI地图 | 一级 AI | M:2078 | 占位28sp标题/70dp图标；保留占位，勿扩建分析页 |
| 23 | 我的 | 一级 ME | M:2079 ProfileScreen | 个人/审批设置/组织；卡片、头像、行距与其他页不同 |
| 24 | 设置 | 我的二级 | M:2551 SettingsScreen | 18dp分区距、14dp行Padding；显示/通知/数据/账号分组统一 |
| 25 | 审批中心/审批队列 | 我的二级 | R:114 ReviewScreen | 待我审批/我提交的是同页队列状态；自制QueueTab与TabRow不同 |
| 26 | 审批详情 | 三级 | R:283 ReviewDetail | 分区卡片、评论条8dp阴影；与任务详情统一基础容器 |
| 27 | 标签管理 | 我的二级/权限控制 | ui/tags/TagManageScreen.kt:59 | 白色编辑Dialog为18dp角，色块40dp；统一浮层与触达 |

个人资料当前为“我的”内面板，改密码/头像为浮层，不虚构单独个人资料路由。需求示例中的“日志内任务管理”在当前LogsScreen没有独立入口/任务列表；本次明确记为不存在，不因文档示例新增。

跳转依据 M:649–748、S:97–137：登录→五项主导航；导图事项→事项/任务/日志详情；视图→日周月年/新建/详情/汇总；汇总→任务全部、组织→成员→任务/日志、日志全部；日志→编辑、历史日期、详情；我的→设置、标签管理、审批中心→审批详情→驳回重提表单。通知抽屉按通知类型进入审批或任务详情。底栏当前在二级页仍显示，本次只记录，未来不擅自改变返回关系。

## 4. 颜色审计

### 4.1 基础背景 Hex 与使用位置

以下列出所有 25 个基础背景 Hex；相同值跨角色只算一种。主题各行的字段各有一个声明；调用次数依赖角色别名，不能分摊为“某个当前主题运行几次”。精确源码调用见 backgrounds.csv；字段显式背景调用次数与位置见 [theme-background-uses.csv](ui-audit-data/theme-background-uses.csv)，每种主题值的定义见 Theme.kt:45–80。该CSV只计background/containerColor显式调用，不把Surface的color参数、默认值或运行时显示次数混入。

| 主题 | 页面 background | 导航/深层 backgroundDeep | dashboard | 卡片 card | 选中 cardDeep |
|---|---|---|---|---|---|
| 暖米 | #FFFBF2 | #FFF2C9 | #FFF1C7 | #FFFCF4 | #FFF2C9 |
| 薄荷 | #E8F4EC | #D9EAE0 | #E2F0E7 | #F4FBF7 | #DCEDE3 |
| 晴空 | #E9F1FA | #D9E7F4 | #E3EDF8 | #F6FAFF | #DCEAF8 |
| 樱花 | #FBF0F3 | #F7E1E8 | #F9E7EB | #FDF8FA | #F9E1E8 |
| 紫藤 | #F1EDFA | #E4DEF3 | #EAE5F7 | #FAF8FF | #E8E2F7 |
| 固定浮层/内容 | #FFFFFF | — | — | 部分详情白色 | — |

`Cream` 与 `WarmCard` 都返回 card；`CreamDeep` 返回 cardDeep，不能当新 Hex。dashboard/backgroundDeep/cardDeep 视觉接近，未来合并为三层背景与明确选中态。白色浮层保留，五种主题选择保留，禁止把用户选择“紫藤”本身当成缺陷。

`backgrounds.csv` 对每种表达式逐处登记位置，`summary.json.background_expressions` 给出使用次数。额外颜色来自 Ink 的透明度、Mint/Peach 状态背景、业务标签/头像、主题预览与遮罩，不属于额外页面层级。静态色候选表 `colors.csv` 包括原始 ARGB 色板及转换代码常量，不能把候选去重数当作有效UI颜色数。

### 4.2 按钮与状态

ButtonDefaults 显式组合：ThemeButton+OnThemeButton（13处）、ThemeButton单独（1）、MintDeep+White（3）、CreamDeep+Ink（2）、accent+Ink（1）、tagColor+Ink（2）、contentColor=Ink（11）、Ink 65%（2）、CoralDark（2）、全限定CoralDark（2）。后两者同色不同写法。另有默认Material按钮、FAB WarmOrange、IconButton与局部Text颜色覆盖，见 buttons.csv 及源码。

问题不是仅“橙色数量多”：ThemeButton随主题、默认primary固定Coral，标签颜色还被用作普通操作按钮。默认白字/Coral对比约 **2.40:1**，白字/暖米按钮B8705A约 **3.82:1**；晴空4A7CA5约4.45:1、樱花B4657C约4.12:1。按sRGB相对亮度计算，普通按钮文字目标4.5:1，不能凭注释“白字可读”验收。

优先级 Theme.priorityMeta：1/2 Ink40%、3 WarmOrange、4 Coral、5 CoralDark；TaskEntrySheet另外把1/2写为“很低/一般”。汇总完成 Mint、拒绝 Peach、其余 CreamDeep；审批 APPROVED Mint/MintDeep、REJECTED Peach/CoralDark、待审批 CreamDeep/Ink。Calendar当前时刻为写死#E5484D；进度某些分支还有#2E7D32/#9E9E9E（colors.csv）。这些应映射统一语义，不能把主标签颜色当执行状态。

### 4.3 弹层与遮罩

Material表面最近已统一白色，不应重复宣称全部仍紫色。仍存在 **JournalDatePicker.kt:133 全屏历史日期 Dialog 使用 WarmDashboard**；`TagEditDialog:200` 为白色但18dp角、额外24dp边距；普通AlertDialog默认角与自定义Card角不同。任务/进度BottomSheet显式白色。M 自制遮罩出现 Black42%、38%，默认Dialog遮罩由平台处理；没有统一控制入口。

## 5. 字体、间距、圆角和阴影

### 5.1 字体

系统字体；没有额外字体资源。Text 参数的逐处清单见 typography.csv。显式常量字号覆盖：**8、9、10、11、12、13、14、15、16、17、18、19、20、22、24、26、28、30sp**；动态字号、头像计算、Material默认不在这18种内。字重出现 Normal、Medium、SemiBold、Bold及条件表达式。页面标题有22Bold、默认TopAppBar和28Bold；任务条15SemiBold、正文13/14/16并存。只少数正文指定20/22/24/26sp行高，多数继承 LocalTextStyle；没有统一letterSpacing/TextAlign策略。日期格16sp字/16sp行、年8/9sp字行一致存在裁切风险，尚需中文与缩放验证。

合并方向：13/14/15sp辅助文本统一BodyMedium或Label；17/18/19sp标题统一TitleSmall；22/24统一页面TitleLarge；26/28/30仅保留Display特例。日历8–11sp须专门审查，不能把密集概览规则复制到任务/日志正文。任务摘要最多2行、日志摘要1行省略保留，完整详情不省略正文。

### 5.2 实际间距与尺寸

间距24值：**0、1、2、3、4、5、6、7、8、9、10、12、14、16、18、20、24、28、32、40、48、50、80、96dp**。不是全部都要机械删除：1dp可能是条目对齐，50dp可能是图标栏缩进，应改为组合Token并说明用途。

| 场景 | 当前值/依据 | 目标方向 |
|---|---|---|
| 页面左右 | 登录28；汇总16；月8；设置16；日历头12 | 普通16，登录24，日历8具名例外 |
| 分区间距 | 设置18；汇总/详情16；内容10/12 | section24，紧凑面板16，内部12 |
| 卡片内距 | Home14；详情16；任务10×8；日志12 | 普通16；紧凑业务项12×8 |
| 行布局 | SettingsRow14×14，SubValueRow起始50 | 常规水平16/纵12；嵌套起点图标24+间距8+16=48 |
| 图标 | 返回Material默认；业务18/13/30/64/70 | 普通24，小元数据16，品牌占位可64 |
| 头像 | 直属44；成员56；执行32；动态头像参数 | 统一24/32/40/48/64，直属40 |
| 直属条目 | 68宽×4+12间距×3=308，加面板内外64=372 | 360dp无法静态容纳四人；等分栅格，不能声称任意屏都够 |
| 导航 | Material NavigationBar默认，标签11sp | 视觉80dp+系统底部insets；未来显式规范 |
| 滚轮 | 行40、视口200、列72 | 行48、视口240、按权重，长年份不裁切 |
| 按钮 | 登录50高；回今天34高；默认M3/局部值 | 主48视觉高；紧凑视觉32/40但触达48 |
| 时间栅格 | 日60/周56每小时，刻度40宽，月任务18高 | 保留业务几何例外，不能改跨日/重叠算法 |

M:649消费Scaffold padding；审批评论R:503有navigationBarsPadding+imePadding；JournalEntryField读取IME高度；新建表单与全屏历史Dialog需逐页检查窗口Insets。现有处理分散，不应盲目叠加padding造成双重空白。安全区与字体缩放缺少统一验收矩阵。

### 5.3 圆角、阴影

uniform圆角为 **0、3、4、6、8、10、12、14、16、18、20、22、24、28dp**，另50%和非对称17dp；CircleShape单列。常见HomePanel22、任务详情16、日志/审批部分12/16、标签6/8/10、输入/按钮12/14、Dialog默认Material形状。普通容器收敛16、输入/按钮12、小组件8、Dialog24。

显式elevation：Home展开10、个人事项编辑12、事项详情4、通知抽屉14、设置toast6、SettingsCard2dp；审批评论shadowElevation8dp。普通面板应0dp+1dp中性边框；需要层级的menu/dialog/sheet才采用规范阴影。现有重阴影与描边组合应拆开评估。

## 6. 组件家族审查与复用

| 家族（16） | 当前实现及差异 | 后续统一点 |
|---|---|---|
| Button | M登录、TaskEntry、Review、Progress各自参数 | PandoraButton 5种变体、状态、触达48 |
| IconButton | 默认Material与自制clickable图标混用 | 24图标/48触区，独立操作必须contentDescription |
| TextField | M默认输入、JournalEntryField、审批评论 | 56最小高/12角，正文16/24，多行增长 |
| Card/Surface | HomePanel/DetailCard/JournalSection/SectionCard/SettingsCard/SummaryPanel | 共用Panel基础，业务结构不强行合并 |
| Chip | TagChips、summary tags、MetaChip、DecisionBadge | TagChip与StatusChip分开，主标签优先 |
| Tab | Material TabRow与QueueTab | 48最小高；中性未选，Primary选中+2dp线 |
| Dialog | Alert、自制Dialog、覆盖Card | 统一白底、24角、遮罩/按钮顺序 |
| BottomSheet | 新建、Calendar任务选择、Progress | 白底24顶部角，IME、安全区一致 |
| Dropdown/Popup | Calendar模式菜单、接收人Popup | 12角、48选项、限宽与锚定，保留组织查询权限 |
| Snackbar | 设置自制浮动toast，错误多inline | 共用可访问提示；表单错误继续inline，勿吞错 |
| ListItem | SettingsRow、人员、任务、日志行 | 同类同密度，按内容增长，不强制所有业务一样高 |
| BottomNavigation | M:649 五项NavigationBar | 现有顺序/路由不变，24图标/12标签 |
| Avatar | AvatarView与RemoteAvatar | 共享远程加载/缓存；基础尺寸与语义统一 |
| Calendar | DateLane/TimeGrid/WeekTaskStacks/YearGrid/轮选择 | 日历专用Token，不替换排布与跨日逻辑 |
| TaskItem | SummaryTaskRow、Calendar任务、详情内记录 | 任务摘要共享信息层级，时间条保留专用形态 |
| JournalItem | JournalCard/JournalRow/JournalSection | 共享作者时间/摘要，日志正文不按标签大面积染色 |

Overlay清单：审批提醒/普通提醒；个人重要事项编辑及展开；通知抽屉；任务创建/编辑、日期/时间、退出确认、标签选择、创建结果、重复范围、执行进度/验收/恢复/错误；日志编辑、标签、退出/发布确认、历史日期及年月；头像菜单/色板/相册；设置字体/主题/提前量/通知分类/改密码/清缓存/退出；标签增改/删除确认；审批通过/驳回确认。逐处位置可用 `rg -n 'AlertDialog\(|Dialog\(|ModalBottomSheet\(|Popup\(|DropdownMenu\(' android/app/src/main/java`复查。相机/相册系统页面属于外部平台UI，不要求改白底。

## 7. 最严重的十个问题（设计一致性优先级）

这里P0指全局统一化首批阻塞项，不表示已证实业务无法完成；与路线采用同一口径。

| # | 优先级 | 证据/影响 | 明确建议 |
|---:|---|---|---|
| 1 | P0 | Theme.kt:113固定Material，AppTheme并存；同主题主要动作两种色 | Material与LocalAppTheme单一适配器 |
| 2 | P0 | 白字Coral2.40:1、暖米按钮3.82:1 | 深化Primary并验算全部变体，勿仅改背景 |
| 3 | P0 | 25基础背景；dashboard/cardDeep近色，历史日期Dialog仍黄底 | 三层背景+选中态Token，白色浮层 |
| 4 | P0 | 无Typography，46表达式组合，行高随继承 | 建立8级文字规范+日历例外 |
| 5 | P1 | 任务summary标签35%透明、编辑lerp10%、详情原色 | 同一TagChip渲染策略，使用原业务色+中性描边 |
| 6 | P1 | 16圆角组合，Home22/表单14/内容10/Modal默认 | 7个Shape Token，普通卡片16 |
| 7 | P1 | 24间距值、登录28/普通16/月8无统一契约 | 4dp基准，列出业务例外 |
| 8 | P1 | 直属固定布局总需372dp，360窄屏需滚动才能看到第4人 | 四列等分，不靠固定68宽 |
| 9 | P1 | Calendar回今天34、滚轮40、色块40；触区与语义分散 | 逐处验证48dp命中区与TalkBack，允许视觉紧凑 |
| 10 | P1 | 6种以上容器重复、2–14dp阴影及自制遮罩 | 基础Panel/Dialog/提示统一，保留用户要求的内层分界 |

优点：五导航稳定；汇总严格三面板、有预览上限；详情多入口复用；数据授权不依赖颜色；白色Material浮层已落地；共享头像与主题持久化可继续沿用。

技术健康度（仅静态、非发布评级）：Accessibility 2/4（存在明确对比问题，TalkBack未测）；性能2/4（有Lazy与图片缓存，无性能采样）；响应式2/4（权重布局与硬尺寸混用）；主题2/4（有Token但双轨）；装饰克制3/4（无KPI，多余彩边/阴影仍在），合计11/20。没有全页截图，不给“看起来像AI”作肯定判断。用户要求的分区面板与条目边框有业务用途，应保留。

## 8. 验收与未验证项

本次可验证：全源码静态盘点、需求溯源、对比度计算、文档数值与工程映射、文档链接、OpenSpec严格校验、runtime目录零改动。没有声称执行新的Android构建或全页面视觉/性能测试；文档与Python清单不改变APK。未来每批迁移必须执行路线中的构建、截图、字体缩放、TalkBack和权限回归，不能用本报告代替。
