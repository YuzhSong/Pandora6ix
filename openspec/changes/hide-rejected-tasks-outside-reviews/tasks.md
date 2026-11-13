## 1. 普通任务可见性

- [x] 1.1 在共用任务页和日历入口排除创建审批已驳回的任务，保留分页元数据及审批历史读取。

## 2. 验证

- [x] 2.1 覆盖混合状态、验收驳回、全隐藏页后续分页及日历过滤，核对所有任务入口与审批独立数据源。
- [x] 2.2 运行 Android 构建、针对性回归、OpenSpec 严格校验及差异检查，记录验证限制。

验证记录：
- `:app:assembleDebug :app:testDebugUnitTest --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process "-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8"`：assembleDebug 通过；全量单元测试在既有 SummaryDataTest.kt 第 60–64 行因 businessTimezone 参数缺失而编译失败，未实际执行全量测试。
- 将 TaskVisibilityTest.java 使用 `javac -encoding UTF-8` 与最终 Debug Kotlin 编译产物、现有序列化/JUnit 依赖独立编译，经 JUnitCore 运行 4 项测试全部通过：混合状态和原对象保留、全隐藏页游标、验收驳回任务保留、日周月日历元数据保留。
- 核对 ContactsFlow 的下属任务分页/预览、TaskSummaryFlow 的三个范围、MainActivity 的日周月年视图与首页任务均通过 CalendarRepository；审批中心仍通过 TaskRepository.reviews 获取历史，未应用普通任务过滤。
- `openspec validate hide-rejected-tasks-outside-reviews --strict` 与 `git diff --check` 通过。
- 未安装 APK 或进行设备界面验证；未修改后端、提交或归档变更。
