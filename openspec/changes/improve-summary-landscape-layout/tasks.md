## 1. 布局修复

- [x] 1.1 修复汇总页系统栏边距消费，横屏隐藏底部主导航。
- [x] 1.2 添加紧凑横屏工具栏及范围下拉，表格填满剩余高度，保留竖屏与列表状态。

## 2. 验证

- [x] 2.1 添加短横屏高度与范围切换的 Compose 回归测试，验证表格视口不再被标题区挤压。
- [x] 2.2 运行 Android 构建、OpenSpec 严格校验及差异检查，尝试独立设备测试并记录结果。

验证记录：
- `:app:assembleDebug :app:assembleAvatarQaAndroidTest --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process "-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8"` 通过，包含三个新增 Compose 布局测试的编译打包。
- 测试覆盖 280dp 容器中表格高度至少 220dp、标题靠顶、范围下拉切换及滚动至第 20 个任务后表头仍可见。以上为新增断言，尚未取得设备执行通过结果。
- 对已连接设备运行 `:app:connectedAvatarQaAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pandora6ix.app.ui.summary.TaskSummaryLayoutDeviceTest`；安装阶段因 `INSTALL_FAILED_ABORTED: User rejected permissions` 失败，实际执行 0 个测试。未覆盖用户正式应用或绕过设备安装确认。
- `openspec validate improve-summary-landscape-layout --strict` 与 `git diff --check` 通过。
- 保留上一项隐藏已驳回任务的未提交修改；本次未提交或归档变更。
