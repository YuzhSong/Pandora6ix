# Pandora 云端部署

站点：`https://www.rainycode.cn`；业务 API：`https://www.rainycode.cn/api/v1`；健康检查：`https://www.rainycode.cn/backend/health`。Web 与未来 Android 通过同一后端访问云端数据库。

## 自动发布

`.github/workflows/deploy-dev.yml` 监听远端 `dev` 的每次 push（无路径过滤），也支持在 dev 手动运行。GitHub Actions 完成后端测试/打包、OpenAPI 核对、Web 测试/lint/build 和发布器测试后，才上传成品。

SSH 专用账号 `pandora-deploy` 仅能执行 root 管理的发布接收器，不具有任意 SSH 命令、PTY、转发或 SFTP 权限。三个 GitHub Actions Secrets 为 PANDORA_DEPLOY_HOST、PANDORA_DEPLOY_KEY、PANDORA_DEPLOY_KNOWN_HOSTS；私钥和 SSH 登录密码不进仓库。官方 setup/checkout actions 固定到已核验的完整提交。

打包工具将 Spring Boot 成品拆为应用类 backend.jar 与内容寻址的 Java 依赖。受限 SSH 先查询服务器缺少的依赖摘要，只传应用、web/、release.json 和缺少的 libraries/ 文件。依赖存于只读缓存，每个发布目录由接收器生成准确的 classpath 链接；不会混用旧版本依赖。release.json 记录提交、runNumber/runAttempt 及逐文件 SHA-256；接收器限制上传大小、解压大小和文件类型，拒绝链接、路径穿越、额外文件、校验不符及过时的发布。完整接收/校验后才获取激活锁，上传有超时，断连不会阻塞后续发布。实际激活串行执行，旧流水线不能覆盖新发布。重跑使用独立 attempt 目录。

发布完成后，`/deployment.json` 返回当前部署的 sha/runNumber/runAttempt/branch，可与 GitHub Actions 对照。域名为 dev 环境，不把 dev 发布自动等同 main 稳定发布。

## 云端布局

| 路径/服务 | 用途 |
|---|---|
| `/opt/Pandora/releases/<sha>-<run>-<attempt>` | 不可变 Java/Web 成品 |
| `/opt/Pandora/current` | 当前程序软链接 |
| `/opt/Pandora/libraries/<sha256>.jar` | root 管理的共享依赖缓存，各发布仅链接自己的精确版本 |
| `/opt/Pandora/shared/data/pandora.mv.db` | 云端持久化 H2 文件库，发布不覆盖 |
| `/opt/Pandora/config/backend.env` | root-only 数据源/bootstrap 环境；不提交 Git |
| `/opt/Pandora/backups` | 每次发布前停服取得的一致数据库备份，root-only |
| `/usr/local/sbin/pandora-receive-release` | 受限 SSH 发布入口 |
| `pandora-backend.service` | pandora-app 用户、Java 21、回环 18080、堆上限 512 MB |
| `/etc/nginx/sites-available/pandora` | Web、API 反向代理和 HTTPS；Certbot 管理证书配置 |
| `certbot.timer` | HTTPS 自动续期 |

首次迁移的是已停止的本地 H2 文件副本，原文件保留。迁移前核对原库企业/账号/日报/任务均为 0，首次发布后通过受控初始化创建 Pandora 企业及 ADMIN；没有把测试账号或演示任务搬上云。初始化授权码随后关闭，管理员密码仅保存在操作者本机 `%LOCALAPPDATA%\Pandora\cloud-access.md`。现行身份、角色和业务时区规则不变，ADMIN-only 不自动获得业务审批权。

本阶段沿用已验证的单实例 H2，数据库不监听公网。更换 MySQL/PostgreSQL 或扩为多个后端实例，需要另行验证驱动、DDL/事务和迁移；不能让多个独立后端直接共写这个文件。

## 日常运维

```bash
sudo systemctl status pandora-backend nginx --no-pager
sudo journalctl -u pandora-backend -n 80 --no-pager
curl -fsS http://127.0.0.1:18080/health
curl -fsS https://www.rainycode.cn/deployment.json
sudo certbot renew --dry-run
```

每次发布先停止后端，在停止期间备份数据库，然后原子切换程序并重启，等待健康检查；存在短暂重启窗口。若新程序不健康，接收器自动恢复旧程序链接并重启，GitHub Actions 失败。数据库不会自动回退，避免覆盖用户新数据。将来有破坏性迁移时，旧程序可能不再兼容新库；届时需核对备份并显式恢复，不能仅靠代码回滚解决。

手工回滚由有 sudo 的运维账号执行：先确认目标目录来自本应用的 releases、核对该版本数据库兼容性与备份，再停止服务、切换 current 并启动检查。不要直接修改 release.json 冒充另一提交，也不要把任何发布包解压到 shared/data。

服务器原有 Dachuang 的 `/opt/video-monitor`、容器、镜像和证书保持原状态。本次未恢复 FactoryVision/Jenkins、未清理其他项目或更改原有 SSH 登录方式。

## 首次安装或维护发布器

在受控运维会话中上传本目录脚本，确认系统已安装 Java 21、Nginx、Certbot 及 Python3，然后以 root 运行 provision.sh。该脚本只设置 Pandora 用户/目录/systemd/sudoers/站点，不覆盖已有数据库与环境文件；已有 HTTPS 站点配置也保留。专用公钥通过运维会话配置 authorized_keys，私钥加密存入 GitHub Secrets。不要把 `sshkey.md` 或私钥上传到 Git。

## 验证

```bash
python3 -m unittest discover -s deploy/tests -v
python3 deploy/package-release.py --sha "$(git rev-parse HEAD)" --run-number 0 --output /tmp/pandora.tgz
```

Windows 可运行 8 项归档/缓存校验测试，3 项真实软链接/回滚测试只在 Linux 运行。云端和 Actions 均须完整通过 11 项。安装、域名和真实 CI 发布结果见本目录 VALIDATION.md。
