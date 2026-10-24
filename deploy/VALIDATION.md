# 云端部署验证记录

2026-10-15，目标 `https://www.rainycode.cn`，目录 `/opt/Pandora`。

## 已完成

- 原有 `backend-syz` 的审批与 Android 后端接口改动按用户确认同步到 dev；Android/Web 业务源码不在本次部署中改写。
- 云端 Java 21 + Nginx + systemd；应用以 pandora-app 运行，只监听 127.0.0.1:18080。公网仅通过 HTTPS 同源访问页面与业务 API，数据库不开放公网端口。
- 迁移前本地数据库副本经只读业务计数核对：企业、账号、日报、任务均为 0；原文件保留。云端沿用文件 H2，启动执行 V2 迁移，创建首个 Pandora 企业与 ADMIN 后关闭 bootstrap 并重启确认配置生效。
- 管理员登录信息保存在操作者本机 `%LOCALAPPDATA%\Pandora\cloud-access.md`，不在 Git 或 CI 日志。CI 的三个专用部署 Secrets 已加密配置，SSH 强制命令、禁止转发/PTY、固定主机公钥；原有 SSH 账号登录方式未变。
- Let's Encrypt 证书签发成功，域名为 www.rainycode.cn；首次证书到期日 2027-01-13。HTTP 跳转 HTTPS、certbot.timer 启用；`certbot renew --dry-run` 模拟续期通过。
- `/opt/video-monitor` 及三项 Dachuang 容器保持原来停止状态，未删除文件、镜像、证书或网络；未恢复 FactoryVision/Jenkins。

## 自动流水线

首个完整成功的 push 流水线：[run #3](https://github.com/YuzhSong/Pandora6ix/actions/runs/38749315070)，部署提交 `c1a67510209b39a3da83b7595c81634bdcdf892d`。检查状态及公网 deployment.json 均确认该版本、runNumber=3，后端健康 UP。

| 检查 | 结果 |
|---|---|
| 后端 Maven package | 50 项测试，0 失败/错误；独立打包成功 |
| Web | 45 项测试通过、lint 无错误（保留两条既有警告）、生产构建成功 |
| 发布接收器 Linux 测试 | 11 项全部通过：恶意路径/链接/额外文件、摘要错误、缺失/损坏缓存、准确依赖链接、旧发布拦截、模拟失败回滚及数据库不覆盖 |
| 最终精简包启动检查 | CI 实际启动最终应用格式，以独立内存库验证自有 health 控制器、迁移和受保护 `/api/v1/me` 401 |
| OpenAPI | 44 个业务操作、33 个路径，源码与导出一致 |
| 公网验收 | 可信 HTTPS、Web 200、backend/health UP、未认证业务接口 401、发布标记与 GitHub 提交一致 |
| 浏览器验收 | 已使用真实云端管理员登录、读取 Pandora 企业/人员、退出清理会话；无 page/console error，截图与结果保存在本机临时目录 |

验证用截图/结果为本机 `%TEMP%\pandora-cloud-check.png`、`%TEMP%\pandora-cloud-qa.json`，不提交仓库。没有通过给公网添加模拟业务数据来验证；初始管理员和企业是首发实际配置。

## 发布与恢复

成品经依赖缓存后，例行包约 1.5 MB；每次 dev push 均触发测试和发布，没有路径过滤。完整上传和校验后才取激活锁，并有超时。每次发布先停服取得一致的私密 H2 备份，再切换程序、启动和检查健康；失败恢复程序，不自动用旧数据库覆盖新写入。回滚分支已在 Linux 故障模拟中验证；未在已上线账号上额外制造破坏性数据库故障。

首轮整包上传因为跨境传输慢而取消；后续修复了断线上传占锁、运维文件 LF 格式及精简 JAR 目录索引，新增了成品启动检查后成功。失败包未被当作成功发布，未触碰用户既有业务数据。

本文归档与后续操作记录提交仍会触发相同流水线；当前实际部署版本始终以 `https://www.rainycode.cn/deployment.json` 和对应 Actions 结果为准。

## 范围与限制

本次交付是单实例的 dev 云端部署，未迁移为 MySQL/PostgreSQL、未增加 Android 网络接入、未宣称固定规模性能验收。数据库备份位于同一服务器，未配置跨地域容灾；未来有破坏性迁移时需要核对恢复兼容性。既有开发依赖 source-map-js 提示和 H2/Flyway 版本提示未作为本次部署扩大升级范围。
