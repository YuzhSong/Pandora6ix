# Pandora Web 管理端

React + TypeScript + Vite。2026-10-14 基于 dev `f5283e6` 接入后端 PR #10：真实管理员登录、受控首次公司初始化、所属公司信息、组织、人员与角色、直属上级、共用标签、公司重要事项、本人改密和服务端退出。

## 使用组长的服务

需要 Node.js 22.18+。在 `web/` 执行：

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

将 `.env.local` 的 `BACKEND_PROXY_TARGET` 设为实际后端 HTTP(S) 地址（只含 origin，不含 `/api/v1` 或凭据），修改后重启 Vite。浏览器通过同源 `/api/v1` 请求，健康路径 `/backend/health` 转发至 `/health`。打开 Vite 输出地址，用企业登录域、登录名和密码登录；账号必须具有 ADMIN 角色。

尚未发现仓库内的团队服务地址、预设管理员或内置密码。首次企业由负责人员在后端进程环境中配置 `PANDORA_BOOTSTRAP_TOKEN`，再由登录页“首次创建公司”输入授权码、公司与初始管理员资料。密码至少 10 个字符、UTF-8 编码后最多 72 字节。授权码不是登录密码，不写入 `.env.local` 或任何 `VITE_*` 变量。

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

再开一个终端，在 `web/` 执行 `npm run dev`。默认代理指向 8080，使用页面初始化公司后即可登录。若本机 8080 已被占用，应使用团队服务配置或先由操作者处理已有服务，脚本不会终止别人的进程。

## 数据与账号行为

- 页面使用 `src/data/repository.ts` 的 HTTP repository，读取所有游标页，不把首个 100 条结果当全部资料。`supervisorUserId` 映射为页面的独立直属上级，组织为空时显示未分配。
- 服务端校验企业归属、角色、树/上级循环和版本。PATCH 发送完整人员字段、角色和现有状态，禁止自动重试覆盖 409；组织类型和已有登录名按实际接口能力禁止编辑。
- 新人员填写初始密码，默认普通员工；角色在独立界面配置。直属上级只选启用的创始人/部门负责人/团队负责人，ADMIN 单独不能成为业务上级。停用/启用、删除和管理员代重置密码的产品入口未新增。
- 公司事项内容必填、按纯文本展示；发布者由服务器返回。创建事项和首次企业初始化使用稳定幂等键，修改携带 expectedVersion。
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

脚本在新临时数据库中生成随机企业和凭据，经过 Vite 请求真实 Spring，验证初始化幂等、认证、企业/组织/人员/角色、超过 100 条的标签分页、事项幂等/版本、刷新持久化、409、改密与退出撤销，结束自动停止测试进程。它不会连接团队服务器。

实际验证与范围见 [BACKEND_INTEGRATION.md](BACKEND_INTEGRATION.md)，需求状态见 [REQUIREMENTS.md](REQUIREMENTS.md)。历史界面规格与验证见 [web-admin-foundation](../openspec/changes/archive/2026-10-11-web-admin-foundation/validation.md)；本轮仅 Web 的实施记录按用户作用域放在 Web 文档中。
