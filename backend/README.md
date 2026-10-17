# Pandora 后端实施记录

2026-10-17 更新：Web #12 / Android #13 已审核合入 dev 并部署，backend-syz 已同步 a493dfe 后补齐组织转换、人员资料持久化、管理员权限与公司日期日报规则。新提交待非作者审查后进入 dev；字段、日期规则、test 等级 1 与上线边界详见 [联调交接](CLIENT_HANDOFF_20261017.md)。系统操作日志存储方案仍待甲方确认。

2026-10-15 更新：`backend-syz` 已合入 `dev ec288d4`。用户确认开放直属上级审批，新增审核列表、详情和决定，打通真实任务闭环；Android 所需十模块契约与对接示例见 [ANDROID_API_V0.2.md](ANDROID_API_V0.2.md)。变更 [complete-android-backend-workflows](../openspec/changes/archive/2026-10-15-complete-android-backend-workflows/proposal.md) 已随云端部署批次同步 dev 并归档，部署入口见 [云端部署](../deploy/README.md)。下面 2026-10-13 段落记录原始实施基线。

2026-10-13；在 `backend-syz` 从 `dev a8adf5d` 开始开发，经 [PR #10](https://github.com/YuzhSong/Pandora6ix/pull/10) 合并到 `dev f5283e6`，随后同步回 `backend-syz`。只涉及 backend 与 OpenSpec。依据 docs 中需求 V1.3、API0.1、DB0.2 和后端缺口清单，变更 [`implement-backend-confirmed-apis`](../openspec/changes/archive/2026-10-13-implement-backend-confirmed-apis/proposal.md) 已在合并验证后归档并同步主规格。

安卓逐模块接入、模型差异与阻塞项见 [ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md)。

## 已实现的缺口

| 缺口 | 本分支实现 |
|---|---|
| 登录、本人、改密、退出 | 企业登录域 + loginName；BCrypt 密码；12 小时可撤销 Bearer 会话；角色/状态每次读取；改密撤销当前会话；退出幂等 |
| 企业初始化与维护 | 受控初始化，同时创建公司根节点与初始 ADMIN；初始化支持幂等；所属企业查询、名称和时区维护 |
| 组织、人员、直属上级、角色 | 公司/部门/团队树；直属上级独立保存；多角色；创建/版本更新；循环、跨企业引用、无效角色拒绝；不允许失去最后一个有效管理员 |
| 持久化与隔离 | JDBC + Flyway；本地文件 H2；企业复合外键、作者保护键；服务端对象授权；事务、行锁、版本冲突 |
| 共用标签 | 企业目录查询；ADMIN/创始人/部门负责人创建；ADMIN 维护；同名允许；主标签必须在集合内；任务 AND 筛选 |
| 公司重要事项 | 本企业列表、全文；ADMIN/创始人创建与版本维护；发布者来自会话；部门受限发布范围未定时返回明确错误 |
| 任务 | 个人/派发申请、管理范围内直属与跨级候选、多人原子校验、待审批快照、列表/全文、待审批内容编辑、进度历史、个人自完成、逐人完成申请与创建者验收 |
| 日/周/月视图 | 仅已批准的本人任务；周一开始、月首尾完整周；任意历史窗口；标签 AND；完整数据库分页和过滤后数量 |
| 日报与条目 | 每人每天唯一；一篇多条；稳定 entryId；版本更新/增补不删除遗漏条目；作者写、管理范围读；单条全文接口 |
| 个人重要事项 | 本人 D～D-9 已发布条目；至多十条；≤10 默认全选、>10 要求手选；手动空集持久化；旧已选过期条目不擅自删除/补位 |
| 导图与基础统计 | 四面板与详情 ID；列表更多入口；本人已批准执行关系的总数、完成数、未完成数；无数据为零 |
| 直属上级审批 | 本人待审队列/提交历史、不可变快照、APPROVE/REJECT、版本/幂等/审计；审批时重查直属上级及接收人，原子激活多人执行关系，旧快照不能决定 |
| 正式接口契约与验证 | [`openapi.json`](openapi.json)：44 个业务操作及命令/读模型 schema；JSON 成功/错误信封、fieldErrors、分页、版本和可选幂等；集成测试及真实进程重启检查 |

Web 管理端已在 PR #11 接入；Android 原型仍需要接入真实网络。本次没有修改客户端，也不声称完成 Android 应用联调。

## 独立运行

需要 JDK 21。Maven Wrapper 在本模块内，不依赖 Android、Node 或外部数据库服务。

```powershell
cd D:\Code\Pandora6ix\backend
$env:JAVA_HOME = 'D:\Downloads\JDK'  # 换成自己的 JDK 21
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

默认本地数据库为 `backend/data/pandora.mv.db`，`data/` 不提交；数据库与工作目录绑定，建议始终从 backend 目录启动。每次启动 Flyway 校验/执行迁移。生产数据库、账号和时区尚无团队固定配置，用户确认先采用此本地持久化方案。

| 配置 | 行为 |
|---|---|
| `PANDORA_DB_URL` | 默认 `jdbc:h2:file:./data/pandora;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0`；可指定独立 H2 文件/连接 |
| `PANDORA_DB_USER` / `PANDORA_DB_PASSWORD` | 数据源凭据，从环境注入；默认仅用于本地 H2 |
| `PANDORA_BOOTSTRAP_TOKEN` | 首次创建企业的高熵秘密；默认空，未配置时公共初始化关闭；不写进代码、日志或 Git |
| `enterprise.businessTimezone` | 初始化时可配置合法 IANA 名称，例如 `Asia/Shanghai` 或 `UTC`；未配置的业务日接口返回 `BUSINESS_TIMEZONE_NOT_CONFIGURED` |

DB0.2 的 MySQL 8.4 是候选。当前驱动、DDL、迁移和验证针对 H2，尚未交付 MySQL 方言/部署验证；更换数据库需要相应驱动和迁移验证，不能只替换 URL 就宣称生产可用。部署时使用 HTTPS/同源反向代理；本轮没有创建数据库服务器或其他基础设施。

### 受控初始化与凭据

初次部署由操作者将自己的随机秘密设置为 `PANDORA_BOOTSTRAP_TOKEN`。调用 `POST /api/v1/admin/enterprise-initializations` 时通过 `X-Bootstrap-Token` 提供该秘密，并传入 `loginDomain/name/businessTimezone/adminLoginName/adminPassword/adminDisplayName`。此操作原子创建企业、公司节点与 ADMIN。已登录 ADMIN 也可初始化新企业；查询/修改企业、组织及用户只能操作其当前所属企业。

生产不自动创建测试账号，不内置管理员密码。API 凭据由初始化和人员录入者指定：密码至少 10 个字符、最多 72 个 UTF-8 字节（BCrypt 的技术输入边界）。账号创建只保存哈希；响应不返回密码或哈希。

```json
{"enterpriseDomain":"your-company","loginName":"your-account","password":"your-own-password"}
```

该 JSON 用于 `POST /api/v1/auth/login`。成功返回 `data.accessToken/tokenType/expiresAt/user`，后续请求添加 `Authorization: Bearer <accessToken>`。不使用 Cookie 身份；Web 接入时需主动发送 Authorization。退出、成功改密返回 204；改密仅撤销本次会话，不假设所有设备强制退出。停用账号撤销已有会话。

## 路由与契约

所有业务路由以 `/api/v1` 开头，详见 [`openapi.json`](openapi.json)。`GET /health` 保留裸 `{"status":"UP"}`；`/actuator/health` 另提供框架健康。

| 方法 | 路由 |
|---|---|
| POST / GET / POST / POST | `/auth/login`、`/me`、`/me/password`、`/auth/logout` |
| POST / GET+PATCH | `/admin/enterprise-initializations`、`/admin/enterprise` |
| GET+POST / PATCH | `/admin/org-units`、`/admin/org-units/{id}` |
| GET+POST / PATCH | `/admin/users`、`/admin/users/{id}` |
| GET+POST / PATCH | `/tags`、`/tags/{id}` |
| GET / GET+POST / GET+PATCH | `/tasks/recipient-candidates`、`/tasks`、`/tasks/{id}` |
| GET / GET / POST | `/task-reviews?scope=pending\|created`、`/task-reviews/{id}`、`/task-reviews/{id}/decision` |
| POST | `/task-assignments/{id}/progress-records`、`/tasks/{id}/complete`、`/task-assignments/{id}/completion-requests`、`/task-completion-requests/{id}/decision` |
| GET | `/calendar?mode=day\|week\|month&anchorDate=YYYY-MM-DD` |
| GET+POST / GET+PATCH / GET | `/work-logs`、`/work-logs/{id}`、`/work-log-entries/{id}` |
| GET / GET+PUT | `/me/important-entry-candidates`、`/me/important-entries` |
| GET+POST / GET+PATCH | `/company-important-items`、`/company-important-items/{id}` |
| GET | `/dashboard`、`/dashboard/task-items`、`/statistics` |

企业初始化、`/statistics` 和导图任务面板分页端点是为既有 FR-USER-04、FR-STAT-01/02、FR-DASH 提供的明确技术契约，已记入本次 OpenSpec；不是新增业务功能。

- 成功：`{"data":...,"requestId":"..."}`；业务错误：`{"error":{"code":"...","message":"...","fieldErrors":[]},"requestId":"..."}`。兼容保留 `timestamp/status/code/message/path` 平铺字段。JSON 格式/未知字段为 400；字段校验为 422；版本冲突为 409；缺失或不可见私人资源统一 404；纯角色限制为 403。
- ID 为 UUID 字符串；日期严格 ISO，日内时刻严格 HH:mm，事件为 UTC；版本从 0 开始。普通变更用 `expectedVersion`，个人完成明确用 `expectedAssignmentVersion`。选择状态不存在时客户端传版本 0，首次手选保存后为 1。
- 查询 `limit` 默认 20、允许 1～100，返回 `{items,nextCursor}`。将 nextCursor 传给同一路由、同过滤条件；继续至 null，不能把首屏当全部数据。列表采用数据库 seek 分页，以时间/名称/顺序及 ID 稳定排序。管理范围根据当前权限重算，游标锚点失去可见性或条件改变时要求重新加载。
- `from/to` 同时提供时单次最多 367 天，可分窗口读取任何历史日期。十日规则仅用于重要条目候选，任务/日历/日报历史查询不套用该限制。
- POST 任务、日报、公司事项、进度、完成申请/验收、个人完成及企业初始化可传 `Idempotency-Key`（1～128 字符）。同身份/操作/键/命令重放原结果；同键不同命令 409，不发生第二次完成/发表。重放记录持久保存，本轮不擅自设定会导致重试重复写入的过期期限。避免将凭据、对象 ID 列表放进键值。
- 组织和人员 PATCH 使用完整维护命令；可选引用传 null 可以清空，roles 必须提供完整集合。组织 kind 变更需要专门迁移，目前明确拒绝。企业/标签 PATCH 为可选字段更新；省略即保留。
- 日报 PATCH `upsertEntries` 省略/为空不会删旧条目；有 entryId 是原条目编辑，无 entryId 必须有唯一 clientEntryKey 才新增。tagIds 省略为保留，空数组 + null 主标签为清空。修改单独 primaryTagId 必须携带完整 tagIds。
- `DAILY_JOURNAL_EXISTS` 在 `error.existingJournalId` 返回本人同日日报 ID；失败内容不会自动合入旧篇。条目单独详情只返回这一条的完整内容。
- ADMIN 可维护目录/公司事项，ADMIN 单独不是业务用户。FOUNDER 管企业下级，DEPARTMENT_MANAGER/TEAM_LEAD 对应所属部门/团队子树；同级、上级、无组织范围人员不能被派发。直属审核人独立于组织归属，不能形成环。
- 创建任务返回 201 只表示 PENDING 申请。未批准任务不会进入接收人的正式任务/日历，也不能进度/完成。待审编辑重查接收人范围及直属审核人，追加不可变快照；审批必须匹配当前任务版本。批准会增加任务和 assignment 版本；后续进度/完成必须使用返回的新关系版本。审核决定可传 Idempotency-Key。
- `/statistics` 基础统计明确使用 `OWN_APPROVED_ASSIGNMENTS`：本人已批准执行关系总数，以真实 COMPLETED 事实计完成；不把进度 100 当完成，不推导多人任务整体完成/父子统计口径。

## 仍未实施 / 待确认

| 文档边界 | 状态 |
|---|---|
| `/inbox`、已读 | 仍 Reserved；用户本轮已确认将三个 `/task-reviews` 接口从旧文档预留提升为实现，不依赖 inbox 也可读取审核队列并完成审批 |
| Q-01 最高级/无上级审核 | 明确 `WORKFLOW_POLICY_UNCONFIRMED`，不免审/自审 |
| Q-02 获批编辑、驳回重提 | 未开放重提；获批/驳回内容编辑拒绝 |
| Q-03 草稿长期同步 | 没有草稿表/同步端点，已发布存储不混入草稿 |
| Q-04 跨日/同刻 TIMED 端点 | 仅开放有明确正时长的同日分钟任务；其他返回政策未确认；企业时区须显式配置 |
| Q-05 重要程度高低方向 | 保存/回传 1～5，不推断优先级方向 |
| Q-06 选中条目到期/编辑/移除联动 | 不删除/补位；读取当前引用内容与 eligibility，已有过期选择可以保持，新过期候选不能加入；移除接口未定义 |
| Q-07 父子关联、整体完成、团队统计分母 | 不开放父子关系；P1 完成率/分布/团队统计仍未实施 |
| Q-08 日志关联任务 | 不加必填 taskId，不提供未确认关联端点 |
| Q-09 标签停用/历史策略 | 不开放删除/停用；维护名称/颜色后读取当前企业目录，无历史快照策略宣称 |
| 部门负责人“受限”公司事项发布 | 缺少发布范围 DTO/规则，明确返回政策未确认；ADMIN/FOUNDER 的全企业维护已实现 |
| P1 提醒、邀约、P2 AI | 本轮未实施，未引入邮件、队列或 AI 服务 |

## 验证与维护

```powershell
cd D:\Code\Pandora6ix\backend
.\mvnw.cmd package
python scripts/generate-openapi.py --check
# 真实进程、独立随机测试库，不使用 backend/data 或预设团队账号：
.\scripts\smoke.ps1
python scripts/android-smoke.py
cd ..
openspec validate backend-identity-directory --strict
openspec validate backend-task-workflow --strict
openspec validate backend-journals-dashboard --strict
```

接口变化后运行 `python scripts/generate-openapi.py` 更新契约。脚本读取显式命令和控制器注解，读模型 schema 由脚本集中维护。

业务集成测试在独立内存库创建临时企业/账号，覆盖跨企业、ADMIN 业务隔离、上下级范围/循环、会话生命周期、AND 标签、全部分页、版本失败回滚、幂等与并发发表、两类完成事实、唯一日报、稳定条目、十日窗口/手动空集/过期保留、日历和导图。新增测试通过真实审批接口打通个人/多人派发流程、审核快照与过期版本、权限变化和并发决定；原有执行单元场景仍保留隔离状态夹具，不是生产绕过接口。

真实进程 smoke 检查独立文件数据库：迁移、初始化/登录、发表幂等、重启后 ID/内容/手动空集/会话保存、导图读取与退出撤销；结束时停止自己启动的进程。运行结果与测试数量见 [`VALIDATION.md`](VALIDATION.md)。这不替代生产 MySQL 验证、固定规模的性能验收或真实客户端联调。

`android-smoke.py` 进一步覆盖十模块真实 HTTP：V1 数据库写入后升级 V2、直属上级批准/驳回、100% 进度仍未完成、个人自完和派发逐人拒绝/重验收、日报补充/冲突、十条选择/空集、标签 AND/角色、统计和再次重启持久化；不通过 SQL 准备批准状态。
