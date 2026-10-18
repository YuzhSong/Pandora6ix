# 2026-10-17 配置验证

## 实际云端结果

- 初始检查：业务 dev 已更新到 fff4e6e5929688c18f0c8e77c302c58d0040ff1f（run 7），H2 2.4.240，数据库管理员原为空密码。
- 取得一致私密备份 `/opt/Pandora/backups/db-console-20261017T085535Z` 后启用；只增加业务只读视图/数据库查看账号并设置随机管理员口令，原业务计数保持 APP_USER=32、ORG_UNIT=13、TASK=2、WORK_LOG=4、WORK_LOG_ENTRY=6。
- 后端与控制台实际监听同一 PID 的 loopback 18080/18082；ss 无公开 18082 监听。Nginx、应用发布成品与 release SHA 没有改动。
- 两个服务器环境文件为 root 所有、0600，临时待提交口令文件已清除。新随机口令、SSH 私钥、只读登录说明仅在私密外部目录，不进入源码或验证输出。
- 经本机 SSH 隧道实际 HTTP 登录只读账号成功；网页人数 32 与管理员业务 API 分页总数一致，应用登录/退出正常，password_hash 不在人员视图。
- 4 种实际 HTTP 越权查询均被 H2 90096 拒绝：会话表读取、原人员密码列读取、零行 UPDATE、零行 DELETE。空密码 SA 登录被 28000 拒绝。没有为云端验收写入或删除业务行。
- 专用 SSH 实测：18080/22 目的转发被拒绝，远程转发被拒绝，shell 执行失败；18082 本机隧道正常。
- 实际再次停服：控制台同时关闭，独立管理员连接可打开已解锁的同一文件库，再获得一致备份 `/opt/Pandora/backups/console-lifecycle-20261017T085811Z`。重启后应用、网页登录和权限校验再次通过。
- 本机已建立桌面“Pandora数据库查看”“关闭数据库查看”入口；打开脚本自动通过私密配置登录只读网页。隧道 PID 可核验，私密目录 ACL 限当前用户、SYSTEM、管理员。

## 隔离自动验证

- Java 真文件 H2 测试通过：已有数据读取、哈希列排除、原表/凭据表读取拒绝、业务 UPDATE/DELETE/CREATE/ADMIN 提权拒绝、实时提交可见、增量列投影刷新、管理员密码更换与旧密码失败、HTTP 服务和数据库关闭重开持久化。
- 真实精简发布格式测试通过：原应用迁移文件库、prepare 建立 18 个安全视图并设置凭据、同 JVM 启动器、真实 HTTP 读账号登录与 4 个权限拒绝、两次进程重启及停服关闭控制台。证据在忽略目录 backend/target/db-console-runtime.log。
- 本轮未修改 Android/Web/业务模块代码，也未部署新业务构建；未启动或改动其他项目服务。

## 限制与维护

仅配置本机 SSH 私钥的用户可访问 localhost 入口，其他电脑需要单独授权。使用 PANDORA_VIEW 视图；PUBLIC 原表无权限。新的业务表需审核白名单，破坏性迁移需处理视图依赖。源码由本轮独立 PR 供非作者审查，云端启用属于用户明确授权的运维配置。

运维实现提交 ad4bb00，审查 PR：https://github.com/YuzhSong/Pandora6ix/pull/15 。云端 tools/manifest.json 记录对应源码/编译 class 摘要；与业务 release.json 分开追溯。
# Desktop launch repair, 2026-10-18

- Reproduced the desktop target invocation and confirmed the cloud Console was healthy. The prior local tunnel was no longer listening; a cold start restored localhost access. No PR merge or server restart was needed.
- Replaced shortcuts' cached Codex runtime host with Windows-installed PowerShell 5.1; installed scripts with UTF-8 BOM. Added a launcher with private status/error diagnostics and a visible failure dialog.
- Verified stop then cold start, actual desktop shortcut completion, HTTP read-only authentication and SELECT of all 32 current people, and the running Chrome window titled H2 Console.
- Verified a missing-script failure returns exit 1 and writes a sanitized diagnostic rather than reporting success. Browser automation tools could not initialize in this session, so verification used the actual Windows launcher, browser process title and real Console HTTP protocol.
- PR #15 remains open and unmerged.
