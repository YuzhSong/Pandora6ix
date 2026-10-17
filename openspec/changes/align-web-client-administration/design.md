# Design

## Context

See proposal.md. User explicitly limits implementation to Web. Backend OrgPatch rejects kind changes; UserCreate/UserPatch lack personal fields. Existing working tree includes uncommitted business-reader pages which must not be destroyed.

## Goals / Non-Goals

Goals: enforce the revised Web surface, prepare validated personnel forms and honest API compatibility, document backend handoff.
Non-Goals: backend/Android changes, deployment, actual audit collection or storage selection.

## Decisions

- 初始展开非 TEAM 组织，团队默认收起；筛选选项增加独立装饰符号，保持纯名称用于选中值及键盘查找，辅助技术读取完整路径。
- 用户确认仅账号卡片内头像打开修改弹窗，页头头像进入我的账号；头像仅做上传界面等待后端：使用内存 object URL 预览 PNG/JPEG/WebP（5 MiB 上限），卸载/替换时释放，不存本地、不调用上传接口、不替换正式头像。
- 事项排序是完整列表的本地草稿，拖动和上/下移/置顶共用纯函数。保存逐条调用已有 PATCH，保留原 title/content/version，仅修改 displayOrder；当前无批量事务 API，因此失败后刷新服务端状态并说明可能部分成功，不自动回滚覆盖并发变更。草稿未处理前禁止普通事项编辑，筛选时禁止排序，提供分页间按钮移动。

- 组织人员分组按公司 → 部门 → 团队构建完整树，people 仅包含直属成员，children 维护下级，总人数累计匹配后代。每层独立展开及显示更多；筛选时展开命中路径，角色分组保持平铺。筛选选项共享同一组织树，前序排序并按深度缩进；空组织可供筛选但无匹配人员的分组不展示。管理员仍独立优先展示。

- Remove company initialization from AuthPage and public auth service. Keep login-domain input because the current login contract requires it; company creation APIs are deployment concerns.
- Remove tag page/editor and business routes, stop loading /tags. Preserve currently uncommitted business-reader files but remove them from application entry points.
- Add nullable optional gender/idNumber/phoneNumber to Person. Presence of all three keys on every /admin/users item advertises the proposed profile contract; missing keys mean old server. This is a compatibility convention for backend agreement, not proof of persistence. Reject new creation and personal-field edits before writing to an old server. For supported responses require returned fields to equal submitted values. Server mismatch reports possible partial write, retains form and requires verification before retry.
- Keep administrator-assigned login/initial password and a single POST /admin/users to create account and employee atomically. No browser password storage. Validate nonblank profile and bounded identity/phone format without treating format validation as identity verification.
- For department/team conversion validate the resulting whole hierarchy and assigned manager roles, filter cyclic parents, then submit existing OrgPatch DTO. The current backend's policy error remains visible. No automatic movement of children or role changes.
- Add system-log pending page only. Backend must collect cross-client events; browser-only logging cannot cover Android tasks and journals. Document event fields, permissions and undecided storage/retention.

## Risks / Trade-offs

- Old backend blocks new employees → show explicit pending integration notice, keep supported existing edits usable.
- Current backend rejects type edits → retain draft and describe required server changes in handoff.
- Personal fields → display masked identity in details, do not log profile bodies or add browser persistence.
- Removing navigation is not server authorization → document backend restrictions; leave business data intact.

## Migration Plan

Ship Web locally with pending states, coordinate documented backend additions, then validate supported server writes and reloads. Reverting Web changes restores the earlier interface without database changes.

## Open Questions

甲方需选择系统日志存储（文件 log / 数据库）、保留期限、查询和导出范围。此决定不影响本次仅展示待接入状态的实现。
