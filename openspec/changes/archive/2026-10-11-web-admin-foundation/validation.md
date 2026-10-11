# Web administration validation — 2026-10-11

## Scope

Web-cy 本地实现，用户已确认按讨论的顺序开发。仅 Web 与本次 OpenSpec；不提交、推送、合并或归档。真实认证和业务接口仍未接入。

## Automated results

- npm ci --no-audit --no-fund：27 个锁定依赖安装成功，未增加运行依赖。
- npm run build：TypeScript 检查与 Vite 生产构建成功，23 个模块。
- npm test：8 个测试通过，0 失败；覆盖持久化/无密码、上下级循环、组织循环/合法层级、重复登录名/无效角色、陈旧版本、非法颜色/可同名标签、存储写失败、损坏数据。
- openspec validate web-admin-foundation --strict：通过。
- git diff --check：通过；README 的 LF→CRLF 提示不属于空白错误。

## Browser results

使用本机临时 Vite 服务及 Codex 内置浏览器；浏览器操作实际验证：

1. 演示入口进入后台；五个导航页面可访问。
2. 新增测试成员，组织设为 Web 开发组、直属上级设为研发负责人，独立字段显示正确。
3. 搜索定位人员；组织筛选正确；无匹配时空状态；重置恢复列表。
4. 编辑职位后取消，出现未保存确认；继续编辑保留；Escape 同样触发保护；放弃后原职位不变。
5. 为测试成员选择团队负责人、取消普通员工，保存后角色列正确。
6. 新增部门、树上选择查看、编辑名称为质量保障部，组织计数与树同步。
7. 新增标签，非法 #GG0000 被拒绝且表单内容保留；改为 #345678 后保存；修改名称；刷新仍能检索。
8. 编辑企业名称成功。
9. 退出先取消仍在后台，再确认退出回到入口；刷新后仍是入口；重新进入保留之前目录数据。
10. 390×844 窄屏：页面整体没有横向溢出（innerWidth=390，document.scrollWidth=375，滚动条占宽）；表格有独立横向滚动。弹窗宽360、高804，处于视窗内且可滚动。
11. 键盘 Shift+Tab 焦点保留在 dialog 中，Escape 可操作关闭保护。
12. 1280×900 桌面完整页面截图检查，全部统计卡片、表格操作和分页可用。浏览器日志未发现 error/warn。

截图保存为当前聊天本地可视化目录的 pandora-web-admin.jpg，未写入仓库。浏览器响应式覆盖恢复默认。

## Limits and next delivery

浏览器中新增的 QA 资料仅留在测试浏览器的本地存储，不进入 Git 种子数据。本次未执行后端/Android 构建、业务 API 联调、数据库测试、真实账号授权测试、全浏览器矩阵或自动可访问性审计。

首轮完成管理端基础页面；企业创建、认证、密码设置、真实会话、服务端企业隔离和真实事务待后续变更。先统一 API0.1 与 backend-foundation 的错误信封、身份域和管理字段，再提供 HTTP repository。


## Continued development — 2026-10-11

本轮持续实现最终管理端：移除产品演示标识，兼容 schema 1 资料迁移，新增公司重要事项与账号显示设置。旧验证记录保留作为历史证据。

- node --test tests/*.test.ts：20 个测试全部通过，新增迁移保留自定义资料、事项纯文本/排序/发布者/版本冲突、偏好账号隔离/失败，以及 API 错误、超时和取消覆盖。
- npm run build：TypeScript 与 Vite 构建通过。
- OpenSpec strict：通过；git diff --check：通过。
- 浏览器：事项新增、完整详情、HTML 字符按纯文本显示、编辑排序、刷新保留、搜索无匹配、取消未保存确认通过。
- 浏览器：本人资料、深色/较大字号、刷新后选中状态保留通过；最终恢复浅色和标准字号。390×844 两个新页面检查无页面横向溢出；已恢复视口。
- 最终浏览器 error/warn 日志为空。截图 pandora-web-admin-progress.jpg 位于聊天可视化目录。

HTTP transport 已完成但未接入业务 repository；真实认证、密码修改、跨设备同步与服务端授权仍待后端契约/实现。未执行 backend 或 Android 测试。本轮未提交、推送、合并或归档。


## Daily administration refinements — 2026-10-11

移除工作页面重复全局统计、英文装饰和口号；调整字号、侧栏、表格行高与间距。新增人员只读详情、排序、每页 10/20/50 条和跨标签页更新提醒。

- 20 项已有规则测试通过；最终 TypeScript + Vite 构建通过；OpenSpec strict 通过。
- 浏览器验证：组织筛选保留于登录名降序与页大小调整；详情组织路径、直属上级和角色显示正确；详情可转入编辑；第 2 页显示第 11 条，改为 20 条回到首屏。
- 另一个真实浏览器标签页保存企业资料后，原标签页出现资料更新提醒；正在输入的职位保持原值，刷新资料按钮禁用。放弃草稿后可手动刷新，通知消失；临时标签页已关闭。
- 390×844：人员详情弹窗宽 360、高约 498，可完整操作；页面 scrollWidth 与 clientWidth 均为 390。窄屏视口已恢复。完整长页截图在弹窗状态下曾失败，随后使用当前视口截图及 DOM 尺寸确认。
- 最终桌面截图 pandora-personnel-refined.jpg 保存在聊天可视化目录，浏览器 error/warn 日志为空。
- 当前仍为本地 repository，未添加虚假的登录、在线状态、时间戳或服务端操作记录。此次未修改业务 API 或数据库。


## Shared App visual language — 2026-10-11

根据用户提供的 App 截图统一暖黄色主色、米白底色、柔和角色配色和边框。theme.css 集中管理浅色/深色主题，移除旧深色样式覆盖；favicon 同步。保留数据自带标签颜色，没有复制来源不明的插画。

- 最终 TypeScript/Vite 构建通过；20 项规则测试通过；OpenSpec strict 与 diff 空白检查通过。
- 浏览器验证标签/事项无匹配结果数、清空搜索恢复列表；事项单页计数与上一页/下一页禁用边界正确。超过 10 条事项的翻页未做浏览器批量数据验证。
- 编辑已有人员时未修改保存按钮禁用；重复登录名保存失败后焦点转到错误提示，输入保留；放弃后原资料未改变。
- 桌面暖色人员页与表单检查通过；深色组织页发现旧白底/蓝色残留后已修复并再次截图验证。
- 390×844 事项页整体无横向溢出，clientWidth 与 scrollWidth 均为 375（滚动条占宽）；恢复默认视口和浅色主题。
- 最终 error/warn 浏览器日志为空，截图 pandora-warm-admin.jpg 保存在聊天可视化目录。
- 尚未进行真实认证或业务接口联调；本轮不提交或推送。


## Requirements audit and working context — 2026-10-11

Web/REQUIREMENTS.md 逐项追踪 FR-USER-01～11、TAG-01/02/03/08、DASH-08 及有关 NFR，说明 Android 范围和 Q-01～09边界。backend 路由搜索及源码确认仅 HealthController 的 /health；未把领域接口当可用 API。

- 23 项自动测试通过，新增组织预填方向、独立上级与已有资料不被默认值改变的检查。最终 TypeScript/Vite 构建、OpenSpec strict 通过。
- 浏览器：组织筛选 org-web 和登录名降序在刷新后保留；选中研发部门并刷新后仍为研发部门。
- 浏览器：从研发部门新增团队，类型 TEAM、上级 org-product 预填正确；新增人员组织 org-product 预填正确，直属上级为空，未自动推导。取消后未新增资料。
- 大字号 390×844 组织页无横向溢出，clientWidth/scrollWidth 均为 375；操作按钮可达。已恢复标准字号及默认视口，仍为浅色主题。
- 长文本增加换行规则；本轮未写入极限长度资料做浏览器数据回归。刷新失败保留快照并禁用编辑已实现，未注入浏览器存储故障；已有 repository 损坏/存储失败测试继续通过。
- 浏览器未发现 error 日志。截图 pandora-organization-audit.jpg 保存在聊天可视化目录。此次未运行业务接口、真实密码、服务端授权、跨设备同步或性能验收；不修改主文档 Planned 状态，不提交、推送或归档。

## Apple-inspired top navigation with App palette — 2026-10-11

参考 Apple 官方 HIG Layout/Materials 调整层次、圆角、留白、系统字体与轻透顶部导航；按用户最新要求保留 App 暖黄色、米白以及深色暖色配色，favicon 同步。移除侧栏 DOM；保留七个 hash 入口、账号、退出和编辑保护。切换页面回到顶部，导航选中项在自身滚动区域内完整可见。

- 23 项既有自动测试通过；最终 TypeScript/Vite build、OpenSpec strict 和 diff 空白检查通过。
- lint 命令改为显式检查 src/tests/vite.config.ts，避免扫描第三方 node_modules；退出码 0，保留两条既有 react(set-state-in-effect) 提示，未声称零告警。
- 浏览器七个导航入口的标题/活动状态正常；深色组织页、390×844 大字号账号页和窄屏人员表单验证通过。窄屏 clientWidth/scrollWidth 均为 375，导航自身横向滚动。企业入口选中框 right 358 < 页面宽 375。
- 未保存人员输入关闭时出现放弃确认，取消测试草稿后未新增资料。恢复浅色、标准字号和默认视口。浏览器 error/warn 日志为空。
- 最终截图 pandora-top-navigation-warm.jpg 保存到聊天可视化目录。认证与业务接口接入边界不变，本次不修改 Android/backend，不提交或推送。

## Form correction and role responsibility — 2026-10-11

- 必填标记采用 field-label 与名称同行；移除组织树图例、表单教程、重复页面描述及角色职责说明。新增按钮保存文案为创建，已有资料为保存修改。
- 人员编辑不再包含角色选择，新人员默认为 EMPLOYEE；角色页面移除职位/直属上级列，仅提供角色编辑，详情中的操作也进入角色表单。saveRoles 保持其他人员字段不变，检查版本与有效角色。
- 24 项自动测试通过，新增角色去重、资料保留、空/未知角色和过期版本拒绝检查。最终 TypeScript/Vite build、OpenSpec strict、git diff --check 通过；lint 退出 0，保留两条既有 React effect 告警。
- 浏览器人员表单已验证，无角色选择/教程。独立确认弹窗默认聚焦继续编辑；Escape 保留输入并返回关闭按钮，放弃关闭全部弹窗并回到新增组织按钮。测试草稿未保存。
- 新增组织预选公司，无请选择上级组织占位；公司根节点编辑没有上级控件。移除图例后树和选中组织仍可操作。角色页面四列和角色专用表单验证通过。
- 390×844 人员表单宽 360、高 626，可完整操作；必填标记与字段名 top 相差约 0.7 px，页面 clientWidth/scrollWidth 均为 375。恢复默认视口，返回公司重要事项。浏览器 error/warn 日志为空。
- 截图 pandora-person-form.jpg、pandora-discard-dialog.jpg、pandora-roles-separated.jpg 保存在聊天可视化目录。企业创建/初始化仍未实现，本次不将组织新增冒充企业创建；不修改后端契约，不提交或推送。

## Compact administration, enterprise workspaces and MVP review — 2026-10-11

- 导航合并为企业管理、组织管理、人员与角色、标签管理、公司重要事项。头像进入我的账号，齿轮进入独立设置；旧 #roles 进入人员页。人员部门/角色分组可折叠，管理员单独首组，每组卡片横向排列，资料/角色操作集中在卡片；组内先显示 12 位并可加载更多。按最高已配置角色排序，不按职位文本猜测权限。
- 企业管理显著创建按钮。本机 catalog 在首次创建时完整纳入旧资料，新企业仅公司根组织及当前管理员，人员/标签/事项隔离，改企业名同步公司名；切换后更新当前资料与页面上下文。保存检查当前企业，防止其他页面切换后旧草稿写入新企业。localStorage 单值写入不是跨页面并发事务或服务端授权。
- 30 项测试通过，新增企业创建隔离/保留、目录写失败、切换后旧编辑拒绝、角色分组及部门子团队筛选。最终 TypeScript/Vite build、OpenSpec strict 和 git diff --check 通过；lint 退出 0，仍有两条既有 React effect 告警。
- 浏览器在单独 localhost origin 验证真实点击创建企业、自动进入新企业、只有管理员和根组织、回原企业保留人员/组织、刷新保留切换；测试标签页关闭。用户 127.0.0.1 origin 没有录入测试企业或改变业务资料。
- 用户页验证部门展开、角色分组順序、搜索定位本人、卡片打开角色编辑后取消；账号与显示设置独立，旧 #roles 兼容。390×844 深色大字号企业页无溢出（390/390），人员部门页无溢出（375/375），8 位部门人员含下级团队，角色表单可操作。恢复浅色、标准字号和默认视口。
- 新截图 pandora-grouped-people.jpg 位于聊天可视化目录；最终浏览器 error/warn 日志为空。
- REQUIREMENTS.md 新增四面板职责表和 MVP 状态。后端 production mapping 仍只有 /health，Android MainActivity 读取 MockData；共享认证、可信企业归属、服务端权限、改密、会话撤销和 Android 共享数据未完成，不能验收系统 MVP。公司十大选择/排序未定，不将本机显示顺序或页大小冒充正式十大规则。
- 本轮未运行 Android/后端全量测试，不改其代码或主文档 Planned，不提交或推送。
