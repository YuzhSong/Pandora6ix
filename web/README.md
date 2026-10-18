# Pandora Web 管理端

React + TypeScript + Vite。2026-10-18 已同步 dev `fff4e6e`，接入后端 `4809241` 的人员资料和组织类型变更。Web 只提供所属公司的组织、人员/角色、公司重要事项和系统日志待确认页面；无创建公司、自助注册、标签或员工业务入口。

新人员与账号一次创建，性别、身份证号、电话必填；历史资料允许逐项补充。叶节点部门/团队可转换，受层级、子组织、负责人角色与版本约束。系统日志存储方式仍待甲方确认；头像保存接口仍未提供。

## 使用组长的服务

需要 Node.js 22.18+。在 `web/` 执行：

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

将 `.env.local` 的 `BACKEND_PROXY_TARGET` 设为实际后端 HTTP(S) 地址（只含 origin，不含 `/api/v1` 或凭据），修改后重启 Vite。浏览器通过同源 `/api/v1` 请求。`BACKEND_HEALTH_PATH` 指定上游健康路径：直连 Spring 用 `/health`，经过团队云端网关用 `/backend/health`。打开 Vite 输出地址，用企业登录域、登录名和密码登录；账号必须具有 ADMIN 角色。

2026-10-16 已同步 dev `5992eb2`。团队站点是 **https://www.rainycode.cn**（不是 `.com`），组长已部署后端与共享 H2 数据库，详见 [部署记录](../deploy/README.md)。直接使用云端页面无需启动本地后端；继续开发本地 Web 时，在 `web/` 执行：

```powershell
Copy-Item .env.cloud.example .env.local
npm run dev
npm run check:backend -- http://127.0.0.1:5173/backend
```

本地 Web → Vite 同源代理 → 云端 HTTPS 后端 → 共享数据库。配置中没有数据库账号或密码；云端账号与此前本机测试数据库账号独立，登录后新增/编辑的内容会写入团队共享库。云端企业及 ADMIN 已初始化，请向组长取得企业登录域、登录名和密码；不要重新创建测试企业或上传本机演示数据。复制配置会覆盖已有 `.env.local`，如有自定义设置应先保留。

登录页由用户填写所属公司的登录标识、管理员账号和密码。固定公司归属由服务端会话保证，Web 不提供创建公司、公司切换或员工注册。公司展示名称与登录标识不同，修改名称不会改写登录标识。

已有公司直接用管理员账号登录。仅首次部署时由部署负责人通过后端受控初始化流程创建公司与首位管理员；授权码不进入 Web、不写入 VITE_* 变量。Web 登录失败保留输入，密码仅留在当前表单内存。

## 仅本机运行后端原样副本

本机已验证 `D:\Java` 为 JDK 23，可运行 Java 21 后端。另开 PowerShell：

```powershell
cd D:\软件开发与测试实训\Pandora6ix\web
$env:JAVA_HOME = 'D:\Java' # 其他机器请换成 JDK 21 或更新版本
# 首次初始化需要在本终端私下设置 PANDORA_BOOTSTRAP_TOKEN。
# 可用安全输入，避免把授权码写进命令历史或仓库：
$localBootstrap = Read-Host '设置本机初始化授权码（请自行保存以便页面输入）' -AsSecureString
$env:PANDORA_BOOTSTRAP_TOKEN = [Net.NetworkCredential]::new('', $localBootstrap).Password
npm run dev:backend
```

该命令按后端源码提交建立原样副本并构建，绑定 `127.0.0.1:8080`；数据库持久化在 `web/.integration/local/`。它只写 Web 忽略目录和 Maven 依赖缓存，不修改原 backend 或其数据库。已有本机企业再次启动无需配置初始化码，可直接登录；Ctrl+C 停止服务。公司数据不会自动复制到团队后端。

再开一个终端，在 `web/` 执行 `npm run dev`。默认代理指向 8080（若已有 .env.local 则以其配置为准）；需先由部署负责人通过受控后台流程初始化本机公司，页面只提供登录。若本机 8080 已被占用，应使用团队服务配置或先由操作者处理已有服务，脚本不会终止别人的进程。

## 数据与账号行为

- 页面使用 `src/data/repository.ts` 的 HTTP repository，读取所有游标页，不把首个 100 条结果当全部资料。`supervisorUserId` 映射为页面的独立直属上级，组织为空时显示未分配。
- 服务端校验企业归属、角色、树/上级循环和版本。PATCH 发送完整人员字段、角色和现有状态，禁止自动重试覆盖 409；已有登录名不可改；非根叶组织可变更类型，公司根组织固定。
- 空部门/团队可通过组织详情的“删除组织”确认删除。公司根组织不可删除；仍有下级或人员（含停用账号）时显示阻止原因。需要部署本次新增的后端 DELETE 接口，契约见 [组织删除](../backend/ORG_DELETION.md)。
- 新人员填写登录名、初始密码、性别、身份证号和电话，服务端同一事务创建资料与账号，默认普通员工；历史人员缺失资料可逐项补充，已填值可修改但不能清空；角色在独立界面配置。直属上级只选启用的创始人/部门负责人/团队负责人，ADMIN 单独不能成为业务上级。人员停用/启用及删除入口未新增。
- 人员卡片明确标注登录账号，点击“账号与初始密码”可查看、复制公司标识、账号和本页保留的交付密码；管理员详情可查看完整身份证号。新建/重置成功后显示交付弹窗，并在当前页保留记录。退出或整页重载会清除记录，不存浏览器持久存储，不从哈希回读实际密码。
- 新增 CSV 批量创建（1～100 人）、逐行预检/结果/停止后续创建，以及账号清单批量复制、TXT 下载。员工忘记密码可通过版本保护的重置接口设置新密码并撤销旧会话，需要部署本地新增后端后才生效。员工使用员工端登录，Web 仍只允许管理员。操作方法见 [账号批量创建与交付](ACCOUNT_DELIVERY.md)。
- 公司事项内容必填、按纯文本展示；发布者由服务器返回。创建事项使用稳定幂等键，修改携带 expectedVersion。
- Bearer 会话只保存在当前标签页 sessionStorage，包含令牌与过期时间；密码、初始密码和初始化授权码不保存。刷新后读取 `/me` 验证身份；401/过期清理快照并回到登录。退出成功才清理本机身份，网络失败保留会话以便重试。改密成功撤销当前会话，回到登录。
- 原 localStorage 目录和旧工作台会话不用于服务端身份，不自动上传或混入新资料。旧适配器保留用于历史迁移测试。主题与三档字号继续按可信账号 ID 存在本机。
- Web 管理员只维护所属公司目录及事项；不增加私人日志查看、任务派发、审批或跨公司切换。

## 检查

```powershell
npm test
npm run build
npm run lint
npm run check:backend -- http://localhost:5173/backend # Vite 实际端口可能不同
```

`check:backend` 也可直接传后端地址，只检查服务存活。独立 Node 命令不自动读取 `.env.local`。生产部署需同源反向代理，Vite 的开发代理不会进入生产构建；不要用跨域 Cookie 配置替代本项目实际 Bearer 契约。

真实联调检查（先用 `dev:backend` 完成副本构建，再停止该实例）：

```powershell
$env:JAVA_HOME = 'D:\Java'
npm run test:integration
```

脚本在新临时数据库中生成随机企业和凭据，经过 Vite 请求真实 Spring，验证初始化幂等、认证、新人员账号登录及权限隔离、历史资料逐项补充、人员资料持久化与角色保留、组织类型双向转换及空组织删除、员工密码重置与旧会话撤销、CSV 批量创建、标签管理关闭、事项幂等/版本、刷新持久化、409、改密与退出撤销，结束自动停止测试进程。它不会连接团队服务器。

验证尚未提交的 backend 修改时，先在 backend 执行 `.\mvnw.cmd -DskipTests package`，再在 Web 设置 `$env:PANDORA_INTEGRATION_JAR='../backend/target/backend-0.0.1-SNAPSHOT.jar'` 后运行联调脚本。脚本同时验证空部门/团队的删除、旧版本拒绝和重读后消失，避免误用先前提交的后端副本。

实际验证与范围见 [BACKEND_INTEGRATION.md](BACKEND_INTEGRATION.md)，需求状态见 [REQUIREMENTS.md](REQUIREMENTS.md)。历史界面规格与验证见 [web-admin-foundation](../openspec/changes/archive/2026-10-11-web-admin-foundation/validation.md)；本轮 Web 交付流程见 ACCOUNT_DELIVERY.md，后端组织删除、员工密码重置及运维脚本在 backend 文档中独立说明。
