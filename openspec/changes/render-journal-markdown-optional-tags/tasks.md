## 1. 实现

- [x] 1.1 移除日志标签必选校验，标注标签可选并保留清空语义。
- [x] 1.2 引入 Markdown 依赖与主题适配组件，接入日志卡片和详情，编辑仍保留原文。

## 2. 验证

- [x] 2.1 补充渲染测试，验证普通文本、预填待办、混合 Markdown 与原文不变。
- [x] 2.2 运行 Android 构建、测试与 OpenSpec 严格校验，记录结果和设备验证限制。

## 3. 阅读排版修正

- [x] 3.1 移除编辑器灰色能力提示，并使列表后未缩进普通文本作为顶格段落渲染。
- [x] 3.2 验证普通/任务/有序列表边界、显式续行、嵌套及代码块，重新构建和严格校验。

## 4. 分区标题与段落间距

- [x] 4.1 简化编辑标题，移除装饰点和标签括号说明；避免 Markdown 块边界叠加空行。
- [x] 4.2 补充列表/嵌套列表与普通段落间距验证，运行构建和严格校验。

分区标题与段落间距验证：
- 补充设备测试断言：普通列表、任务列表、有序列表、嵌套列表到正文只换一行，段落边界不叠加空行，代码块内部空行保留。
- `:app:assembleDebug :app:assembleAvatarQaAndroidTest` 使用上述单工作进程及临时 4GB 堆参数通过。
- 检测到连接设备后运行 `:app:connectedAvatarQaAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pandora6ix.app.ui.logs.JournalMarkdownDeviceTest`，安装阶段被设备以 `INSTALL_FAILED_ABORTED: User rejected permissions` 拒绝，未执行测试；未尝试绕过设备安装确认。
- OpenSpec 严格校验与差异检查通过。设备上的实际渲染仍待验证。

排版修正验证：
- `JournalMarkdownSourceTest` 使用 Debug 编译产物、现有 CommonMark 依赖，经 `javac -encoding UTF-8` 编译后通过 JUnitCore 独立执行：4 项测试全部通过。采用针对性执行以避开上述现有 SummaryDataTest 编译阻断；不代表全量单元测试通过。
- `:app:assembleDebug :app:assembleAvatarQaAndroidTest --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process "-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8"` 通过。
- 补充设备测试，断言普通说明文本不带 LeadingMarginSpan；本次未执行设备测试。
- OpenSpec 严格校验和 `git diff --check` 通过。

验证记录：
- 首次同时构建 Debug 和 AvatarQa 时 Kotlin 编译内存不足。采用 `--max-workers=1 -Pkotlin.compiler.execution.strategy=in-process "-Dorg.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8"` 重试，不修改持久构建配置。
- `:app:assembleDebug :app:assembleAvatarQaAndroidTest` 重试成功，包含新增 JournalMarkdownDeviceTest 的测试包编译及打包通过。
- 最终原文保留调整后重新运行 `:app:assembleDebug :app:testDebugUnitTest`：assembleDebug 通过；单元测试被现有 SummaryDataTest.kt 第 60–64 行缺少 businessTimezone 参数的编译错误阻断，未实际执行。
- ADB 未发现已连接设备，因此新增设备渲染测试和发布/编辑完整流程未运行。
- `openspec validate render-journal-markdown-optional-tags --strict` 与 `git diff --check` 通过。
- 静态核对 create、同日合并及 patch 均保留正文源码，空标签创建与清空标签沿用现有请求字段语义。变更待评审，不提前归档。
