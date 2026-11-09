## Why

课程设计甲方（老师）验收公司页面时提出：任务汇总要像他们现在使用的在线表格一样，一行一个任务、按列看截止日期、紧急程度、完成情况、进度备注和负责人；卡片列表不被认可，手机上需要滑动也可以接受。同时公司页面只需要看自己的任务，下属与下属日志改放到单独的“通讯录”入口，并能按日期查看下属日志。

## What Changes

- **公司页面**：删除顶部“我的任务 / 我管理的 / 我创建的”范围选项，日历固定显示自己的任务（`scope=mine`）。
- **公司页面顶部**：在“任务汇总”左侧新增“通讯录”按钮。
- **任务汇总**：去掉“直属下属”和“最近日志动态”两块，只保留任务；任务改为表格展示：
  - 列：任务名称（固定在左侧）、截止日期、紧急程度、完成情况、进度备注、负责人；
  - 表头固定，其余列可左右滑动，所有行同步滑动；按主标签分组显示组标题行；
  - 保留“我的任务 / 我创建的 / 下属任务”切换与分页加载，点任务行进入原任务详情；
  - 提供“横屏查看”按钮，横屏时一屏可以看到全部列。
- **通讯录**（新页面）：承接原任务汇总中的直属下属、组织架构、成员详情（个人信息、电话、负责的任务、最近日志）与下属日志。
- **下属日志按日期筛选**：通讯录的下属日志与成员日志列表支持“全部 / 今天 / 近 7 天 / 本月 / 指定日期”筛选，使用已有 `GET /work-logs` 的 `date/from/to` 参数。
- **后端**：任务列表项新增 `latestProgressNote`（当前用户可见执行关系里最近一条非空进度备注），供表格的“进度备注”列使用；任务详情同样返回该字段。

## Capabilities

### New Capabilities
- `android-contacts`：通讯录入口、直属下属与组织架构、成员详情、下属日志的日期筛选。

### Modified Capabilities
- `android-task-summary`：任务汇总只保留任务，改为表格展示；下属与日志移到通讯录。
- `summary-panel-previews`：删除三面板总览、任务预览/全部任务页与人员/日志预览的要求。
- `calendar-view`：公司页面只显示自己的任务，入口行增加通讯录。
- `backend-task-workflow`：任务视图新增 `latestProgressNote`。

## Impact

- Android：`ui/calendar/CalendarScreen.kt`、`MainActivity.kt`、`ui/summary/*`（新增 `TaskSummaryTable.kt`、`ContactsFlow.kt`）、`AndroidManifest.xml`（MainActivity 处理屏幕旋转，横屏查看不重建界面）、`data/DashboardModels.kt`。
- 后端：`TaskService` 任务视图、`backend/openapi.json` 与生成脚本、集成测试。
- 不涉及数据库结构、权限规则与 Web 端。
