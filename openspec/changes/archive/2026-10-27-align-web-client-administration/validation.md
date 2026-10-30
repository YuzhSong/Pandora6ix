# Validation — 2026-10-16

## PR #12 提交范围复验

- 排除已取消业务页面的未跟踪草稿（BusinessPages.tsx、businessReader.ts、businessReader.test.ts），保留在本机；PR 未包含这些文件、.integration、.env、本机凭据、数据库或截图。
- 对 Git 索引中的 Web 测试执行 `node --test`：66/66 通过（此前 74 项包含本机业务阅读草稿的 8 项，不计入本 PR）。
- 对索引内源码、测试与开发脚本执行 oxlint：无错误，仅 App 两项既有 effect 警告。
- 最后一次生产构建通过；OpenSpec 严格校验、暂存 diff 检查通过。
- 本次更新现有 Web-cy → dev 的 PR #12；组织类型、人员新字段、头像保存与真实审计采集仍按后端交接清单等待配合。不自动合并或部署。

## 默认展开、头像和事项排序

- 完整 Web 测试 74/74 通过；生产构建通过，lint 无错误（4 项既有警告），OpenSpec 严格校验与 diff 检查通过。
- 真实人员页首次挂载：公司及三个部门均展开，团队收起。组织选项部门 ●、团队 ○ 的数据测试通过，层级路径保持不变。
- 真实账号页点击头像成功打开修改头像对话框，保存按钮禁用且说明待后端接入；关闭后返回账号页。上传检查 PNG/JPEG/WebP 及 5 MiB 上限，不写本地存储、不请求后端。
- 真实事项页使用置顶把最后一项移至草稿首位，显示顺序 1，其余依次重排；保存/取消控件出现，取消恢复原列表。此次未保存云端顺序。拖动与按钮共用移动函数，版本化保存及部分失败停止由测试验证。
- 截图：web/.integration/avatar-click-dialog-live.png、web/.integration/company-order-draft-live.png。

## 人员分层追加验证

- 公司层级追加：组织树与人员分组 8 项测试通过，构建通过，lint 无错误（4 项既有警告），OpenSpec 严格校验及 diff 检查通过。
- 真实页面公司组显示 31 位非管理员，部门嵌套其下。组织筛选顺序为 6ix → 部门 → 团队，包含无人员的既有团队；选择产品研发部显示其直属成员和下属团队，共 19 人，并保留公司祖先路径。
- 组织筛选截图：web/.integration/organization-filter-hierarchy-live.png。

- 人员分组 5 项测试通过，覆盖直属成员、团队成员不重复、匹配路径及人数、独立管理员和角色分组。
- 生产构建通过，lint 无错误（保留原有 4 项警告），OpenSpec 严格校验、git diff --check 通过。
- 在真实云端资料的本地 Web 页面展开产品研发部，显示直属人员及 Android/Web/后端/需求团队；Web 团队展开显示 4 人。收起再展开部门后，Web 团队展开状态仍保留。只操作读取与本地展开状态，未修改后端数据。
- 效果截图：web/.integration/personnel-hierarchy-live.png。

- `npm test`：66/66 通过。初次沙箱内运行有两项 loopback 代理测试网络失败；获准在沙箱外重跑后全部通过。
- 对组织策略错误码调整后，针对 HTTP repository、组织类型和人员资料重跑 13 项测试全部通过。
- `npm run build`：TypeScript 与 Vite 生产构建通过。
- `npm run lint`：无错误，4 项既有 set-state-in-effect 警告（App 2 项，保留但不挂载的 BusinessPages 2 项）。
- `git diff --check`：通过，仅提示现有 Git 配置的行尾转换。
- `openspec validate align-web-client-administration --strict`：通过。
- 检查 App/AuthPage/AdminPages/AdminEditor/HTTP repository，无标签页、公司开通或业务页面挂载；HTTP 管理资料不再请求标签或业务数据。
- backend/ 与 android/ 没有变更。未进行云端写入、新字段真实数据库联调或真实系统日志采集验收。当前后端仍禁止组织类型变更、没有个人资料字段；Web 已处理依赖提示和拒绝反馈。
- 实施阶段在 Web-cy 本地检查；用户随后授权提交至现有 PR #12，提交时排除未挂载的业务阅读源码与测试、临时文件及本机配置。
