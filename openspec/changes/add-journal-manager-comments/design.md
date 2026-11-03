## Context

日志可见范围由 `ScopeService.readable` 决定：本人，或业务层级更高且在其管理范围内的上级。日志详情接口为 `GET /work-logs/{id}`，Android 详情页在查看他人日志时只有作者 id，因此显示“成员 xxxx”。

## Goals / Non-Goals

**Goals:** 日志详情展示作者部门信息；上级可在详情页下方留言，作者可见。

**Non-Goals:** 不做留言编辑、楼中楼回复、@ 提醒；不新增推送/站内通知（后续可接入通知模块）；不在 Web 端展示。

## Decisions

1. **权限复用 `ScopeService`**：能读即能看留言；能读且不是作者即能留言。“上级”的定义与日志可见范围一致，不新增规则。不可读时返回统一 404，与日志详情一致。
2. **独立表 `work_log_comment`**（id、enterprise_id、work_log_id、author_user_id、content ≤500、created_at），外键指向日志与用户，按 `(enterprise_id, work_log_id, created_at)` 建索引；删除日志时先删留言。
3. **接口**：`GET /work-logs/{id}/comments` → `{items, canComment}`；`POST /work-logs/{id}/comments` `{content}` → 留言（201）；`DELETE /work-log-comments/{id}` → `{commentId}`。留言视图含 writer 的姓名与职位、`allowedActions`（本人可 DELETE）。
4. **作者信息只放在详情接口**：列表接口不加，避免逐条查询组织。部门取作者组织向上最近的 DEPARTMENT 节点，与 `/me` 的部门口径一致。
5. **Android**：作者信息替换“成员 xxxx”；留言区在内容之后，输入框最少 4 行、500 字计数，发送中禁用并显示进度，失败保留文字。

## Risks / Trade-offs

- [留言没有通知，作者可能看不到] → 本次在日志详情可见；通知接入列为后续。
- [上级调岗后失去读取权限] → 历史留言保留，按现有可见范围决定谁能看到。

## Migration Plan

Flyway V16 新增表，无数据迁移；回滚需删除表与接口（只影响新增功能）。
