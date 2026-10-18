# 私有数据库网页查看

用户授权的 Pandora 运维入口。H2 Console 与原后端同一 JVM 运行，使用同一文件数据库；仅监听服务器 `127.0.0.1:18082`。不注册到公共 Web/API，不启用 H2 TCP/AUTO_SERVER，不改变应用发布成品。

## 使用

本机桌面“Pandora数据库查看”启动专用 SSH 隧道并自动以只读账号打开浏览器。地址为 `http://127.0.0.1:18082/`，仅在配置了私钥的电脑建立隧道后可用。关闭时用桌面“关闭数据库查看”。隧道不随电脑开机自动启动。

桌面快捷方式使用系统自带的 `%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe`，参数为 `-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "本机私密目录\launch-db-console.ps1" -Action Start`（关闭入口用 `Stop`）。将三个 PowerShell 脚本以 UTF-8 BOM 保存到私密目录，兼容 Windows PowerShell 5.1。启动失败会弹窗提示，私密目录 `launcher.log` 保留操作时间、完成状态和脱敏错误位置；SSH 连接错误另见 `tunnel-error.log`。桌面入口不依赖 Codex 缓存目录中的 PowerShell。

左侧展开 **PANDORA_VIEW**，点击表生成 SELECT，可查看云端实时记录。PUBLIC 原表不可直接查询。

| 视图 | 内容 |
| --- | --- |
| APP_USER | 人员资料（不含密码哈希） |
| ORG_UNIT | 公司、部门和团队 |
| USER_ROLE / ROLE | 人员角色 |
| TASK / TASK_ASSIGNMENT | 任务及接收人执行关系 |
| TASK_REVIEW | 任务审批 |
| WORK_LOG / WORK_LOG_ENTRY | 日报及条目 |
| COMPANY_IMPORTANT_ITEM | 公司重要事项 |
| PERSONAL_IMPORTANT_SELECTION | 个人重要事项选择 |

例如：`SELECT * FROM PANDORA_VIEW.APP_USER LIMIT 50;`。只读权限由数据库授予，不能通过修改、删除或结构命令写入业务表；会话令牌、初始化记录、幂等缓存与密码哈希不提供。

本机 `%LOCALAPPDATA%\Pandora\db-console\访问说明.md` 含手动登录资料，`access.json` 含只读口令，`id_ed25519` 为专用私钥。目录 ACL 限当前用户、SYSTEM 和管理员访问；这些文件只留本机，不提交 Git 或转发。服务器凭据在 root-only `/opt/Pandora/config/backend.env` 和 `db-console.env`。

## 实现与启用

`PandoraDatabaseConsole` 调用原应用 main，待应用完成迁移后，以外部管理员凭据维护 `PANDORA_VIEW` 业务视图和非管理员 `PANDORA_VIEWER`，再启动官方 H2 WebServer。数据库登录 URL 只保留文件路径与 IFEXISTS/SCHEMA，不把应用的 WRITE_DELAY 等管理员级连接设置传给只读账号。随机的控制台管理密码保护 Console preferences/tools，普通查看账号不获得该密码。

可选启动器位于 `/opt/Pandora/tools/db-console/classes`；源码与配置脚本同目录留档。systemd drop-in `/etc/systemd/system/pandora-backend.service.d/40-db-console.conf` 加载独立环境文件及启动器。原 `/opt/Pandora/current/backend.jar` 和 `/lib` 不改写，应用发布标记仍代表实际业务版本。正常 dev 部署重启后入口自动恢复。

首次操作先与发布器共用 `/opt/Pandora/deploy.lock`，停服取得一致备份，再设置随机数据库管理员密码、读账号与视图，并更新私密环境。控制台同进程退出，因此既有发布器停服复制 H2 的备份约束仍成立。安装失败时撤销本次 drop-in、探测已提交的数据库密码并恢复应用；不自动用备份覆盖业务库。

`configure.py` 必须 root 执行，只接收**公钥文件路径**。先编译验证 Java 21 工具并将 class 和源码部署到上述 tools 目录，再运行：

```bash
sudo python3 /opt/Pandora/tools/db-console/configure.py /path/to/public-key.pub
```

专用 SSH 账号 `pandora-db-view` 只允许转发到 `127.0.0.1:18082`。`sshd_config.d/60-pandora-db-view.conf` 限定该用户的本地转发，禁止 shell、PTY、远程转发、代理和其他目的端口；已有部署 SSH 账号保持原用途。

## 本地验证

将 H2 jar 路径指向后端所用的确切版本（本次 2.4.240），Java 21 编译输出放到忽略目录：

```powershell
$h2jar = 'C:\Users\YOUR_USER\.m2\repository\com\h2database\h2\2.4.240\h2-2.4.240.jar'
javac -cp $h2jar -d backend/target/db-console-classes deploy/db-console/PandoraDatabaseConsole.java deploy/db-console/ConsoleIntegrationTest.java
java '-Dh2.bindAddress=127.0.0.1' -cp "backend/target/db-console-classes;$h2jar" com.pandora6ix.ops.ConsoleIntegrationTest
python deploy/db-console/check_runtime.py
```

第二项需要已构建的 backend jar 和 Web dist，用既有打包器构造真正的精简发布格式，在独立随机文件库中验证旧启动方式、凭据初始化、真实 HTTP 登录和两次新启动器重启。不会使用团队数据或账号。`verify_console.py` 可复用为浏览器协议验收，不打印口令或业务行。

## 运维与停用

```bash
sudo systemctl status pandora-backend --no-pager
sudo ss -lntp
sudo journalctl -u pandora-backend -n 40 --no-pager
```

只停网页入口：将外部 `db-console.env` 的 `PANDORA_CONSOLE_ENABLED` 设为 false 后重启后端。完全恢复原应用启动器：备份并移走**仅** `40-db-console.conf`，daemon-reload 后重启。保留当前正确的数据库连接口令；不能把旧空密码环境文件直接覆盖回来。移除用户/权限/视图时另行核对依赖并按明确范围操作，不删除业务库。

未来添加业务表时审核显式视图白名单；新增列会在启动时重新投影，哈希列仍排除。破坏性表/列迁移要同时处理这些视图依赖。关闭浏览器不会自动停止后台 SSH 隧道，桌面关闭入口仅终止其记录并核验过的 ssh.exe PID。
