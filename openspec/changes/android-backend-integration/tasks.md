## 1. 阶段一：网络与登录

- [x] 1.1 Manifest 声明 INTERNET；配置 debug-only 网络安全例外
- [x] 1.2 gradle 增加 OkHttp/kotlinx-serialization/coroutines 依赖与 serialization 插件；BuildConfig 注入 API 基址
- [x] 1.3 实现 ApiClient：统一 `{data,requestId}`/错误契约解析、Bearer 注入、401 全局清会话回登录
- [x] 1.4 实现 SessionStore：token 私有存储与清理；切换账号数据隔离
- [x] 1.5 登录页（企业域/登录名/密码）→ `POST /auth/login`；错误密码/网络失败分别提示
- [x] 1.6 "我的"接 `GET /me` 真实资料与 capabilities；改密 `POST /me/password`（≥10 字符）；退出 `POST /auth/logout`；401 保护页不可返回进入
- [ ] 1.7 验证：合法登录、错误密码、过期 401、退出后隔离

## 2. 阶段二：页面数据读取

- [x] 2.1 `GET /dashboard` 四面板 + moreUrl/nextCursor 分页；三态（加载/失败重试/空）
- [ ] 2.2 单条详情按稳定 ID：company-important-items / tasks / work-log-entries / work-logs
- [x] 2.3 `GET /tags`（id/colorHex/primaryTagId/displayColor）替换本地标签目录
- [x] 2.4 `GET /calendar` 日/周/月（服务端时间口径、AND 标签、分页、同 taskId 跨格）
- [x] 2.5 `GET /tasks?scope=...` 任务列表与 `GET /statistics` 统计
- [x] 2.6 模型重构：WorkTask→kind/reviewStatus/authorizedAssignments；DemoRole→capabilities
- [ ] 2.7 验证：多页读取、空数据、失败重试、换账号隔离

## 3. 阶段三：日报与重要事项

- [x] 3.1 日报一篇多条：POST/PATCH，clientEntryKey/entryId/upsertEntries/expectedVersion
- [x] 3.2 同日 409 existingJournalId 补充；编辑 409 冲突重读
- [x] 3.3 重要事项 entryId 勾选（候选 D～D-9）、最多十条、空集合保存、requiresSelection 语义
- [ ] 3.4 验证：每天唯一、稳定 ID、版本冲突、空集重启保持、换设备同数据

## 4. 阶段四：任务闭环

- [x] 4.1 `POST /tasks` PERSONAL/DELEGATED + recipient-candidates 真实候选；PENDING 展示
- [x] 4.2 审批队列/快照详情/decision（APPROVE|REJECT + review.version）
- [x] 4.3 进度记录（assignment.expectedVersion，100%≠完成）
- [x] 4.4 个人自完成（expectedAssignmentVersion）与派发逐人验收（ACCEPT|REJECT + request 版本）
- [x] 4.5 allowedActions 驱动按钮；403 不绕过
- [ ] 4.6 验证：真实上级批准、跨级范围、非法混入拒绝、重复提交不追加完成事实

## 5. 阶段五：联调验收

- [x] 5.5 合并前审查修正：空 POST、旧会话在途响应、草稿/头像偏好账号隔离和界面缓存清理

- [ ] 5.1 Idempotency-Key 复用/新键策略与 409 家族统一处理
- [ ] 5.2 账号隔离与本地偏好按账号存储
- [ ] 5.3 真机真实账号全流程走通（含 401/403/404/409/422 各验证一次）
- [ ] 5.4 openspec validate --strict 与模块构建通过
