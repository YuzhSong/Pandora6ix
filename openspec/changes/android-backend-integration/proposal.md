## Why

后端十模块接口已在云端（https://www.rainycode.cn/api/v1）完成部署并可通过真实流程验收，而 Android 端仍使用 MockData 与内存列表，没有登录、网络与真实数据。需要将 Android 全部业务数据与操作替换为真实后端接口，形成可联调验收的移动端。

## What Changes

- 新增网络基础设施：INTERNET 权限、OkHttp + kotlinx.serialization + Coroutines 依赖、BuildConfig API 基址、统一响应/错误契约解析、Bearer 会话注入与 401 全局处理。
- 新增登录与会话：企业域/登录名/密码登录、`GET /me` 真实资料、改密（≥10 字符）、退出；会话失效回登录页并禁止进入保护页。
- 页面数据读取替换 MockData：导图四面板（含分页 moreUrl/nextCursor）、稳定 ID 单条详情、企业标签、日/周/月视图（服务端时间口径）、任务列表、本人统计；补加载/失败重试/空数据三态。
- 日报改为"一篇多条工作条目"（clientEntryKey/entryId/upsertEntries/expectedVersion），个人重要事项按 entryId 勾选（最多十条、支持空集合保存）。
- 任务闭环：个人/派发创建（真实接收人 userId 候选）、直属上级审批（task-reviews 队列/快照/决定）、进度更新（100% 不等于完成）、个人自完成、派发逐人验收；操作项一律由 allowedActions 驱动。
- 联调验收：Idempotency-Key 管理、409 版本冲突族处理、账号隔离、真机完整流程。

## Capabilities

### New Capabilities
- `android-network-session`: 网络层、统一契约、Bearer 会话与 401 处理。
- `android-auth-screens`: 登录、我的资料、改密、退出交互。
- `android-api-data`: 导图、详情、标签、日历、任务列表、统计的接口化读取。
- `android-journal-entries`: 日报一篇多条与个人重要事项 entryId 选择。
- `android-task-workflows`: 任务创建、审批、进度、完成与验收闭环。

### Modified Capabilities

## Impact

- 仅 Android 模块（android/）：新增网络/会话/仓库层，替换 MockData 消费面；MockData 保留为无网络的演示基线，不再作为业务数据源。
- 不修改 backend/、web/ 与数据库；不引入 /admin/* 调用；不提交任何凭据。
- 年视图不在后端 mode 枚举内，保留与否待评审（先按既有日期区间查询方案验证）。
- 接口细节以 backend/ANDROID_API_V0.2.md 与 openapi.json 为准。
