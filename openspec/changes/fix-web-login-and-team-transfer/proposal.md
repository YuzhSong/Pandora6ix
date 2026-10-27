# Proposal

## Why
管理员要求精简登录页并以组织管理作为登录入口，同时报告普通员工跨团队岗位调整返回 DATA_CONFLICT。需要验证并修复实际失败路径。

## What Changes
- 删除登录页底部管理员使用说明。
- 默认和重新登录后的入口改为组织管理，保留已登录时的有效深链接。
- 增加跨团队岗位调整回归检查，修复已确认的失败原因并提供中文冲突提示。
- 组织管理只显示在职人员，统一成员列表与组织树人数；日志详情使用中文名称、状态并扩大可读区域。

## Capabilities
### New Capabilities
无。
### Modified Capabilities
- `web-admin-shell`: 精简登录内容，组织管理作为默认登录入口。
- `organization-directory`: 当前组织成员与计数排除已离职人员。

## Impact
Web 登录、路由和错误提示；人员岗位调整的后端回归检查及必要修复。不新增 API 或数据库表。
