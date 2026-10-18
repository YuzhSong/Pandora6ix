# 部门与团队删除

2026-10-18，基于 dev `4c4b032`，本地补充；部署状态需另行确认。

`DELETE /api/v1/admin/org-units/{id}` 使用管理员 Bearer 会话，JSON 请求体：

```json
{ "expectedVersion": 0 }
```

成功返回 **204 无响应体**。只删除本公司的空部门或团队，不级联、不删除公司根组织或历史业务记录。直属人员包含停用账号；需先转移全部人员，调整或删除下级组织。当前数据库仅组织父级和人员组织字段直接引用组织，现有外键继续约束，无新增迁移。

| HTTP | code | 行为 |
| --- | --- | --- |
| 401 / 403 | UNAUTHENTICATED / FORBIDDEN | 未登录 / 非 ADMIN |
| 404 | RESOURCE_NOT_FOUND | 不存在、跨公司或已删除 |
| 409 | VERSION_CONFLICT | 预期版本过期 |
| 409 | ORG_ROOT_DELETE_FORBIDDEN | 公司根节点 |
| 409 | ORG_HAS_CHILDREN | 还有下级组织 |
| 409 | ORG_HAS_MEMBERS | 还有直属人员，包括停用账号 |
| 422 | VALIDATION_FAILED | 版本缺失、null 或负数 |
| 400 | 结构化请求错误 | 缺少请求体或非法 JSON |

删除与组织新建/编辑、人员新增/迁移共用企业行锁并在事务中校验。Web 发送当前版本，确认成功后更新组织树、筛选和计数，并选中父节点；失败后须刷新，不能自动重试不确定的删除结果。

先发布 backend 再发布 Web。旧服务器不支持 DELETE 时 Web 提示尚未部署。OpenAPI 由 `python scripts/generate-openapi.py` 生成，以 `--check` 验证。对应 OpenSpec：`delete-organization-units`。
