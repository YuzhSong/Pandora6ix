# Design

## Context

dev fff4e6e fast-forwarded into Web-cy. Backend 4809241 implements the contract already proposed by Web; see backend/CLIENT_HANDOFF_20261017.md. User scope remains Web; no backend or Android implementation edits.

## Goals / Non-Goals

Goals: verify genuine new backend support and make legacy editing match optional PATCH semantics.
Non-Goals: cloud test-data writes, audit storage selection, avatar API, deployment, automatic push/PR.

## Decisions

- 人员卡片明确标注登录账号并提供查看入口，详情展示可复制的公司登录标识和账号。新建成功使用服务器返回的账号及管理员本次输入的初始密码展示交付弹窗；密码仅保留于当前弹窗 React 内存，关闭/退出即清除，不进入 AdminData、存储、URL 或日志。已有人员只展示账号，不承诺密码查询或重置。

- 人员组织筛选单独加宽至 280px；层级菜单至少 340px 并限制在视口内，名称单行、极窄屏可省略且保留完整路径提示，不改变其他选择框尺寸。

- Keep field-presence support detection for mixed deployments; do not enable old servers by removing the guard.
- Full validation applies to creation. On update validate each non-null value independently. Existing populated fields cannot be cleared because backend COALESCE preserves null values; explicit feedback prevents a false cleared state.
- Required markers/constraints reflect per-field existing data: a missing legacy field is optional, a populated field cannot be erased. Role-only updates retain prior personal data.
- Reuse the backend's current OrgPatch and Web's matching hierarchy/manager validation. Preserve old-server policy feedback for undeployed servers.
- Build an unchanged backend copy under ignored web/.integration with JDK 23 and run actual HTTP repository calls through Vite against an isolated fresh H2 database. Add assertions for successful employee login, single-field legacy update, complete-field reload and both conversion directions. Never touch team data to validate writes.

## Risks / Trade-offs

- Cloud may lag current source → keep compatibility feedback and report local and cloud checks separately.
- File log versus database audit is undecided → leave current pending page and requirements intact.
- Existing frontend validation requires every field on any profile update → regressions cover legacy nulls and known-value clearing.

## Migration Plan

Merge latest dev locally, implement Web compatibility fix, verify isolated persistence, update docs with actual results. No schema change or production write is required by this Web change.
