# 设计说明

## 面板构成

首页从 2×2 四面板改为「上排两个业务面板 + 下方每日事项整行」：

- 事件纪（原公司十大重要事项）：数据源不变（dashboard.companyImportantItems，含游标分页）。
- 任务看板（原公司十大派发任务）：数据源改为 `CalendarRepository.tasks(scope = "mine")`，每个执行人都能看到被派发给自己的任务；分页直接走该接口的 cursor（不再复用 dashboard.more 的 moreUrl 拼接，避免 query 参数冲突）。
- 个人十大重要事项、个人日志面板移除；`PersonalImportantEditor` 代码保留但无入口（后续清理），日志能力仍在日志页。

## 每日事项

- 模型 `DailyItem(id, date, content, done)`，date 为 ISO `yyyy-MM-dd`；与任务体系完全解耦。
- 存储 `DailyItemsStore`：SharedPreferences（文件名按账号 id 隔离，与 NotificationPrefs 同一模式），JSON 序列化整个列表；服务端接口接入时仅需替换该类实现。
- UI `DailyItemsPanel`（ui/components）：PandoraPanel 容器；月历与公司页月视图一致的周一开头网格（java.time YearMonth 计算，项目中已启用 coreLibraryDesugaring 同类用法）；选中日 primaryContainer 圆底高亮、今天主色加粗；打点规则「有未完成=主色 / 全完成=弱色」。清单行使用圆形勾选控件（与既有编辑器圆形勾选语言一致）。所选日期与月份用 rememberSaveable 保持，配合首页 SaveableStateProvider 跨 tab 切换不丢。
- 日期状态在面板内部持有；数据列表由 HomeScreen 持有并通过 `mutateDaily`（改列表 + 落盘）回调，保证单一数据源。

## 命名

- 导航标签是纯展示字符串（NavItem.label），无路由/存储影响；Route 枚举名保持 HOME/VIEW 不变，避免破坏 SaveableStateProvider 的 key 与推送路由逻辑。

## 备选方案

- 任务看板继续用 dashboard.companyTasks：无法满足「执行人可见」，弃用。
- 每日事项建后端表 + API：超出本次前端改版范围，先本地持久化，接口接入点集中在 DailyItemsStore。
