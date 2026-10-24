# Android 与后端对接清单

2026-10-17：#13 已合并到 dev 并部署；backend-syz 后续修正了日报公司日期规则与部门分组元数据，详见 [最新交接](CLIENT_HANDOFF_20261017.md)。本次含必要 Android 契约适配，需构建更新客户端；真机全流程尚未验收。

**2026-10-15 更新：** `backend-syz` 已同步 `dev ec288d4`，安卓成员两分支的当前改动均已包含于 dev。用户本轮明确确认实施直属上级审批；最新十模块端点、DTO、版本与审批流程以 [ANDROID_API_V0.2.md](ANDROID_API_V0.2.md) 和 [OpenAPI](openapi.json) 为准，覆盖旧 API0.1 的审批 Reserved 说明。本轮不修改 Android 源码。

2026-10-13。PR #10 已合并到 `dev f5283e6`，`backend-syz` 已同步该提交。本清单依据安卓现有源码、docs 需求 V1.3/API0.1 与已实现 [OpenAPI](openapi.json) 核对；只记录对接工作，不修改安卓代码或新增业务接口。

## 当前状态与前置工作

后端核心接口已经提供，但安卓尚无真实网络接入：

- [MainActivity.kt](../android/app/src/main/java/com/pandora6ix/app/MainActivity.kt#L84) 使用 MockData 和 `remember` 内存列表；启动直接进入导图，没有真实登录流程。
- [build.gradle.kts](../android/app/build.gradle.kts) 目前只有 Android/Compose 等依赖，未配置业务 HTTP/JSON 客户端；[AndroidManifest.xml](../android/app/src/main/AndroidManifest.xml) 未声明 INTERNET 权限。
- [MockData.kt](../android/app/src/main/java/com/pandora6ix/app/mock/MockData.kt) 的任务、日志、标签模型与正式 DTO 不一致，不能直接把模拟数据当成服务端数据。

安卓前置工作：增加网络权限、按构建环境配置 API 地址、HTTP/JSON 客户端、Bearer 会话管理、统一错误处理、Repository/ViewModel 加载状态及异步请求。跨账号缓存与显示偏好按用户隔离；网络失败显示错误/重试，不能自动换成 MockData 并显示保存成功。

普通安卓业务账号不调用 `/admin/*`。企业/管理员初始化及组织、直属上级、业务角色由管理端录入；不要把 bootstrap secret 放进 APK。后端联调前需准备合法业务时区、创始人/部门负责人/团队长/员工及跨部门/跨企业测试账号，账号凭据通过受控渠道提供，不提交 Git。

本机模拟器访问电脑后端可使用 `http://10.0.2.2:8080/api/v1`，其中 `10.0.2.2` 是开发机回环地址别名；健康检查为 `http://10.0.2.2:8080/health`。真机需要其可达的后端地址，不能把手机 localhost 当作电脑。参见 [Android 模拟器网络文档](https://developer.android.com/studio/run/emulator-networking-address)。本地 HTTP 调试须配置仅开发环境的网络安全例外；发布环境使用 HTTPS。参见 [Android 网络安全配置](https://developer.android.com/privacy-and-security/security-config)。

## 逐模块接入

下表路由均带 `/api/v1`。标记“已实现”只说明后端端点和服务已实现，不说明安卓已调用。

| 模块 | 后端接口 | 安卓需要完成 | 后端状态/边界 |
|---|---|---|---|
| 登录与会话 | `POST /auth/login`、`GET /me`、`POST /auth/logout` | 新增企业登录域、登录名、密码登录；保存 accessToken；自动附 Bearer；401 后回登录；退出后禁止进入保护页 | 已实现。会话 12 小时；无刷新令牌端点，过期重新登录 |
| 我的与改密 | `GET /me`、`POST /me/password` | 用真实姓名/部门/职位/角色/组织/时区替换模拟资料；提交 currentPassword/newPassword；成功 204 后清理当前会话并重新登录 | 已实现。新密码至少 10 字符、最多 72 个 UTF-8 字节；当前安卓“至少 6 位”和“已修改（演示）”须调整 |
| 导图四面板 | `GET /dashboard`；更多读取 `GET /company-important-items`、`GET /dashboard/task-items`、`GET /me/important-entry-candidates` | 四面板读取真实摘要；保留 itemId/taskId/entryId；用每个面板 moreUrl 与 nextCursor 读取更多；空数据、加载失败分开显示 | 已实现。公司派发任务面板仅本人已批准 DELEGATED；日报面板按工作条目展示 |
| 单条全文 | `GET /company-important-items/{id}`、`GET /tasks/{id}`、`GET /work-log-entries/{id}`、`GET /work-logs/{id}` | 点击面板用稳定 ID 读取对应单条全文；日报整篇和工作条目详情分别处理；不再只传列表下标或拼接文本 | 已实现。单条工作内容不是整篇日报；不可见私人资源与不存在统一 404 |
| 任务创建和 @ 候选 | `GET /tasks/recipient-candidates?q=&cursor=&limit=`、`POST /tasks` | PERSONAL/DELEGATED 分支；个人任务锁本人；派发用候选 userId 而非姓名；候选搜索分页；提交 kind、时间、重要程度、recipientIds、标签 IDs；接收服务端 PENDING 结果 | 已实现。员工可建本人个人任务；管理者派下级；无有效直属审核人返回政策未确认 |
| 任务列表/编辑 | `GET /tasks?scope=mine\|managed\|created&from=&to=&tagIds=`、`GET /tasks/{id}`、`PATCH /tasks/{id}` | 本人任务、管理范围、本人提交申请分别查询；根据 allowedActions/reviewStatus 和执行关系显示按钮；保存期望版本，409 重新读取 | 已实现。待审批创建者可编辑；获批/驳回编辑、重提仍未开放 |
| 直属上级审批 | `GET /task-reviews?scope=pending\|created`、`GET /task-reviews/{id}`、`POST /task-reviews/{id}/decision` | 用 reviewId 打开不可变快照；审核人传 APPROVE/REJECT 和 review.expectedVersion；创建者读结果；批准后刷新任务/执行关系版本 | 本轮已实现。指定当前直属上级可决定；旧快照/变更的范围拒绝，支持幂等和并发保护。消息 inbox 仍预留 |
| 人员头像 | `PUT /users/{id}/avatar`、`GET /users/{id}/avatar` | 上传压缩后的图片 data URL（JPEG/PNG/WEBP，≤768_000 字符）；按 userId 读取真实头像字节，未设置返回 404（客户端回退姓名首字）。审批详情用于展示请求人/审批人/执行人头像 | 已实现。上传限本人或 ADMIN；读取限同企业；二进制响应不套 `{data, requestId}` 信封 | 补进度输入/历史和个人完成交互；进度带 assignment.expectedVersion，个人完成带 expectedAssignmentVersion；提交成功后刷新关系版本 | 已实现，仅批准 ACTIVE 关系。progressPercent=100 不自动完成；PERSONAL 完成来源 SELF |
| 派发任务逐人验收 | `POST /task-assignments/{id}/completion-requests`、`POST /task-completion-requests/{id}/decision` | 接收人申请完成；创建者按 requestId 逐人 ACCEPT/REJECT，携带请求版本；分别显示每位接收人结果 | 已实现，完成来源 CREATOR_ACCEPTANCE。普通接收人不能代验收，不能使用个人自完成代替 |
| 日/周/月视图 | `GET /calendar?mode=day\|week\|month&anchorDate=&tagIds=&cursor=&limit=` | 将 ISO 日期/HH:mm 转为展示模型；使用服务端 rangeStart/rangeEnd/today/businessTimezone；AND 标签、全部分页、跨格同 taskId；不往日历混入日志 | 已实现，仅批准本人任务。安卓 YEAR 不是正式 mode；若保留年视图，需评审全年任务区间读取方案 |
| 企业标签 | `GET /tags`；有权业务角色可 `POST /tags` | 使用 tagId/name/colorHex/version；独立 primaryTagId；任务颜色用 displayColor；接收人和标签选择不靠名字判权限 | 已实现。创始人/部门负责人创建，其他业务角色选用；ADMIN 维护路径属于 Web，删除/停用仍未开放 |
| 日报发布和编辑 | `GET /work-logs?date=&from=&to=&scope=`、`GET /work-logs/{id}`、`POST /work-logs`、`PATCH /work-logs/{id}` | 一篇多条 entries 编辑器；新条目 clientEntryKey；成功保存 journalId、entryIds 和 version；同日 409 读 existingJournalId 再补充；上级只读下属，作者可写 | 已实现，缺省 scope=mine；managed 需管理权。现有 saveDailyLog 的本地单 content 覆盖方式须替换 |
| 个人重要事项 | `GET /me/important-entry-candidates`、`GET /me/important-entries`、`PUT /me/important-entries` | 从完整工作条目候选勾选 entryIds；保存 expectedVersion；支持最多十条、明确空集、requiresSelection 和 eligibility；保存后刷新导图 | 已实现。引用稳定 entryId；不是 sourceLogId 指整篇日报，也不是独立复制/编辑一份内容 |
| 本人基础统计 | `GET /statistics`，或 `/dashboard.statistics` | 展示 total/completed/uncompleted 和空状态；按返回 basis 理解口径，不能用进度或截止日期自行算完成 | 已实现，口径 OWN_APPROVED_ASSIGNMENTS。团队完成率/分布等 P1 接口尚未提供 |
| 创始人公司事项维护 | `POST /company-important-items`、`PATCH /company-important-items/{id}` | 若按已确认需求增加创始人维护交互，携带标题、全文、排序、版本；普通员工仅查看 | 后端已实现 ADMIN/FOUNDER 维护；当前安卓只有查看原型。部门“受限”发布范围尚未定义 |

## 需要先纠正的模型/交互差异

| 当前安卓实现 | 对接正式契约时的处理 |
|---|---|
| `WorkTask.status` 中文字符串、`assignee` 姓名拼接 | 拆为 kind、reviewStatus、authorizedAssignments；保留 assignmentId、recipientUserId、executionStatus、progressPercent、completionSource、版本。多人不是一个完成状态 |
| `DemoRole.canCreateTasks` 仅 MANAGER=true、EMPLOYEE=false | 依据 `/me` 身份/能力区分“可创建本人个人任务”和“可派发”；不能继续一刀切禁用员工个人任务 |
| `TaskEntrySheet` 必须选接收人，保存 UUID 本地“草稿（演示）” | PERSONAL 锁本人，DELEGATED 必选有权 userIds；真正提交后使用服务端 taskId/reviewId，显示申请待审，不当作正式派发 |
| `TaskTag` 只有名称/ARGB；tags[0] 当主标签 | 标签目录使用 ID/#RRGGBB；primaryTagId 独立；排序不等于主标签。同名允许，不能用名字作键 |
| `WorkLog` 单 content；同日直接覆盖内容 | Journal 与 WorkEntry 分离，已发布条目稳定 ID；PATCH upsertEntries 不删除遗漏项；正文/标签/头版本同事务 |
| `PersonalImportantItem.sourceLogId` 引用整篇日志，可写内容副本 | 改为 entryId 引用；选择只修改集合。正文修改需作者按日报 PATCH 原条目，未决联动不能假设另有重要事项正文 API |
| 日期使用设备 Calendar、短月日标签检索历史 | API 使用完整 YYYY-MM-DD，按企业 businessTimezone 展示业务日；候选范围/今日以服务端结果为准，历史筛选不能丢年份 |
| 导图直接 take(10)、详情依赖下标或“日志§内容”字符串 | 摘要可以分页展示，更多需读全；公司事项/任务不擅自以十条为总量；详情保留对应类型的 ID |
| “管理团队”固定 teamLeaders 列表 | `/me` 只提供已定义本人/组织字段，不提供这份团队长名单。新版“我的”只读本人资料；若仍要保留名单，先明确有权展示范围，不调用 `/admin/users` 越权补齐 |
| 改密/退出只关弹窗、展示演示 Toast | 发真实命令；验证失败不显示成功；204/401/会话失效按契约处理；切换账户不串读个人重要事项和草稿 |

相关证据：[任务录入](../android/app/src/main/java/com/pandora6ix/app/ui/calendar/TaskEntrySheet.kt#L167)、[个人事项原型](../android/app/src/main/java/com/pandora6ix/app/mock/PersonalImportantItems.kt)、[日志原型保存](../android/app/src/main/java/com/pandora6ix/app/mock/DailyLogs.kt)、[改密原型](../android/app/src/main/java/com/pandora6ix/app/MainActivity.kt#L837)。

## 公共契约必须对齐

1. 成功 `{data,requestId}`，分页 `{items,nextCursor}`；错误读取 `error.code/message/fieldErrors`，保存 requestId 用于排查，不记录密码或 token。
2. UUID/业务 ID 都是字符串，版本是整数；不能用姓名、数组下标、自然日期生成正式业务 ID。正常编辑 expectedVersion 必填，个人完成另用 expectedAssignmentVersion。
3. 非幂等操作重试不能换键重复提交。使用 Idempotency-Key 时，同一动作/同一命令保留原键；修改命令或发起新动作使用新键。同键不同命令 409。
4. 409 VERSION_CONFLICT 重新读取后让用户处理，不自动覆盖；DAILY_JOURNAL_EXISTS 使用 existingJournalId；401 重新认证；403 不重试绕过；404 兼容不存在/不可见；422 显示字段错误。
5. PERSONAL 与 DELEGATED、审批与执行、进度与完成分别处理；只按后端状态/allowedActions 显示可操作项，按钮隐藏不代替服务端授权。
6. tagIds 多选为 AND，空集合不筛；非空标签有明确主标签；日报不染色。企业时区未配置、未决流程返回明确配置/政策错误，不能假装完成。
7. 字体、主题、按账号本地偏好继续留客户端；草稿确认/退出保护可本地实现，不新增未定义的服务端草稿同步或偏好端点。

## 后端仍有的阻塞项

| 内容 | 目前对安卓联调的影响 |
|---|---|
| 消息仍 Reserved | `/inbox`、已读尚未实现；审核改用本轮提供的 `/task-reviews` 队列/详情/决定。原型消息仍是假数据，不代表 Android 已调用接口；后端真实任务闭环已可通过接口验收 |
| Q-01 无上级审核 | 创始人/无有效直接上级创建任务明确拒绝，不能免审/自审 |
| Q-02 获批修改/驳回重提 | 仅待审创建者编辑已实现；重提/获批编辑入口不接猜测的 API |
| Q-03 草稿长期同步 | 当前没有服务端草稿端点；不能把本地保存当已发布，也不能上传未确认草稿 |
| Q-04 同刻/跨日 TIMED | 已确认允许跨天：`startDate+startTime` → `endDate+endTime` 可跨日（如 22:30 → 次日 02:15）；同日仍须结束晚于开始（违反返回 422 `VALIDATION_FAILED`，客户端需本地先拦）。企业时区由管理端显式配置 |
| Q-05～09 | 不推测重要程度方向、自动补位/条目删除、父子关联与总体/团队统计分母、日报任务关联、标签停用/历史策略 |
| P1/P2 与生产验收 | 提醒、邀约、AI、团队率/分布未提供；MySQL 部署、真机/多设备业务联调、固定规模性能验收未执行 |

本轮用户已授权审批，并由 `complete-android-backend-workflows` 实施；消息仍需另行确认。新任务必须通过真实直属上级审核后生效；不能为演示把 PENDING 自动置为 APPROVED。`scripts/android-smoke.py` 通过真实接口完成审核与验收，没有 SQL 批准夹具。

## 推荐接入顺序与验收

1. 网络层、登录、`/me`、退出/改密及账号隔离；验证合法登录、错误密码、过期/撤销、角色能力。后端人员/组织录入由管理员完成。
2. 标签、公司事项、导图和本人重要事项读取；替换 MockData，验证多页读取、条目单独详情、空数据/失败状态。
3. 日报 entries 发布/编辑，再接重要事项选择；验证每天唯一、稳定 ID、版本冲突、最多十条和手动空集重启保持，换设备读取同一条数据。
4. 任务个人/派发创建、真实候选、任务列表与日周月视图；验证跨级范围、非法混入整笔拒绝、标签 AND、PENDING 不进入正式接收/日历。
5. 接入审核队列、快照详情和决定，再接进度、个人完成与逐人验收；验证 100% 未完成、个人/派发分支不能混用、重复提交不追加完成事实。
6. 通过新建→真实直属上级批准→执行→完成/验收接口验收完整流程；消息、AI/团队统计按后续范围排期。

2026-10-13 基线为 39 项测试。本轮新增审批与真实闭环验证结果见 [VALIDATION.md](VALIDATION.md)。后端 HTTP 验证不等于 Android 应用已经接入或通过真机验收。
