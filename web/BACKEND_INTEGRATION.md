# Web 后端接入检查与实施记录

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
