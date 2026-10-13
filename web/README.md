# Pandora Web 管理端

React + TypeScript + Vite，持续开发企业管理员的 Web 工作台。已实现首次公司初始化、所属公司信息维护、组织、人员与角色分组、直属上级、共用标签、公司重要事项，以及头像账号与独立显示设置。

## 本地开发

使用 Node.js 22.18+（规则测试直接运行 TypeScript）。

```powershell
cd web
npm ci
npm run dev
```

打开终端显示的地址，点击“进入工作台”。当前认证服务尚未接入，这个入口只管理浏览器工作空间会话。

```powershell
npm test
npm run build
```

## 数据与实现状态

- AdminRepository 隔离页面与存储，目前使用 createLocalAdminRepository。业务接口尚未接入，修改保存在当前浏览器；不提供服务端授权、跨设备同步或多标签页并发事务。
- 为兼容已录入资料，保留存储键 pandora-admin-demo-v1，数据结构迁移到 schema 2。兼容既有 pandora-enterprise-catalog-v1 目录，首次读取绑定当时所属公司并保持绑定，其他公司资料保留，不提供列表和切换入口。迁移保留自定义资料，只更新原始默认名称；会话键也保持兼容。退出保留资料。
- 人员与角色合并入口，按部门或最高业务角色分组，管理员单独首组；可折叠横向账号卡片，每组先显示 12 位并支持加载更多；所属组织与直属上级独立配置。人员编辑只维护资料与组织关系；新人员默认普通员工，角色在独立页面配置。保存校验循环、重复登录名、角色、颜色、版本冲突与存储失败。
- 公司重要事项支持新增、编辑、搜索、完整纯文本详情和显示顺序；编辑保留发布者，正文不作为 HTML 执行。
- 显示设置按账号保存，支持浅色/深色/跟随系统及三档字号。写入失败保留原设置。
- 密码修改、账号认证、启停用与删除尚未开放。Web 管理员职责不包含私人日志查看或任务审批。

## 接口接入

src/api/client.ts 提供统一 HTTP 请求、超时、成功信封、204、401、409、请求标识及两种后端错误格式的处理。复制 .env.example 配置 VITE_API_BASE_URL，不要填写凭证。它尚未替换业务 repository；须先确认 DTO、认证方式和企业初始化契约，再接入服务端。

已规划接口包括 /auth/login、/me、/admin/users、/admin/org-units、/tags、/company-important-items，前缀 /api/v1，实际可用性以 backend 实现与团队契约为准。

OpenSpec：web-admin-foundation；验证记录见已归档变更的 [validation.md](../openspec/changes/archive/2026-10-11-web-admin-foundation/validation.md)。

2026-10-13 接入基础：开发服务器已配置 `/api/v1` 代理及 `/backend/health` → `/health` 的映射，复制 `.env.example` 到 `.env.local` 后设置 `BACKEND_PROXY_TARGET`。运行 `npm run check:backend -- http://localhost:5173/backend` 检查实际 Web 代理连接（端口以 Vite 输出为准）。健康成功只说明服务存活，业务 repository 仍是本地存储；dev 尚无业务 API。后端分支审查、接口缺口、环境配置及下一轮接入顺序见 [BACKEND_INTEGRATION.md](BACKEND_INTEGRATION.md)。

其他标签页更新资料时会提示刷新，编辑期间保留输入并禁止刷新覆盖。

视觉参考 Apple 官方 HIG，采用顶部横向导航及清晰卡片层次，配色保留 App 端的暖黄色与米白，由 src/theme.css 管理浅色/深色配色。标签与事项搜索提供清空及结果数，事项按每页 10 条显示；已有记录未修改时不能重复保存。

## 需求完成情况

逐项状态、未完成要求及后端接入顺序见 [Web 需求核对](REQUIREMENTS.md)。本地人员录入不等于账号可登录，角色配置不等于服务端授权完成。

表单必填标记与字段名称同行，关闭有修改的表单时使用独立确认弹窗。新增组织按当前上下文预选上级；企业根组织编辑不显示上级选择。企业创建仅在无资料时的首次初始化流程完成，本机初始化公司根组织和当前管理员资料；不创建服务端身份或凭据，不代表可信企业归属初始化已完成。

主导航为组织管理、人员与角色、标签管理、公司重要事项。公司信息维护合并到组织管理；旧 #enterprise 链接兼容到组织管理。我的账号从右上角头像进入，显示设置从齿轮进入；旧 #roles 链接兼容到人员页。完整四面板职责与 MVP 缺口见 REQUIREMENTS.md。


2026-10-11 公司范围收敛验证：30 项自动测试通过；生产构建通过；lint 无错误（保留两项既有 effect 状态更新警告）；OpenSpec 严格校验通过。浏览器验证既有公司资料、四项主导航、公司信息编辑、旧企业路由、独立空环境初始化及刷新持久化、390px 窄屏弹窗。初始化只生成本机资料，可信账号认证与服务端权限仍待接入。
