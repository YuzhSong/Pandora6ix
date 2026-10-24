# 初始账号密码交付

本地实现，需配置密钥并部署后端和 Web 后生效。不会自动修改云端既有账号、密码或补造历史交付记录。

## 生命周期

- 管理员新建非 ADMIN 人员或重置员工密码时，事务内同时保存 BCrypt 登录哈希和独立加密交付记录。任何一步失败整体回滚。
- 员工通过 `POST /api/v1/me/password` 自行改密，事务内删除交付密文并标记 CHANGED，沿用原有会话撤销规则。员工选择的新密码只保存 BCrypt，不进入交付记录；即使新旧密码文字相同也不再向管理员提供。
- 管理员重置后生成新的交付记录。已有记录被替换，旧版本不再可用。
- 分配 ADMIN 角色或停用人员时清除记录；停用人员和管理员不导出密码。外部修改哈希会使原交付记录失效。
- 默认保留 168 小时（七天），到期后不能读取；每分钟清理过期密文。可配置 `pandora.handoff-retention-hours`（1～720 小时）。
- 管理员刷新、退出重登、后端重启后仍可导出有效记录。导出文件由管理员自行交付；已经下载的历史文件无法由服务端收回，不能当作员工改密后的当前密码。

## 加密配置与部署

Flyway V5 创建 `credential_handoff` 表。登录密码哈希与可交付临时密码分离。使用 AES-256-GCM、每次独立随机 nonce，并绑定公司/用户 ID；数据库不保存解密密钥。不得把生产密钥提交 Git、写入前端或日志。

运维在受控环境生成 32 字节随机密钥，例如 `openssl rand -base64 32`，放入服务器受保护的环境配置 `PANDORA_HANDOFF_KEY`，保存在独立密钥管理/备份中。所有后端实例必须使用同一密钥。测试配置中的公开 fixture key 仅用于临时测试数据库，不能用于部署。

密钥缺失：服务可以启动及读取无密码状态，但新建员工/重置密码以 `503 HANDOFF_KEY_NOT_CONFIGURED` 回滚；不假装保存成功。密钥格式错误会阻止启动，错误或丢失的有效格式密钥读取旧记录时返回 `503 HANDOFF_UNAVAILABLE`，不尝试猜测密码。

先备份数据库、配置密钥，再部署后端自动迁移 V5，最后部署 Web。验收创建 → 退出重登/服务重启 → 导出 → 员工改密 → 不再导出 → 管理员重置 → 导出新密码。不得用真实员工账号做破坏性验收。

轮换密钥需在计划窗口先清空旧交付密文并安排必要的重置，或另行实施多密钥迁移；本轮未提供自动轮换。回滚到不支持清除交付记录的旧后端前必须先清空交付记录，避免旧代码改密后留下可读取秘密。未在本任务执行部署或生产数据操作。

## 查询接口

`GET /api/v1/admin/credential-handoffs`，可选 `userId` 查询单个员工。仅本公司 ADMIN，跨公司/不存在对象 404，非管理员 403，ADMIN 目标 403。响应 `Cache-Control: no-store, private`，标准 data/requestId 包装：

```json
{"items":[{"personId":"<id>","displayName":"<name>","loginName":"<current login>","enterpriseDomain":"<domain>","status":"AVAILABLE","initialPassword":"<temporary password>","expiresAt":"<ISO instant>"}],"nextCursor":null}
```

状态 AVAILABLE / CHANGED / EXPIRED / UNAVAILABLE，只有 AVAILABLE 带 initialPassword 和 expiresAt。普通人员查询、创建/编辑返回、本人资料均不返回密码、密文或哈希。前端不会把密码存入 localStorage/sessionStorage、人员快照或持久浏览器状态。

Web 详情和批量导出按需查询，窗口显示期间每五秒及重新聚焦刷新；复制/下载再次请求最新数据。窗口关闭、读取失败或会话失效后丢弃密码。无法从此前只有 BCrypt 的历史账号恢复初始密码，不会用约定默认值冒充服务端记录。
