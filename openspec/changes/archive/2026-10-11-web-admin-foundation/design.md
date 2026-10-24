# Design

Requirements audit: maintain a Web-only traceability report against V1.3 USER, TAG and DASH requirements and their actual backend dependencies. Do not convert document Planned acceptance rows to Passed. Add contextual organization creation defaults and preserve mounted page state during refresh; stale snapshots remain readable but mutation controls are disabled after reload failure. Long labels must wrap or have an intentional table scrolling path at increased font size.

Warm application theme: load a dedicated theme stylesheet after structural styles; define neutral surfaces, warm yellow selection/actions, brown action text and pastel role badges for light/dark themes. Keep existing data-provided tag colors. Add search reset/result counts and company-item pagination, disable unchanged edits, and focus failed-save feedback. No new assets, backend fields or unconfirmed business operations are needed.

Continued usability refinement: use neutral administrative wording and compact spacing, remove repeated global statistics from working pages, add a read-only personnel dialog and controlled directory sort/page-size. Observe the existing directory storage key and offer explicit reload for external updates; never reload over an active draft. No new backend contract or business field is introduced.

## Context

见 proposal.md。web 仅有 React 占位页，后端业务 API 尚不存在。API0.1 的管理接口字段待完善，错误信封与当前 backend-foundation 不一致。本次用户已授权按已讨论顺序开发首版，不要求发布。

## Goals / Non-Goals

Goals: 最终管理端持续实现，关系校验集中、数据访问可替换、管理页面与本机设置完整，变更不影响其他模块。
Non-Goals: 正式认证、真实密码、业务 API、SQL、任务与日志操作、审批、统计汇总、企业间切换、正式企业创建、标签删除停用。

## Decisions

- 沿用 React/TypeScript/Vite，不新增 UI 框架或路由依赖；用 hash 路由保留刷新/浏览器返回，组件和 repository 分离。相较只用页面 state，hash 路由可分享和回退。
- 使用单企业演示数据、固定角色与字符串 ID；组织 parentId 和人员 supervisorId 独立。管理关系按 ID 遍历校验，不根据职位文本推导。
- 用 asynchronous AdminRepository 定义查询及保存边界，mock adapter 承担校验、expectedVersion 冲突与 localStorage 版本化数据。每次写入重新读取最新存储；写成功后才发布结果，避免失败后页面假成功。浏览器原型不承担正式授权，后续 HTTP adapter 必须接入服务端权限。
- 开发会话使用 sessionStorage、无账号密码；页面使用工作台入口，不声称身份认证成功。目录资料保留已有 key 并迁移 schema v1 到 v2，只规范化旧种子名称，不覆盖用户自定义资料；本地开发 adapter 与正式后端 adapter 不能混用身份。
- 界面使用深蓝导航、浅灰背景、白色管理卡片与蓝色强调。展示人员/组织真实本地目录计数，不编造任务统计。窄屏导航折行，宽表允许横向滚动。
- 表单使用原生 dialog 与原生输入标签；受控草稿、保存前校验、保存中锁定、错误保留内容、放弃确认与焦点恢复。mock 不增加待确认的标签唯一名规则。

## Risks / Trade-offs

- 本地存储可由访问者修改 → 开发文档明确记录当前 local adapter 与认证待接入；不存凭据、密码。产品去除演示文案不改变此实现边界。
- 两标签同时写 localStorage 非事务 → 版本能检查顺序的陈旧修改，不声称并发事务；正式冲突由后端保障。
- 管理 API 不全 → 不伪造调用；独立 repository 等正式认证、企业接口及错误格式统一后替换。
- 无删除政策 → 不提供组织、人员、标签物理删除。

## Migration Plan

现有占位页替换为管理端；npm ci 与 npm run build 验证。数据使用 pandora-admin-demo-v1 key，与后续服务端数据独立。回滚为恢复本次 web 文件，不修改 Android、后端或共享文档基线。评审及合并前不归档本变更。

## Continued implementation

- CompanyImportantItem 使用 id/title/content/displayOrder/publisherUserId/version，依据现有 DB/API 草案；无删除、发布状态或强制确认规则扩展。新事项立即保存，列表按顺序/id稳定排序，不将空白工作台填成假业务事项。
- theme=light/dark/system，fontSize=small/standard/large，按 userId key 保存。应用根设置 data 属性；system 用 CSS prefers-color-scheme。写失败不更新UI、不显示成功。退出保留偏好，按账号读取避免混用。
- 本人资料为只读，来自当前 local 管理员记录；密码接口未接入时不提供假改密或收集密码。账号页显示密码操作未就绪。
- HTTP transport 接受明确 baseUrl 与 fetch 注入，凭据只按同源发送，读取 JSON data 信封与 204，错误统一为带 status/code/requestId 的 ApiError。仅保存基础设施代码，不启用猜测的管理端整体加载。通过 VITE_API_BASE_URL 配置后续联调，目录 adapter 替换需另补确定的契约。

## Apple-inspired top navigation revision

This user-requested revision supersedes the earlier warm palette and sidebar layout. Refer to Apple HIG Layout and Materials: content-first white/gray surfaces, restrained blue actions, system fonts, rounded controls and a subtly translucent sticky top header. Keep the existing Pandora identity and data-defined tag colors. Place enterprise/account controls in the first header row and all seven destinations in a second horizontal row; overflow scrolls only within navigation at narrow widths. Preserve hash routing, dialog draft protection, local data and display preferences. No font download or UI dependency is required.

Palette correction: the latest user instruction retains the App’s warm yellow and cream palette in both display modes; Apple HIG informs layout, hierarchy and top navigation only. This supersedes the neutral/blue palette decision above.

## Form and responsibility correction

Wrap label text and required marker in one inline field-label element. Remove tutorial text from forms and page panels while retaining errors and meaningful empty-state actions. Show a second native modal dialog for unsaved changes, focus Continue by default and restore focus to the original closing control if editing continues. Personnel forms preserve current roles; newly created personnel default to EMPLOYEE. Role editors use a dedicated version-checked saveRoles operation that updates only roles. Role lists omit supervisor/job-title columns and profile editing. Organization drafts preselect an eligible parent; hide parent selection for existing COMPANY nodes. Keep enterprise initialization listed as incomplete: it cannot be created as a child or second root inside the current enterprise.

## Compact management and enterprise creation

导航只保留企业管理、组织管理、人员与角色、标签管理和公司重要事项；账号由头像进入，显示设置由右上角齿轮进入，旧 roles hash 跳转到人员页。人员按部门/团队上下文或固定角色分组，管理员单独一组，组内横向卡片；角色级别采用已配置角色 ADMIN/FOUNDER/DEPARTMENT_MANAGER/TEAM_LEAD/EMPLOYEE，不从自由文本职位推断。分组可折叠，搜索展开匹配组。

本机 enterprise catalog 用单个版本化 localStorage 值原子保存 activeId 和各 enterprise 的完整 AdminData。未存在 catalog 时读取原 DATA_KEY，首次创建把原数据原样纳入，写入失败不改变原数据或活动企业。新企业仅初始化一个 COMPANY 根组织和当前管理员本机资料，不复制其他企业的人员/标签/事项。企业编辑同步根组织名称，切换后 remount 组织/人员以清除跨企业上下文。企业创建/切换在编辑或加载失败期间不可用，真实认证/企业归属由后端契约另行实现。新增本机隔离与存储失败测试，需求报告继续将真实企业创建标记为待联调。

## Company-scoped administrator correction

此修订取代第 10 轮本机企业切换设计。主导航仅组织管理、人员与角色、标签管理、公司重要事项；旧 #enterprise 路由进入组织管理。本公司名称维护合并进组织页，并同步根组织名称。新浏览器无持久化资料时先显示公司初始化表单，只建立根组织和管理员本机资料，不收集密码或声称创建服务端账号。已有目录直接进入所属公司，不再允许 initializeEnterprise。

为保留已有数据，移除公开 list/create/selectEnterprise 方法，兼容读取已有 catalog，首次读取绑定其中现有 activeId；使用独立 BINDING_KEY 保存该绑定并在以后读取时忽略 catalog activeId 的变化，写入只更新绑定公司的目录。原其他公司目录保留但不展示或切换。DATA_KEY 既有资料保持兼容，初始化在无 DATA_KEY/catalog 时一次写入。本机绑定不是正式身份授权，可信归属仍由后端 /me 决定。
