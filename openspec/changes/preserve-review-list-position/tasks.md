## 1. Implementation

- [x] 1.1 提升列表滚动状态，按队列隔离列表与分页状态
- [x] 1.2 刷新完整已加载分页窗口，失败保留数据，避免刷新与加载更多冲突
- [x] 1.3 提升“我的”滚动状态到导航层，子页面返回保留位置并按账号重置

## 2. Validation

- [x] 2.1 添加多页刷新、末页缩短及失败的回归测试
- [x] 2.2 调试 APK 构建与 OpenSpec 严格校验（含“我的”页面修复）
- [ ] 2.3 运行 Android 单元测试
- [ ] 2.4 设备验证深处记录详情往返、系统返回、定时刷新和加载更多
- [ ] 2.5 验证“我的”从审批中心、设置返回的位置及账号切换重置

验证记录：`git diff --check` 与 `openspec validate preserve-review-list-position --strict` 通过。Gradle 的 `:app:assembleDebug` 成功，主代码和测试代码均编译成功；`:app:testDebugUnitTest` 的测试进程无法加载 `worker.org.gradle.process.internal.worker.GradleWorkerMain`，未执行成功。使用仓库现有 verification-user-home 缓存重跑的提权请求被用户拒绝，未继续重试。设备 UI 验证尚未执行。

追加“我的”页面修复后重新执行 `:app:assembleDebug`，因用户 Gradle 缓存锁文件不可写未能启动，提权请求被用户拒绝。本次追加修改尚未通过构建和设备 UI 验证；上一轮构建成功仅覆盖审批清单修复。

后续验证：日志光标跟随修复期间获准再次执行 `:app:assembleDebug`，构建成功，覆盖当前“我的”和审批清单修改。单元测试和设备 UI 验证仍待完成。
