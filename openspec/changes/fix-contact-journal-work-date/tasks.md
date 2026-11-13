## 1. 日志日期展示

- [x] 1.1 修改共用 JournalRow，独立显示工作日期及带标签的发布时间，并处理空字段。

## 2. 验证

- [x] 2.1 核对通讯录全部日志入口复用该组件，运行 Android 构建、单元测试、OpenSpec 严格校验及差异检查，记录结果。

验证记录：
- ContactsFlow 的全部下属/成员列表及最近日志预览均调用 JournalRow，已核对。
- `android/gradlew.bat :app:assembleDebug :app:testDebugUnitTest`：assembleDebug 通过，单元测试源码编译通过；testDebugUnitTest 因测试进程无法加载 `worker.org.gradle.process.internal.worker.GradleWorkerMain` 启动失败，未取得测试通过结果。
- `openspec validate fix-contact-journal-work-date --strict`：通过。
- `git diff --check`：通过。
- 未进行设备界面验证。
