## 1. P0 设计基础层

- [x] 1.1 新增 Color.kt（五主题语义 Token）、Type.kt、Dimens.kt、Shape.kt
- [x] 1.2 Theme.kt 由所选主题 key 统一生成 LocalAppTheme、语义 Token 与 Material3；旧别名转接为 deprecated
- [x] 1.3 新增 PandoraButton（Primary/Secondary/Outline/Text/Destructive，loading）与 PandoraPanel
- [x] 1.4 集中状态/优先级颜色映射并补充对比度与映射单元测试
- [x] 1.5 试点：导图与视图页面改用 Token 与基础组件
- [x] 1.6 P0 验证：assembleDebug、testDebugUnitTest、openspec validate、五主题检查

P0 记录：五主题导图/视图及日月年模式已在 Pixel_9a 模拟器（1080×2424，420dpi≈411dp 宽）对接隔离的本地后端与测试账号截图检查。对比度单元测试发现规范原值三处不足，已最小幅度修正（见 design.md 决策 7）。

## 2. P1 公共组件

- [x] 2.1 Dialog/AlertDialog/自制浮层统一白底、24dp、40% 遮罩与按钮顺序
- [x] 2.2 BottomSheet、DropdownMenu/Popup 统一外观
- [x] 2.3 TextField、TagChip/StatusChip、Tab、ListItem、IconButton、Snackbar 提示统一
- [x] 2.4 底部导航与 TopAppBar 按规范显式配置
- [x] 2.5 P1 验证：构建、单测、组件五主题检查

P1 记录：新增 PandoraAlertDialog/Dialog/BottomSheet/DropdownMenu/TextField/TopBar/TabRow/ListItem/Toast/IconButton；全部 AlertDialog、OutlinedTextField、ModalBottomSheet、DropdownMenu 调用改用统一组件，主题 backgroundDimAmount=0.4。历史日期全屏选择器改为白色浮层并自理系统栏。模拟器核对：暖米/紫藤下弹窗为纯白、任务表单 Sheet、审批两队列 Tab。观察到审批中心列表页无系统返回处理（改造前已存在，未改动，记入验收报告）。

## 3. P2 页面迁移

- [ ] 3.1 导图二级（展开、事项详情、个人事项编辑、通知抽屉）
- [ ] 3.2 视图/任务（日历、新建/编辑、任务详情、执行进度）
- [ ] 3.3 任务汇总、组织架构、成员详情
- [ ] 3.4 日志（今日、历史、编辑、详情）
- [ ] 3.5 AI 地图、我的、设置、审批、标签管理、登录
- [ ] 3.6 P2 验证：构建、单测、五个一级页面与二级跳转检查

## 4. P3 验收

- [ ] 4.1 重跑静态盘点，区分硬编码、Token 定义、主题映射与业务色
- [ ] 4.2 五主题、360/393dp、字体缩放检查与截图（环境允许时）
- [ ] 4.3 局部修复发现的偏差
- [ ] 4.4 更新三份 UI 文档并新增验收报告
- [ ] 4.5 最终构建、单测、openspec validate --strict
