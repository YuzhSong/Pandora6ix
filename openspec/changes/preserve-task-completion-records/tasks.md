## 1. Fix

- [x] 1.1 日历入口改用独立完整详情状态，避免列表覆盖执行历史。
- [x] 1.2 完成任务时原子保存阶段文字，保留历史、授权与幂等规则。

## 2. Verify

- [x] 2.1 增加个人、派发与重复任务完成记录及失败重试测试。
- [x] 2.2 执行相关后端回归、Android 编译与 OpenSpec 校验并记录结果。

验证：BusinessApiIntegrationTests 全部 120 项通过；Android :app:compileDebugKotlin --offline 通过；openspec validate preserve-task-completion-records --strict 通过；git diff --check 通过。尚未执行手机实机验收或部署。
