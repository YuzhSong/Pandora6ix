## 1. 合入行为

- [x] 1.1 保留 PR #57 的任务可见范围、执行列表、最新编辑时间及通知行为。
- [x] 1.2 在 PR #56 的汇总布局上应用刷新按钮移除，并同步横屏布局规格及测试。

## 2. 验证

- [x] 2.1 在权限测试中按执行人 ID 选中当前用户的执行关系，验证后端全量打包及 OpenAPI。
- [x] 2.2 构建 Android 应用和设备测试包，记录 Android 单元测试结果。
- [x] 2.3 验证 OpenSpec 及待合入的源码差异。

## 验证结果

- 后端 `mvnw.cmd -B -ntp -Duser.timezone=UTC package` 通过全部 177 项测试，原间歇失败的用例单独重复执行 6 次全部通过。OpenAPI 校验通过。
- Android `:app:assembleDebug :app:assembleAvatarQaAndroidTest` 通过。`testDebugUnitTest` 在已有 `SummaryDataTest` 调用缺少 `businessTimezone` 参数处编译失败。设备测试完成打包，尚无实机执行结果。
- `openspec validate align-public-task-visibility --strict`、横屏布局 OpenSpec 严格校验和 `git diff --check` 通过。
