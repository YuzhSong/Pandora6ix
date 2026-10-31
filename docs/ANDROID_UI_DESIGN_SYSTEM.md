# Pandora Android UI Design System

版本1.0，2026-10-31。设计依据与现状见 [审计](UI_AUDIT_REPORT.md)，迁移见 [路线](UI_REFACTOR_ROADMAP.md)。**这是后续开发的规范契约，尚未批量实现到现有页面。** 不将本文件当作新增业务、路由或后端权限的授权。

## 1. 原则与产品边界

Pandora服务员工日常任务与日志，管理者在权限范围内浏览。沿用系统字体和现有暖米身份，以信息层级、对齐、留白与可预测交互形成统一感。Apple仅作为布局原则参考，Android返回、Material交互、IME及触控习惯保留。

五导航固定：导图、视图、日志、AI地图、我的。导图保留四板块；视图保留日周月年与跨日连续/重叠分行；任务汇总标题行小入口、三面板、内部筛选Tab、3任务/4直属/3日志预览及全部入口；成员信息/任务/日志分区。保持单条任务/日志之间清晰分界；不加KPI、完成率、统计图或视频。审批、设置保持在我的组织信息前。

分区面板允许包含有独立点击范围的任务/日志条目容器，这是已确认业务结构。内层用细边框和紧凑间距，不用多重阴影或装饰卡片。

## 2. Color System

色值均为sRGB `#RRGGBB`；透明遮罩另用ARGB。默认暖米。三种背景层级；选中态、状态淡底是小面积语义容器，不能扩散成新页面背景。

| Token | Hex（默认） | 使用场景 | 禁止使用场景 |
|---|---|---|---|
| AppBackground | #FFFBF2 | 页面/导航背景 | 模态浮层 |
| Surface | #FFFCF4 | 我的、审批、普通内容卡片、输入底 | 大面积警告/错误背景 |
| ElevatedSurface | #FFFFFF | 全部Dialog/Sheet/Menu；已确认白色功能面板 | 按主题改紫/黄、加色调叠层 |
| TextPrimary | #302B2A | 正文与标题 | 在深色按钮上 |
| TextSecondary | #615B59 | 次级正文、字段标签 | 任意降低alpha代替Token |
| TextTertiary | #76706E | 时间/说明/placeholder | 用更淡灰显示可读内容 |
| TextDisabled | #9A9490 | 真正禁用的文字 | 普通时间/正文 |
| TextOnPrimary | #FFFFFF | 深色主按钮 | 浅色品牌Coral底 |
| BrandCoral | #FF8377 | 保留品牌标识/小面积装饰 | 普通白字按钮/正文链接 |
| Primary | #A65E49 | 提交/保存/创建、选中、链接 | 每页面自创橙/红 |
| PrimaryPressed | #8D4C3B | 主动作按压 | 静态页面背景 |
| PrimaryDisabled | #E8DDD7 | 主按钮禁用底 | 可操作按钮 |
| Secondary | #615B59 | 次要图标/次要动作文字 | 假冒不同主操作色 |
| PrimaryContainer | #F3E3DC | 选中Tab/开关/紧凑控件淡底 | 整页染色 |
| Border | #D8D3CF | 卡片1dp细边框 | 唯一的输入/焦点辨识依据 |
| InputBorder | #827A75 | 可交互输入未聚焦边框 | 普通面板重边框 |
| Divider | #E8E3DF | 1dp列表分割线 | 错误/选中状态 |
| Focus | #A65E49 | 2dp键盘焦点/输入聚焦 | 永久装饰框 |
| Success | #386345 | 完成/批准的文字图标 | 普通提交动作 |
| SuccessContainer | #DDEEDC | 成功小标签 | 页面背景 |
| Warning | #78580B | 待审批/待验收/重要 | 普通主要按钮 |
| WarningContainer | #FFF2C9 | 警告小标签 | 审批页面大背景 |
| Error | #B34E4A | 错误/驳回/危险动作 | 通常操作/未选中控件 |
| ErrorContainer | #FFE0D8 | 错误小标签/inline提示 | 整页背景 |
| Info | #355F7A | 进行中/提示 | 通用主按钮 |
| InfoContainer | #DCECF2 | 信息小标签 | 日志正文背景 |
| NeutralContainer | #EEEAE6 | 未开始/低优先级 | 禁止伪装成不同卡片层级 |
| Scrim | #66000000 | 40%黑遮罩 | 每个浮层自选透明度 |
| SnackbarContainer | #302B2A | 暂态提示 | 普通卡片 |
| SnackbarContent | #FFFFFF | 提示正文 | 浅色底正文 |
| Ripple | #1F302B2A | Ink约12%的点击反馈 | 永久染色 |

白字/默认Primary对比约4.86:1；TextTertiary/暖米背景约4.71:1。其他组合包括状态标签也须验算，普通文字≥4.5:1，大字及必要非文字边界≥3:1；禁用控件不承担正常内容可读性职责。输入框不能只有低对比卡片Border，必须用InputBorder、标签和Focus。

### 2.1 五主题兼容

保留现有 `warm/mint/sky/sakura/lilac` key、名称和按账号保存方式。只能由主题适配器替换下列角色；正文中性色与状态语义保持一致，白色浮层绝不跟随紫藤变紫。表中是迁移目标，不代表已改现有Theme.kt。

| key | AppBackground | Surface | ElevatedSurface | Primary / Focus | PrimaryPressed | PrimaryContainer |
|---|---|---|---|---|---|---|
| warm | #FFFBF2 | #FFFCF4 | #FFFFFF | #A65E49 | #8D4C3B | #F3E3DC |
| mint | #E8F4EC | #F4FBF7 | #FFFFFF | #4C7A5C | #3D634A | #DDEEDC |
| sky | #E9F1FA | #F6FAFF | #FFFFFF | #3F6F96 | #335B7C | #DCECF2 |
| sakura | #FBF0F3 | #FDF8FA | #FFFFFF | #9E526A | #834257 | #F6E2E9 |
| lilac | #F1EDFA | #FAF8FF | #FFFFFF | #6E5B99 | #59487E | #E7DDF4 |

每主题PrimaryDisabled统一#E8DDD7，Secondary/状态等沿主表。底部导航采用AppBackground，indicator用PrimaryContainer小面积。删除dashboard/backgroundDeep作为额外大面积背景角色；旧值只能在适配过渡期保留，不能供新页面直接使用。白色汇总、成员及任务详情面板复用ElevatedSurface的**同一白色值**，不创造第四底色；工程可用 `panelSurface` 语义别名指向该Token，不附加浮层阴影。

### 2.2 状态与优先级

| 数据语义 | 文字/图标 | 小面积底 | 必需文案 |
|---|---|---|---|
| NOT_STARTED | TextSecondary | NeutralContainer | 未开始 |
| IN_PROGRESS | Info | InfoContainer | 进行中 |
| PENDING_ACCEPTANCE | Warning | WarningContainer | 待验收 |
| COMPLETED / APPROVED | Success | SuccessContainer | 已完成 / 已批准 |
| review PENDING | Warning | WarningContainer | 待审批 |
| review REJECTED | Error | ErrorContainer | 已驳回 |
| 逾期且未完成 | Error | ErrorContainer | 已逾期；保留真实执行状态 |
| 优先级1/2 | TextSecondary | NeutralContainer | 很低 / 一般 |
| 优先级3 | TextSecondary | NeutralContainer | 普通 |
| 优先级4 | Warning | WarningContainer | 重要 |
| 优先级5 | Error | ErrorContainer | 紧急 |

UI仅映射服务端已存在的值，不创建新枚举；未知值显示“状态未知”+NeutralContainer，不映射为成功。多执行人状态仍按现有汇总规则，只统一呈现。颜色必须辅以文字/图标，逾期不能覆盖完成态。当前时间线使用Info，不混用Error红造成误解。

### 2.3 业务标签与头像例外

服务端 `colorHex` 为业务数据，不受固定品牌色数量限制，不能修改数据库颜色来实现视觉统一。所有任务/审批/创建标签主标签排最前，其他维持原顺序。标签使用原色100%填充+Border1dp，主标签附“主”标识；取消各页35%/40%透明和独立lerp加深。动态色太浅通过中性边框分界，不能更换色相；文字在TextPrimary与白色中按更高对比度选择。无效颜色使用NeutralContainer。标签色不用于普通操作按钮。日志标签可着色，日志正文/整卡不染色。

新建标签的候选色可在后续独立改造中统一调整，但不得批量重写已有标签；头像占位色板、系统相册/相机UI保留业务/平台例外。改造不影响后端返回的电话、姓名与权限。

## 3. Typography System

`FontFamily.Default`，不加字体文件；Compose `sp`跟随系统字体缩放与现有应用偏好，两者只在根层处理一次。中文采用系统fallback。全部letterSpacing=0sp；默认TextAlign.Start，日期/导航/居中按钮显式Center。常规正文不设固定高度。

| Token | 字号 | FontWeight | 行高 | 场景/Material映射 |
|---|---:|---|---:|---|
| Display | 28sp | Bold(700) | 36sp | 登录品牌/少量占位；headlineMedium |
| TitleLarge | 24sp | SemiBold(600) | 32sp | 页面标题；headlineSmall |
| TitleMedium | 20sp | SemiBold(600) | 28sp | 分区、对话框标题；titleLarge |
| TitleSmall | 18sp | Medium(500) | 24sp | 卡片标题；titleMedium |
| BodyLarge | 16sp | Normal(400) | 24sp | 正文/输入；bodyLarge |
| BodyMedium | 14sp | Normal(400) | 20sp | 辅助正文/任务条标题；bodyMedium |
| LabelLarge | 14sp | Medium(500) | 20sp | 按钮/Tab/标签；labelLarge |
| Caption | 12sp | Normal(400) | 16sp | 时间/元数据/导航；bodySmall与labelSmall |

Material其余Typography槽必须显式引用上述八级中的一种（不新增字号）：display*→Display；headlineLarge→Display；titleSmall→LabelLarge；labelMedium→Caption。只为日历概览允许 `CalendarMicro=10sp/14sp/Normal`，仅年概览日期与月任务条；不得用于可编辑表单、标签和主要说明。概览文字无法独立满足大字体阅读时依赖已有下钻/任务详情，并提供完整semantics；大字体优先减少同屏数量或滚动，不能强行缩小到8sp。

摘要规则：任务标题最多2行ellipsis、日志摘要1行ellipsis、人员姓名默认1行并可进入详情；完整标题/正文在详情换行不截断。按钮字过长允许增长或单列；不得固定height裁掉中文行高。

## 4. Spacing、Grid 与 Insets

| Token | dp | 常见用途 |
|---|---:|---|
| Space2 | 2 | 元数据微间隙；不能当页面内距 |
| Space4 | 4 | 紧凑文字组/分组内部 |
| Space8 | 8 | 图标与文字、按钮间、紧凑条目纵内距 |
| Space12 | 12 | 标题→内容、列表纵内距、卡片内业务条目横内距 |
| Space16 | 16 | 页面水平、普通面板内距、紧凑面板间距 |
| Space20 | 20 | 特定双行表单组（须组件统一使用） |
| Space24 | 24 | 普通分区间、弹窗内容、登录水平边距 |
| Space32 | 32 | 大分组间距 |
| Space40 | 40 | 占位图文分隔，仅空态 |
| Space48 | 48 | 大模块分隔；触控尺寸另用TouchTarget |

其他几何Token：`BorderWidth=1dp`、`FocusWidth=2dp`、`IconSmall=16dp`、`Icon=24dp`、`TouchTarget=48dp`。禁止把1dp边框解释为spacing例外。透明触区不可覆盖相邻操作。

标准内容左右16dp；顶部在TopAppBar已消费insets后16dp；标题到内容12dp；普通分区24dp；汇总/成员详情面板间16dp（业务紧凑例外）；普通Panel内16dp；任务/日志独立条目横12、纵8、内部4/8；普通列表纵12。底部16dp+真正未消费的安全区；浮动动作不得遮住最后一项。

四列栅格：外边距16，列间8，单列宽 `(availableWidth−32−24)/4`。360dp→76dp列宽，393dp→84.25dp；两列跨度=`2列+8`，整行=`4列+24`。如果嵌入面板内，先再扣面板左右16：360屏内列宽68dp，393屏内列宽76.25dp。直属四人按面板内可用宽等分，各项中心对齐、头像40dp、最小48触达，不继续写死68dp再加12dp间隔。超长姓名省略可进详情；字体放大时允许横向滚动而不裁字。

宽度<360沿相同16dp边距，表单操作可堆叠；600–839dp可用8列（16列距），内容最大720dp居中；≥840dp内容最大960dp、12列/16间距，长正文最大640dp。仍保留五项底栏，不自动切换侧栏/新增双详情导航。只有视觉容器响应式，不修改信息流。

日历是允许的布局例外：七列按日期等分；内容左右8dp，刻度栏40dp；日每小时60dp、周56dp；月连续条18dp最小视觉高/3dp角可暂保留，点击必须有可达的选择列表与下钻。不可把跨日拆成独立每日任务来适配栅格。登录水平24dp、最宽480dp。所有例外放组件内，页面不再添加任意偏移。

Insets：根Scaffold消费systemBars后向子页传剩余padding，子页不得再重复statusBarsPadding。BottomSheet/全屏Dialog单独处理systemBars和IME；评论/提交条在键盘上方，滚动内容必须可到达；导航高度不含系统手势区。横屏、刘海、三键导航和手势导航都需截图验收。

## 5. Radius 与 Elevation

| Token | 数值 | 使用 |
|---|---:|---|
| RadiusSmall | 8dp | 小型非胶囊控件、元数据容器 |
| RadiusMedium | 12dp | Button、TextField、Dropdown、独立任务/日志条目 |
| RadiusLarge | 16dp | 普通Panel/事项卡片 |
| RadiusXLarge | 20dp | 仅明确大型编辑容器，不能套普通卡片 |
| RadiusDialog | 24dp | Dialog，Sheet仅顶部两角 |
| RadiusPill | 50%（语义上等同足够大胶囊） | Chip/筛选，Compose RoundedCornerShape(percent=50) |
| RadiusCircle | CircleShape | 头像、圆形图标 |

Pill不在Compose写999dp而使用百分比，避免不同高度失真。Shape映射small=8、medium=12、large=16、extraLarge=24（Material浮层）；20dp走显式自定义容器Token。普通Panel0dp阴影+1dp Border；交互条目0dp+细框或分割线二选一；Dropdown3dp、Dialog6dp、Sheet3dp、Snackbar0dp。遮罩已建立层级时不添加10–14dp厚阴影。

## 6. Button System

统一常规按钮最小高48dp/最小宽64dp/横16dp/RadiusMedium/LabelLarge；文字可使其增长；视觉紧凑按钮40dp高、横12dp，外围触区48；仅标签式小入口可32dp视觉高，仍48dp触区。全宽提交同样48，不另设“大按钮圆角”。

| 变体 | 默认背景/文字/边框 | 按压 | 禁用 |
|---|---|---|---|
| Primary | Primary / TextOnPrimary / 无 | PrimaryPressed | PrimaryDisabled / TextDisabled |
| Secondary | PrimaryContainer / TextPrimary / 无 | Primary 12%叠层 | NeutralContainer / TextDisabled |
| Outline | 透明 / Primary / Border1dp | Ripple | 透明 / TextDisabled / Divider1dp |
| Text | 透明 / Primary / 无 | Ripple | 透明 / TextDisabled |
| Destructive | 透明 / Error / Error1dp | ErrorContainer | 透明 / TextDisabled / Divider1dp |

最终确认删除可使用Error实底/白字，不能让同一页面同时多种危险主按钮。取消统一Secondary或Text（与所在对话框整体一致），不得看起来比确认更强调。hover（鼠标）PrimaryContainer/中性叠层、focus 2dp Focus、pressed走表中、loading禁用重复提交并显示16dp进度+“保存中…”（保留按钮宽度）；加载不能消失原label导致布局跳动。错误显示字段/操作区文字，不把按钮随机染红。Android ripple遵循系统减少动画偏好。

IconButton：24dp图标、48dp触区；默认TextSecondary、选中Primary、危险Error；无背景或PrimaryContainer选中底。触区要与点击回调semantics绑定；不要给装饰图标重复读名称。

## 7. Modal & Overlay System

| 组件 | 背景/形状 | 尺寸/边距/层级 |
|---|---|---|
| Alert/Confirm/Custom Dialog | ElevatedSurface，24角 | `min(窗口宽−48dp, 560dp)`；普通内容24dp内距；内容过高可滚动 |
| ModalBottomSheet | ElevatedSurface，顶部24角 | 手机全宽；宽屏最大640dp；内容横16dp、底16dp+IME/insets；顶部默认拖柄 |
| Dropdown/接收人Popup | ElevatedSurface，12角 | 最小112dp、最大320dp且≤窗口−32dp；每项≥48dp；高不超过可用窗口50% |
| Snackbar | Ink/白字，12角 | 离内容底16dp及底栏/insets；正文BodyMedium，操作LabelLarge；无额外遮罩 |
| 通知抽屉 | ElevatedSurface | 最大360dp或窗口90%；保留现有右侧进入语义，遮罩同Scrim |

所有模态Scrim统一Black40%；SurfaceTint透明。不能只改M3默认而漏掉自制覆盖Card和全屏Dialog。全屏历史日期选择是模态编辑工具，白底，标题栏/系统区域也按白色浮层处理；相册/相机系统UI例外。

标题TitleMedium，正文BodyLarge，标题→正文16dp，正文→动作24dp。动作右对齐：取消在前、确定在后，间距8dp；放不下换竖排每项全宽48dp。确认LabelLarge/Primary，危险确认Error，取消TextSecondary。标题描述真实动作，不放实现说明、装饰性AI小字。

层级顺序：内容→菜单/锚定Popup→模态Scrim→Dialog/Sheet→可访问暂态提示；一次只激活一个交互模态。Back返回最顶浮层，有未保存表单时走既有确认，不更改保存规则。Dialog焦点停留在浮层内，关闭后回到触发控件；日期/时间滚轮选中行48dp，整体5行240dp，不把未选中数值降到无法阅读的alpha。

## 8. Card、List、Input 规范

| 组件 | 底/角/框/阴影 | 内距/排版/高度 |
|---|---|---|
| PandoraPanel | Surface；白色功能面板用ElevatedSurface；16角/Border1/0阴影 | 16内距；标题TitleSmall→内容12；内容自适应 |
| TaskItem | Surface/12角/Border1/0 | 横12纵8、内部4/8；标题BodyMedium最多2行，标签LabelLarge、时间Caption，头像24/32；按内容增长，无固定高 |
| JournalItem | 同TaskItem | 作者BodyMedium，时间Caption，摘要BodyMedium单行；最小72dp，不堆在一起 |
| ImportantItem | Surface/16角/Border1/0 | 横16纵12；标题BodyLarge，元数据Caption；无彩色侧条 |
| PersonListItem | 继承面板底/无重复框 | 头像40、间隔12、文字16/14；单行最小56、双行最小72、三行最小88dp |
| OrganizationItem | 同PersonListItem | 每层缩进12dp，最多显示3级视觉缩进36，层级关系通过文本/展开表达；不改组织树/权限 |
| SettingsItem | 同PersonListItem | 图标24、间隔8、纵12；双行增长；分割线跟文字起点对齐 |
| TagChip | 原业务色/胶囊/Border1/0 | 横8纵4，LabelLarge；主标签前置并加“主” |
| StatusChip | 语义Container/胶囊/0 | 横8纵4，Caption；文字/状态图标不能只用色块 |
| TextField | Surface/12角/InputBorder1；focus2；error Error | 单行最小56dp，横16；BodyLarge；label BodyMedium；多行至少3行并可增长 |

行之间采用8dp间隔和独立1dp框，或0间隔+1dp Divider，**同类列表选择一种**；任务/日志各自独立框以满足已确认的清晰分界。不要在条目框下再叠加重分割线。点击整个条目有ripple与role；子级按钮独立操作且不串触发。加载、空态、失败重试、无权限、部分加载分别显示，不能用“暂无”掩盖错误。

任务/日志/人员共享视觉基础，各自信息结构保持；不强行统一固定高度。执行记录维持内联单条分界；禁止恢复“任务说明”只读弹窗。输入错误就地展示并可被读屏读到；loading禁重复提交，离线失败不丢草稿。

## 9. Icon & Navigation System

复用 `material-icons-extended` 现有Material图标，默认Filled；方向图标使用AutoMirrored，避免混入随机emoji作操作图标。普通24dp、元数据16dp、导航24dp、占位插图64dp；普通TextSecondary，选中Primary，错误Error。图标与文本8dp。Material图标描边由资产决定，不手动改粗细；已有星形优先级保留文字辅助。

BottomNavigation视觉内容高80dp+系统手势insets，五等分；图标24、label Caption12/16，选中Primary/PrimaryContainer，未选TextSecondary。小红点/数量仅真实未读提醒，来自既有业务，不做统计卡。TopAppBar最小64dp（可随标题增长），标题TitleLarge、返回48触达。Tab最小48dp，LabelLarge、Primary2dp下划线，未选TextSecondary/透明；多行增长不裁切。标题右侧任务汇总为TextButton紧凑入口，不放大卡片。保持全部路由和返回语义。

## 10. Compose工程映射与命名

当前仅Theme.kt集中AppTheme/别名/Material，通用RemoteAvatar，其他基础容器分散。以下是未来迁移文件，**本次不创建Kotlin实现，也不移动现有页面目录**：

| 文件（ui/下） | 职责与Token |
|---|---|
| theme/Color.kt | `PandoraColorTokens`：lowerCamel字段appBackground/surface/elevatedSurface/textPrimary/primary等；五主题表；标签色数据适配 |
| theme/Type.kt | `PandoraTypography`八级+calendarMicro，映射完整M3 Typography |
| theme/Dimens.kt | `PandoraSpacing.space2…space48`、`PandoraDimensions.touchTarget/icon…`、`PandoraGrid`规则 |
| theme/Shape.kt | `PandoraShapes.small/medium/large/xLarge/dialog/pill/circle`，M3 Shapes映射 |
| theme/Theme.kt（现有） | 主题选择→统一Token→MaterialTheme；保留旧别名作为deprecated转接，逐批迁移后删除 |
| components/PandoraButton.kt | 明确variant、loading、enabled，禁止调用者自由指定主操作containerColor |
| components/PandoraPanel.kt | 基础Surface+内距/边框；业务Panel调用 |
| components/PandoraDialog.kt | Confirm/Alert/Sheet外观与安全区包装，支持已有内容 |
| components/PandoraTagChip.kt | 原色/主标签顺序/对比度/边框；与StatusChip分离 |
| components/PandoraTextField.kt | 错误/标签/IME/单多行一致 |
| components/PandoraListItem.kt | 人员/设置的共享基础；不硬塞任务模型 |
| components/RemoteAvatar.kt（现有） | 保留仓库/缓存逻辑，接入尺寸Token与语义 |

M3映射：primary=Primary、onPrimary=TextOnPrimary、secondary=Secondary、primaryContainer=PrimaryContainer、background=AppBackground、surface/surfaceContainer*=ElevatedSurface（确保默认浮层白）、surfaceTint=Transparent、onSurface/onBackground=TextPrimary、error=Error、outline=InputBorder、outlineVariant=Border、scrim=Scrim。普通Card显式用自有Surface，不能靠surfaceContainer默认推测。AppTheme/MaterialTheme必须由同一已选key生成，不能再固定另一个Coral Scheme。

文档Token用PascalCase标识；Kotlin字段lowerCamel并按类型域组织；组件`Pandora`前缀。页面只组合组件与业务数据，不重新定义颜色、角和间距。非业务绘图中不允许页面内Hex；数据colorHex解析只在Tag适配器，Avatar/Calendar专用值在对应Token域。状态映射集中一个UI映射函数，不让每页自写when颜色。

## 11. 组件约束、例外与验收

所有交互组件覆盖default/pressed/focus/disabled/loading/error；支持鼠标时补hover。动画150–250ms、推荐200ms，遵循系统关闭动画，不添页面装饰入场。按钮忙碌语义、错误liveRegion、Tab选中、人员/日期完整描述可被TalkBack读取。非交互图标description=null，独立按钮不为null。

例外白名单仅：日历专用几何/微文字；品牌登录Display；业务标签与头像色；用户明确白色功能面板；平台相册/相机。新增例外必须在对应OpenSpec和本文件记录组件、数值、原因、回归截图；不能以“好看”申请新橙色。

每批验收至少：360/393/600dp、1.0/1.3/2.0字体、五主题、长中文/空态/错误/加载/无权限、IME与系统导航两模式。检查48dp命中区、正常文字4.5:1、对话框白底、标签原色/主标签优先、五底栏未变、详情正文可读。Compose构建和必要回归测试通过后才替换旧Token；截图只在测试账号与授权范围，不修改真实业务记录来制造状态。

## 12. 平台依据

48dp触控区域及Material组件默认触区扩展依据 [Android Compose可访问性默认行为](https://developer.android.com/develop/ui/compose/accessibility/api-defaults)。因此审计中的34/40dp视觉尺寸只标记为需验证，不能据此断言命中区必然不足。安全区处理依据 [Android Window Insets](https://developer.android.com/develop/ui/compose/system/insets)；组件默认处理不免除自制浮层检查。具体品牌色、栅格、圆角是Pandora本次确定的产品规范，不声称是Android强制标准。
