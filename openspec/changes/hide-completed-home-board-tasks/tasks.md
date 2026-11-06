## 1. 实现

- [x] 1.1 实现按当前执行人状态过滤的首页分页函数，并接入首次加载与加载更多。
- [x] 1.2 添加本人/他人完成状态、待验收、连续空页、分页失败与游标保护测试。

## 2. 验证

- [x] 2.1 运行针对性单元测试、Android debug 构建、差异检查与 OpenSpec 严格校验并记录结果。

验证结果：
- `:app:assembleDebug`：通过。
- `:app:testDebugUnitTest --tests com.pandora6ix.app.data.HomeTaskBoardTest`：代码及测试编译通过，Gradle Test Executor 因 `GradleWorkerMain` ClassNotFoundException 未能启动。
- 使用 JDK 21 `java -cp <debug/debugUnitTest 编译目录与缓存中的 JUnit 4.13.2、Hamcrest 1.3、Kotlin stdlib 2.0.21、coroutines 1.9.0、serialization-core 1.7.3 JAR> org.junit.runner.JUnitCore com.pandora6ix.app.data.HomeTaskBoardTest` 直接执行同一测试类：6 项全部通过。
- `git diff --check`、`openspec validate hide-completed-home-board-tasks --strict`：通过。
- 未执行设备界面验收；待确认真实账号下首页、展开计数和点击详情。
