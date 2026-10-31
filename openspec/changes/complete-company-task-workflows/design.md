## Context

公司派发面板复用 mine 的 DELEGATED 查询，未区分公开性。任务已有创建/修改审批和完成验收接口，但手机端入口与字段不完整。头像已有 V7 avatar_image，主题只在 rememberSaveable 保存。

## Goals / Non-Goals

**Goals:** 按用户七项顺序完成跨端闭环，每项验证后独立提交。

**Non-Goals:** 开放私有任务、改写旧审批记录、创建额外分支、部署云端或修改无关功能。

## Decisions

1. 增加 task.company_wide 显式标记，历史任务默认 false。仅 FOUNDER 可创建全公司公开派发任务，普通派发默认私有；公司面板仅取同企业已批准公开任务。公开阅读复用任务详情，额外赋予只读内容可见性，执行/修改操作仍由原有权限决定。Android 为创始人派发提供明确开关；用户未明确要求将全部创始人任务公开，不按创建者猜测公开性。
2. 完整编辑复用 TaskEntrySheet，扩充 TaskPatch 对参与人的支持，保留当前生效快照直到上级批准；对编辑中撤掉执行人等冲突做版本和执行状态验证。重复任务遵循已有范围编辑协议。
   参与人变更通过 current_member 标记保留历史执行关系及记录，获批时原子更新当前成员；移出成员的直接操作也必须拒绝。重复范围内各次任务提交各自不可变的修改审批，保留发生日期锚点和当前版本校验，避免原有直接生效路径绕过审批。
3. 执行面板按服务端 allowedActions 和待验收申请版本提供 ACCEPT/REJECT；成功后重新读取真实详情。
4. 驳回重发使用独立新任务，预填被驳回申请快照，提交使用新幂等键，旧审批永久保留；仍进行接收人和标签的当前权限验证。
5. 我的页排列个人资料、审批中心、全部设置（含现有标签入口）、组织信息；删除关于说明。
6. 追踪相册/拍照到上传、数据库读取、客户端缓存的全链路；更新头像后使共享缓存失效，所有人员组件复用 RemoteAvatar，失败不误报上传成功。
7. 按账号本地保存已知主题 key，在登录/重启时读取，设置变更时写入；未知值回退现有默认主题。

## Risks / Trade-offs

### Confirmed UI refinement

- Task assignments project recipientName within existing task authorization; the editor seeds these names and proactively loads only missing names through the existing authorized paginated candidate API, including compatibility with deployed older responses.
- Set all Material surface-container roles to white with transparent tint so alert/date/time/exit-confirmation dialogs inherit white; remove explicit tinted sheet overrides where applicable.
- Keep stored tag colors unchanged. Derive a slightly stronger tint for the creation/editor picker only, based on the actual tag color; all selected tags receive fills. Detail chips use exact persisted color, primary first, with a 1dp theme-neutral outline.
- Move the existing execution state actions and all textual history directly into the execution panel. Remove the description dialog and its launcher.
- Approval screens use the same themed card background as profile with a shared neutral border; preserve semantic approval/rejection badges.

- 新增公开任务阅读容易意外扩大写权限 → 分离只读投影与原有操作授权，覆盖非参与成员、跨企业、普通派发隔离测试。
- 编辑参与人影响执行记录 → 明确保留既有记录与历史，在批准时原子应用；不让等待审批的变更提前生效。
- 老版本后端未部署新增字段 → 同步契约并报告部署边界，不伪造成功。
- 共享模拟器账号可变 → 用隔离集成数据验证权限与流程，记录实际界面验证范围。
