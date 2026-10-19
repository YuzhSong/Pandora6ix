# 管理员重置员工密码

本地新增，云端需部署后生效。对应 `normalize-employee-login-credentials` OpenSpec；一次性拼音改名/统一密码维护仍由组长按 `scripts/EMPLOYEE_ACCOUNT_MAINTENANCE.md` 执行。

`POST /api/v1/admin/users/{id}/password-reset`，使用本公司 ADMIN 的 Bearer 会话：

```json
{ "expectedVersion": 0, "newPassword": "<管理员本次设置的新密码>" }
```

- 密码至少 10 字符，至多 72 个 UTF-8 字节，不能用字符数量绕过 BCrypt 字节限制。
- 只允许重置本公司非 ADMIN 的人员；管理员自己改密仍使用 `/me/password`。
- 成功 200 返回现有管理员 User 投影（资料与角色保留，version 加 1）。不返回明文密码或密码哈希。
- 同一事务撤销该员工所有旧会话；管理员及其他员工会话保持有效。停用账号不会因重置变为启用。
- 未登录 401；非管理员或 ADMIN 目标 403（后者 `ADMIN_PASSWORD_RESET_FORBIDDEN`）；不存在/跨公司 404；过期版本 409；缺失、负版本、无效密码 422。
- 使用公司行锁和用户行锁，与组织/角色维护及员工本人改密串行，版本条件保护并发提交。

Web 需在请求成功后才显示本次新密码的交付记录。当前页面可查看、复制或批量导出；退出或整页重载清除内存，不通过 API 查询员工自行修改后的真实密码。接口未部署时明确显示部署提示。
