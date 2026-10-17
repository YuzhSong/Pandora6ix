# Web 后端接入检查与实施记录

## 2026-10-17 后端资料与组织类型联调

backend-syz 已实现 gender/idNumber/phoneNumber 持久化和部门↔团队转换，契约与上线边界见 [后端交接](../backend/CLIENT_HANDOFF_20261017.md)。真实 Vite + Spring + H2 联调已验证创建、重读、角色修改资料保持与组织双向转换，尚待新后端 PR 审核进入 dev。

针对已构建的后端，可设置 `PANDORA_INTEGRATION_JAR` 为 jar 绝对路径后运行 `npm run test:integration`；脚本只读 jar，在 web/.integration/runs 的独立随机文件数据库运行，不使用团队账号。未设置时仍使用原有 `.integration/backend-<revision>` 副本流程。

## 2026-10-16 同步云端数据库链路

登录失败体验修正：认证或网络失败后保留当前表单三项输入，不再无条件清空密码；服务端统一凭据错误使用中性提示，不擅自定位到密码字段。公司开通与日常登录分离，普通入口改为“公司还未开通？”说明，受控初始化表单放在“部署人员入口”；初始化成功只回登录并清除开通秘密，不自动混合登录结果。没有新增公开注册、公司名称检索或管理员授权接口。

用户补充确认人员上级选项按业务角色区分：创始人无更高业务上级，部门负责人仅选创始人，团队负责人可选部门负责人/创始人，普通员工可选上述三类；ADMIN 不视为业务职级，多角色取最高业务职级。Web 候选同时排除本人、停用人员及会形成循环的关系；组织归属与直属上级仍独立，不额外限制只能同部门。角色配置允许同步调整直属上级，角色和上级通过现有完整人员 PATCH 原子提交；若降级导致直属下属关系不符合层级，先提示调整下属而不静默改写。后端源码和权限规则没有修改，本轮是 Web 侧候选及提交约束。

角色徽标统一使用独立配色（管理员紫、创始人金、部门负责人蓝、团队负责人绿、员工灰），支持深色主题。按用户要求通过云端接口移除了补充成员姓名/职位中的“演示”字样，保留人员、角色和上下级关系；现有 API 不允许改登录名，因此已有账号标识保留，未绕过后端直接改数据库。新增层级、循环排除和角色降级关系测试。

本轮用户确认界面调整：正常退出后登录页不显示“已退出登录”；“企业登录域”改为“公司登录标识”，说明其与公司展示名称、个人登录名的区别。首次部署入口补充首位管理员及部署授权码用途说明；按国内甲方使用场景移除业务时区输入，初始化命令固定 `Asia/Shanghai`（北京时间），不更改后端契约。管理员角色允许多人，仍由服务端确保至少保留一位启用管理员。

用户纠正了“甲方专用站点”的理解：撤回固定公司登录方案，移除公司名称/登录域构建配置和固定展示，恢复由用户填写所属公司的登录标识、账号与密码，并保留受控初始化入口。公司名称登录尚缺后端契约支持，本轮不做前端固定名称映射、不改后端或云端登录域。

`Web-cy` 从 `936a931` 无冲突快进至 `origin/dev 5992eb2`，包含组长对 PR #11 的 OpenSpec 审阅归档、后端审批补齐及云端部署。没有额外修改 backend、Android 或 deploy 源码。

用户确认团队地址为 `https://www.rainycode.cn`，`.com` 是笔误。云端 `backend/health` 返回 UP，`deployment.json` 的 sha 与上述 dev 相同。按现有部署方案，由云端后端访问单实例持久化 H2 文件库；Web 不连接数据库端口，不需要数据库密码。

新增 `.env.cloud.example`，本机 `.env.local` 指向云端 HTTPS，业务仍走 `/api/v1`。代理新增 `BACKEND_HEALTH_PATH`：直连 Spring 默认 `/health`，云端网关配置 `/backend/health`，避免把云端健康请求误改写成站点 HTML。保留本机后端模式及证书校验，不上传此前本机测试资料。

提交前验证：49 项 Web 测试通过（含云端网关路径和三项角色层级测试）、生产构建通过、lint 无错误且仅有两条原有警告。实际启动本地 Vite 后，经 `http://127.0.0.1:5173/backend` 检查云端健康得到 UP，经本机 `/api/v1/me` 得到后端 JSON 401（UNAUTHENTICATED），说明健康与业务鉴权两条代理路径均已连通。浏览器验证错误公司标识提交后三项输入保留、正常退出无冗余提示、部门负责人候选仅显示创始人和未设置选项。

初始验收使用用户提供的云端账号从本地 Web 登录，读取原公司及管理员资料，刷新后仍正常。之后按用户授权通过现有 API 调整公司名称并补充人员、组织、标签与事项，当前共 30 位人员、3 个部门、8 个团队、8 个标签和 5 条公司事项，五种角色均有数据。姓名已由用户确认，补充数据的显示名称/职位按用户要求去掉“演示”；登录名及原有上下级关系保留。真实写入后已通过接口和浏览器重新读取确认。

管理员密码未写入文件、配置或 Git；补充账号的初始凭据、录入脚本、核对结果及浏览器截图仅保留在忽略目录 `.integration/`，不进入 PR。Web 仍仅允许 ADMIN 登录，其他业务角色供 App 使用。用户拟与后端协调的正式自助公司开通、请求移除 businessTimezone、服务端业务职级约束尚未交付；本轮继续沿用原接口与北京时间参数，不宣称这些后端能力已实现。

## 2026-10-14 业务接入方案

提交前界面验收补充：大卡片采用半透明材质（浅色约 32% 透明、深色适配），遵循减少透明效果偏好；标签改为完整彩色底块和自动深浅文字，选色提供 12 个常用色、自定义色相/鲜艳度/明暗滑杆，以及浏览器支持时的屏幕取色。登录、初始化、资料编辑和改密表单统一为字段下方中文校验提示，保留必填、格式和范围校验；公司事项新增顺序默认为 1，Web 仅接受正整数。用户选定的三色 Pandora 图标已用于导航、登录页及 favicon，原图保存在 `public/pandora-logo.png`，供 Android 后续复用；本次未修改 Android 图标。

本版提交格式沿用 `feat(web): …`，PR 使用仓库模板，目标为 `Web-cy` → `dev`。本机截图、运行日志、数据库及测试账号不进入提交。团队后端部署地址和管理员账号仍由组长提供；当前本机原样后端副本可继续供用户检查。

视觉复核补充：按用户要求，将当前本机测试企业及公司根组织改为 `6ix`，通过现有服务端接口更新；未硬编码替换其他企业资料。该企业实际有 1 个管理员、1 个根组织，标签与公司事项均为 0，空状态来自尚未录入的数据，不自动灌入演示记录。

登录页调整了标题字重、字号/行距、左右视觉重心与半透明卡片；全站底层采用黄白多层径向渐变，深色模式为炭灰加暖金光晕。统一选择组件替换筛选和编辑弹窗的系统原生下拉层，支持方向键、Home/End、Enter、Escape、字母定位及点击外部关闭。浏览器已验证键盘筛选、原生 dialog 顶层内的弹出菜单与深色配色；构建及 lint 检查见本轮执行记录。截图在忽略目录 `.integration/login-refined.jpg`、`.integration/select-refined.jpg`、`.integration/select-dark.jpg`。

基线更新为 dev `f5283e6`（后端 PR #10 已合入，Web PR #9 已合入）。下方 2026-10-13 缺口为历史审查；本次实际契约以 `backend/openapi.json`、`backend/README.md` 及实现为准。继续仅修改 Web，后端源码只读；方案与验收记录放在本文件，按本轮作用域不创建根目录 OpenSpec 文件。

需求：Web 使用可信管理员身份和共享服务端资料，接登录、受控首次企业初始化、公司信息、人员/组织/角色、标签、公司事项、本人改密与服务端退出。后端拒绝时保留表单并提示错误，409 不自动覆盖；401 清理当前身份和旧公司快照。显示设置保留本机按账号配置。

设计：Bearer 令牌仅存当前标签页 sessionStorage，密码/初始化秘密仅用于当前表单请求。所有业务列表读取至 nextCursor=null，显式映射 supervisorUserId 与可空组织，不透传本地 schema 字段；PATCH 保留状态及完整 roles，登录名和组织 kind 的编辑遵循实际后端能力。保存使用后端响应更新快照，避免“写成功、刷新失败”导致误报或重复创建。公司事项与企业初始化采用稳定幂等键。原本机目录不上传或混入服务端资料。

本地验证：没有发现团队服务/账号时，后端原样复制至忽略的 `web/.integration/` 目录构建运行，数据库与临时凭据同样隔离；不修改原 backend，验证完成停止自己启动的进程。

- [x] HTTP 认证、DTO 映射、分页及管理员 repository。
- [x] 登录/初始化、编辑适配、改密与退出界面。
- [x] 自动测试、生产构建、lint 与真实后端链路检查。
- [x] 浏览器检查及最终接入说明。

验证结果：45 项 Web 自动测试通过；生产构建通过；lint 无错误（2 条现有 effect 状态更新警告）。真实后端原样副本在随机隔离 H2 库运行，经过实际 Vite 代理和 Web auth/repository 验证初始化重放、管理员登录、非管理员拒绝、公司名称/组织/人员/角色、102 条标签全分页、事项创建幂等与修改、刷新持久化、409 冲突、改密失败保留身份/成功撤销、旧令牌 401 和退出撤销。不是模拟后端响应。

浏览器验证真实登录后的服务端公司资料、新增人员初始密码及业务管理者候选、标签创建成功提示、刷新后标签仍存在、改密表单、退出与刷新后回登录；390px 初始化表单没有横向溢出。验证截图保留在本机忽略目录：`.integration/browser-tags.jpg`、`.integration/browser-setup-mobile.jpg`，不提交临时数据库或测试凭据。

新增 `npm run dev:backend` 已实际构建并启动按后端提交隔离的原样副本，在 `127.0.0.1:8080` 健康检查得到 `UP`，数据库仅写入 `.integration/local`。启动步骤与团队服务代理配置见 README。验收结束停止自己启动的实例，原 backend/Android/根目录规格及团队数据库无修改。

本轮没有发现团队部署地址或预设管理员；验证使用临时企业与随机账号。连接团队服务时仍需组长提供服务地址和真实管理员凭据。没有执行 Android 客户端联调、生产部署或全系统 MVP 验收；没有新增账号启停用/删除产品入口、Reserved 审批入口或 Q-01～09 未确认规则。

---

2026-10-13。范围：仅修改 `web/`；后端只读检查，不构建、不启动、不改代码/配置/数据库。用户要求优先于根目录 AGENTS.md 的 OpenSpec 写入流程，本次将 Web 方案、验收和任务记录在此，未在根目录新建 OpenSpec change，也不声称完成 OpenSpec 严格校验。

## 检查基线

- GitHub `dev`：`06340db`，本机 `Web-cy` 已无冲突快进至此。保留 Web 开发分支。
- 刷新过远端分支：`backend-xjy` 为 `30edc22`（2026-10-10，架构基础）；`backend-syz` 为 `e7dc702`（2026-10-09，设计文档）。两个分支均没有已实现的业务接口。本文将“后端1”按架构代码分支 `backend-xjy` 检查，并同时核对另一后端分支，避免漏掉未合入 dev 的实现。
- dev 包含后端架构及集成修复，应以 dev 为联调基线，不能为接后端退回更早的 backend-xjy。
- 依据：[`接口设计 API0.1`](../docs/api/Pandora_接口设计_20261009_V0.1.md)、[`需求 V1.3`](../docs/requirements/Pandora_产品需求与设计文档_20261009确认修订版_V1.3.md)、[`Web 需求核对`](REQUIREMENTS.md)。API0.1 自身明确是草案，`/api/v1` 业务路由尚未实现。

## 后端已有什么

| 内容 | 源码证据 | 可用范围 |
|---|---|---|
| Spring Boot / Maven 工程 | [`pom.xml`](../backend/pom.xml) | Java 21，独立工程骨架 |
| 服务存活检查 | [`HealthController`](../backend/src/main/java/com/pandora6ix/backend/controller/HealthController.java) | `GET /health` 返回裸 JSON `{"status":"UP"}`；Actuator 配置另暴露 `/actuator/health` |
| 请求标识、统一错误 | [`RequestIdFilter`](../backend/src/main/java/com/pandora6ix/backend/api/error/RequestIdFilter.java)、[`GlobalExceptionHandler`](../backend/src/main/java/com/pandora6ix/backend/api/error/GlobalExceptionHandler.java) | 当前是平铺错误对象；dev 修复了 404、405、400、500 映射 |
| 任务状态码 | [`domain/task`](../backend/src/main/java/com/pandora6ix/backend/domain/task) | 枚举与测试；没有任务执行逻辑 |
| 业务分层预留 | [`application`](../backend/src/main/java/com/pandora6ix/backend/application)、`service/package-info.java`、`repository/package-info.java` | 六个 UseCases 是空接口；service/repository 没有实现 |
| 基础测试源码 | [`src/test`](../backend/src/test/java/com/pandora6ix/backend) | 健康、错误契约、状态枚举；本次未运行后端测试 |

`/__test/error-probe` 是测试类内控制器，不能算生产 API。`WebConfig` 目前只注册请求标识过滤器，没有登录拦截或 CORS 实现。

## Web 联调阻塞清单

以下业务路由均带 `/api/v1` 前缀。路由名只引用已有草案；标为“未定”的内容不能自行补成正式 API。

| 优先级 | Web 功能 | 后端缺失 | 必须确认的契约 / 验收 |
|---|---|---|---|
| P0 | 登录、本人、改密、退出 | `POST /auth/login`、`GET /me`、`POST /me/password`、`POST /auth/logout` 均未实现；无会话或安全依赖 | 企业登录域、Session/JWT、初始管理员凭据、过期与撤销、Cookie/CSRF 或令牌规则；确认后 Web 再接登录表单与 401 流程 |
| P0 | 公司初始化、所属公司维护 | 没有创建/查询/更新企业接口；可信初始化与管理员绑定没有实现 | 该部分路由/DTO 在 API0.1 尚未完整定义；需要初始化幂等、可信归属、只维护本企业规则 |
| P0 | 组织、人员、直属上级和角色 | `/admin/org-units`、`/admin/users` 查询/创建/修改均未实现；无 DTO/权限业务实现 | 组织类型与树、独立直属上级、角色枚举/能力、初始人员凭据、资料修改与改角色是否同一命令、服务端拒绝循环与跨企业关系 |
| P0 | 持久化与企业隔离 | pom 无 JDBC/JPA/MySQL 依赖；无 datasource、实体、迁移或业务 repository | 存储方案、数据库迁移与初始化；服务端从会话判定企业和权限，不能信任前端传 role/enterpriseId |
| P1 | 共用标签 | `/tags` 的 GET/POST/PATCH 未实现 | 已有草案 Tag `{id,name,colorHex,version}`；企业共享、角色权限、版本冲突；同名不擅自禁止，删除/停用继续等待 Q-09 |
| P1 | 公司重要事项 | `/company-important-items` 列表、全文、创建/修改未实现 | 标题/正文、排序、发布者、版本与分页 DTO；发布者取可信身份；与 Android 共用存储；displayOrder 不擅自等同正式“十大”筛选规则 |
| P1 | 错误、分页、并发 | 业务成功信封、游标分页、事务、expectedVersion 尚未实现 | 成功 `{data,requestId}`；当前平铺错误与草案嵌套错误需统一（Web 已兼容两种）；完整读取全部分页；409 不能自动覆盖 |
| P1 | 接口验收 | 没有业务集成/权限测试、正式 OpenAPI 或可联调数据说明 | 新账号可登录、角色生效、跨企业拒绝、会话撤销、并发冲突、跨端可见；提供受控测试账号与示例响应 |

后端其他缺口：任务、日历、日报、工作条目、个人重要事项、Dashboard 也只有设计/分层预留，尚无控制器和用例实现。这些归 Android/后端闭环，不补成 Web 管理员的私人日报、派发或审核入口。消息/审核 API 是 Reserved，不纳入本轮 Web 开放范围。需求文档中的未决项 Q-01～09仍需按影响范围确认。

## 本次 Web 实施方案

### 需求与边界

开发环境可访问真实 `/health`，能区分成功、未就绪、无网络、超时与格式错误。健康成功只证明服务存活，不证明业务或数据库已接通。业务接口未实现时保持本地 repository；不新增猜测的 HTTP repository，不把本机工作台入口包装成真实登录，不向服务器上传现有本地资料。

### 设计

- Vite 开发代理：`/api/v1/...` 原路径转发；`/backend/health` 改写为 `/health`。后端地址由服务端开发变量 `BACKEND_PROXY_TARGET` 指定，默认 `http://127.0.0.1:8080`。浏览器使用同源路径，当前无需修改后端 CORS。
- HTTP client 的默认成功信封保持严格校验；只有显式 `responseFormat: 'json'` 才读取裸对象。独立 health client 校验 `status === 'UP'`，拒绝 HTML 页面或业务信封误响应。
- `src/api/config.ts` 导出 `healthClient`，业务 `apiClient` 保持现有前缀。连接检查放开发命令，不把工程诊断塞入产品设置页面。
- 同源代理仍使用现有 `credentials: 'same-origin'`。这不代表已选定认证方案；跨域 Cookie/Authorization/CSRF 后续按正式身份契约实现。
- Vite 代理只用于 `npm run dev`。生产托管需要站点反向代理或经团队确认的跨域配置；`vite preview` 不能作为本次代理的部署验收。

### 本地检查步骤

```powershell
cd D:\软件开发与测试实训\Pandora6ix\web
Copy-Item .env.example .env.local
# 将 .env.local 中 BACKEND_PROXY_TARGET 改为后端同学提供的地址。
npm run dev
```

另开终端检查经过 Web 代理的连接（将 5173 替换为 Vite 实际端口）：

```powershell
cd D:\软件开发与测试实训\Pandora6ix\web
npm run check:backend -- http://localhost:5173/backend
```

直接检查后端，不经过 Web 代理：

```powershell
npm run check:backend -- http://127.0.0.1:8080
```

未传地址时，命令读取 `BACKEND_URL` 环境变量，否则使用 `http://127.0.0.1:8080`。该独立 Node 命令不自动读 Vite 的 `.env.local`；远程地址建议显式传参数。`.env.local` 不提交，也不填凭据。服务未运行时命令退出码为 1，不能将其记录成联调成功。

## 下一轮业务接入顺序

1. 后端同学先交付身份、企业初始化、本人、组织/人员/角色的正式 DTO、实现及测试数据，解决 P0 阻塞。
2. Web 按正式契约增加 HTTP AdminRepository，替换工作台加载和保存；获取可信身份后接真实登录/退出，避免服务端身份与本地目录混用。
3. 依次接共用标签、公司事项，明确游标分页和版本冲突；核对 Android 的共享数据结果。
4. 联合验收权限、初始化、账号生命周期、冲突、跨设备刷新。本机偏好继续按账号保存。

## 本次任务与验证

- [x] 拉取 dev、快进 Web-cy，并只读核对两个后端分支。
- [x] 建立后端缺口清单及业务接入顺序。
- [x] 实现 Web 开发代理、裸 JSON 健康检查与只读命令。
- [x] 验证自动测试、生产构建和 lint，核对实际服务是否运行。
- [x] 记录最终验证结果，并确认修改路径全部在 web/。

验证结果：`npm test` 共 35 项通过，包括一个由临时契约服务驱动的真实 Vite 代理测试。该测试只证明代理转发、健康 JSON 解析和错误传递，临时服务不是 Spring 后端。`npm run build` 通过；`npm run lint` 无错误，保留 `src/App.tsx` 两条原有 effect 状态更新警告。`git diff --check` 通过，本次改动文件全部位于 `web/`。

本机未发现端口 8080 的后端监听，`npm run check:backend` 按预期以 `NETWORK_ERROR` 和退出码 1 结束，因此没有完成真实 Spring 服务联通或任何业务联调。启动后端属于后端同学的范围，本次只提供 Web 侧检查命令。正式业务接入仍等待后端交付。
