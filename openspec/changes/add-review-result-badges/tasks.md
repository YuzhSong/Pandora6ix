## 1. Implementation

- [x] 1.1 实现按账号持久化结果已读记录及分页未读计算
- [x] 1.2 合并入口角标，加入前台刷新和列表逐条已读交互

## 2. Validation

- [x] 2.1 添加通过、拒绝、已读、分页及计数上限回归测试
- [ ] 2.2 运行 Android 单元测试、构建与 OpenSpec 严格验证

验证记录：`openspec validate add-review-result-badges --strict`、`git diff --check` 已通过。Android `:app:testDebugUnitTest :app:assembleDebug` 因用户 Gradle 缓存锁文件无写权限无法启动；提权执行请求被拒绝，测试与构建尚未验证。需获得缓存访问权限后完成本项，再进入 PR 评审。
