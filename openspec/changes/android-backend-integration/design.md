# Design

## 技术选型

- HTTP/JSON：OkHttp 4 + kotlinx.serialization + Kotlin Coroutines（Compose 已随依赖携带协程）。不引入 Retrofit/Hilt，保持轻量渐进。
- API 基址：`build.gradle.kts` 中 `buildConfigField` 注入；debug 默认云端 `https://www.rainycode.cn/api/v1`，本地联调可切 `http://10.0.2.2:8080/api/v1`（仅 debug 生效 cleartext 例外）。发布仅 HTTPS。
- 架构：`ApiClient`（统一契约/Bearer/401）→ `XxxRepository` → `ViewModel`/`produceState` 驱动 Compose；登录前不进入保护页。

## 关键决策

1. 会话：accessToken 存应用私有 SharedPreferences（`SessionStore`），不落日志、不进版本库；无 refresh token，401 一律清会话回登录。
2. 错误契约：成功 `{data,requestId}`，分页 `{items,nextCursor}`；错误读取 `error.code/message/fieldErrors`；保存 requestId 用于排查。页面三态（加载/失败重试/空数据）为统一模式。
3. 幂等：非幂等写操作携带 `Idempotency-Key`；同一动作/命令保留原键（重试复用），新动作新键；409 IDEMPOTENCY_CONFLICT 展示错误不重发。
4. 版本：编辑类请求必带 expectedVersion；409 VERSION_CONFLICT 重新读取由用户处理。个人完成另用 expectedAssignmentVersion，审批用 review.version。
5. 模型重构：`WorkTask` 拆为 kind/reviewStatus/authorizedAssignments（assignmentId/recipientUserId/executionStatus/progressPercent/completionSource/version/allowedActions）；`DemoRole` 替换为 `/me.capabilities`；标签用字符串 ID 与 colorHex。MockData 仅作无网络演示基线，不再作为业务数据源。
6. 年视图：后端 calendar 无 year mode；暂按"年视图点击月份→以该月 anchorDate 查 month 区间"的方式保留入口，正式方案待评审（见 API V0.2 第 14 行）。
7. 时区与日期：业务日以服务端 businessTimezone 返回为准；请求日期使用完整 `YYYY-MM-DD`，历史筛选不丢年份。

## 分阶段交付

按 proposal 的五阶段顺序：网络与登录 → 页面读取 → 日报与重要事项 → 任务闭环 → 联调验收。每阶段独立可运行、独立提交；后端未开放的缺口（inbox、驳回重提、获批编辑、无上级自审、跨日 TIMED、服务端草稿）不接猜测 API，先与后端确认。

## 测试

- 单元：日期/契约解析、Idempotency-Key 复用规则。
- 联调：真实账号真机全流程（登录→建任务→上级批准→进度→完成/验收→日报→重要事项）；401/403/404/409/422 每类至少一次人工验证。
