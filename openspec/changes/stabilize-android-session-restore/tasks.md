## 1. Fix

- [x] 1.1 实现区分网络失败、会话失效、取消和旧响应的恢复逻辑，接入启动重试页面与延迟回调保护。
- [x] 1.2 添加启动恢复和并发会话绑定回归测试。

## 2. Validation

- [x] 2.1 执行针对性测试、Android 完整构建、OpenSpec 校验及差异检查。
- [x] 2.2 保留真实账号数据覆盖安装 APK，在连接手机上检查冷启动、页面稳定性与异常日志，记录复现边界。

当前验证：
- `:app:assembleDebug :app:compileDebugUnitTestKotlin` 通过；SessionRestoreTest + PasswordRenewalTest 最终直接 JUnit 运行 16 项通过（包含本机 HTTP 并发、真实挂起超时、损坏响应测试）。会话 HTTP 读取增加 15 秒总时限。
- `git diff --check`、`openspec validate stabilize-android-session-restore --strict` 通过。
- 实机只读诊断：未捕获到应用致命异常；发现热更新覆盖层；抓到登录页面但会话仍存在。没有输出 token 或密码；不能仅凭这些证据断言热更新或 401 是自动跳变的唯一根因。
- 初次覆盖安装被手机拒绝；用户允许后已保留数据覆盖安装完整 APK。观察到原会话恢复首页，轮询后仍为首页且无登录表单，随后可进入年视图操作。复测中用户有手动重启、手机自动锁屏冻结进程，故不将所有进程重启或采样空白认定为应用自行跳转；未捕获应用致命异常。未做长期稳定性保证，未卸载或清除应用数据。
- 后续日志曾出现 authenticated 后再次 started，补充将认证状态/恢复副作用提升至主题外的应用根节点并跳过有效账号的重复恢复；测试启动命令也改用标准启动器标志，避免创建重复 Activity 干扰。最终一次冷启动日志为单次 started→authenticated，随后可连续进入首页、年/月视图。重复恢复的具体触发因素不能仅凭同 PID 日志唯一归因于主题。
