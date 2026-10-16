## ADDED Requirements


### Requirement: 网络层统一契约
ApiClient SHALL 以 `BuildConfig` 配置的 API 基址发起 HTTPS 请求，统一解析成功体 `{data, requestId}` 与错误体 `error.code/message/fieldErrors`，不记录密码或 token。

#### Scenario: 正常响应
- **WHEN** 任一业务接口返回 2xx
- **THEN** 客户端解出 data 并保留 requestId 供排查

#### Scenario: 业务错误
- **WHEN** 接口返回 4xx/5xx
- **THEN** 页面展示按错误码/HTTP 状态映射的中文用户提示（如登录失败显示"企业域、登录名或密码不正确"），不向用户暴露英文原文与状态码；提供重试入口，不伪造成功

### Requirement: Bearer 会话注入与 401 处理
所有已登录请求 SHALL 自动附带 `Authorization: Bearer <accessToken>`；收到 401 时 SHALL 清理会话、回到登录页，且不得自动重试或回退 MockData。

#### Scenario: 会话过期
- **WHEN** 会话超过 12 小时或被撤销后任一请求返回 401
- **THEN** 应用清除本地会话并展示登录页，保护页不可通过返回键进入

### Requirement: 账号隔离
切换账号后，本地会话、界面数据与按账号保存的偏好 SHALL 完全隔离，不得读取上一账号的个人重要事项、草稿或缓存。

#### Scenario: 换号登录
- **WHEN** 账号 A 退出后账号 B 登录
- **THEN** B 看不到 A 的任何个人数据
