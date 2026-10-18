# 员工账号拼音化与统一密码：交给组长执行

本次只提供离线维护工具，**尚未修改云端账号或密码**。适用公司登录标识为 `pandora`，当前公司名称为 6ix。操作前组长须核对实际数据库配置与映射表。

需一起交付：

- `RenameEmployeeAccounts.java`：Java 21+ 源码启动，无需编译或发布新后端。
- `employee-login-pinyin.tsv`：31 位现有员工的旧账号、新拼音账号和姓名。

保留管理员 `admin` 的账号、密码和会话；其他员工保留原 user ID、资料、职位、组织、角色、任务、日报，只改登录名、密码哈希及版本，并撤销其旧会话。既有拼音账号保留原名并参与密码统一。`李舟` 使用 `lizhou`；两条 `李舟（手机联调）` 分别使用 `lizhou-shoujiliantiao-1`、`lizhou-shoujiliantiao-2`。组长可在执行前修改映射表中的新账号，不能重复。

## 云端操作顺序

以下命令由有服务器维护权限的组长执行。受限 `pandora-deploy` 发布账号不能执行这些命令。不要运行在未停止的数据库上，不要创建第二个共享文件数据库实例。

1. 在受控运维会话上传两个文件到 `/opt/Pandora/maintenance/employee-accounts/`。目录由 root 所有、组为 `pandora-app`、权限 750，两个文件权限 640，确保 `pandora-app` 可读。映射不包含密码。
2. 核对 `/opt/Pandora/config/backend.env` 中的数据源 URL、用户名、密码。下面数据文件路径按当前部署文档给出，如配置不同必须先调整，不能试开其他文件。
3. 停止后端并备份当前数据文件：

```bash
sudo -i
systemctl stop pandora-backend
umask 077
install -d -m 700 /opt/Pandora/backups
cp -p /opt/Pandora/shared/data/pandora.mv.db "/opt/Pandora/backups/pandora-before-account-rename-$(date +%Y%m%d-%H%M%S).mv.db"
```

4. 在当前 shell 中按实际配置输入数据库信息。统一员工密码按本次沟通的值输入，隐藏输入不会进入命令历史；脚本不内置密码，也不打印密码或哈希：

```bash
read -r -p 'PANDORA_DB_URL（按 backend.env）: ' PANDORA_DB_URL
read -r -p 'PANDORA_DB_USER（按 backend.env）: ' PANDORA_DB_USER
read -r -s -p 'PANDORA_DB_PASSWORD（按 backend.env，空密码直接回车）: ' PANDORA_DB_PASSWORD
printf '\n'
export PANDORA_DB_URL PANDORA_DB_USER PANDORA_DB_PASSWORD
```

5. **先预览**。本命令只校验、显示映射并回滚，不改账号、密码或会话：

```bash
sudo -u pandora-app --preserve-env=PANDORA_DB_URL,PANDORA_DB_USER,PANDORA_DB_PASSWORD \
  java --class-path '/opt/Pandora/current/lib/*' \
  /opt/Pandora/maintenance/employee-accounts/RenameEmployeeAccounts.java \
  pandora /opt/Pandora/maintenance/employee-accounts/employee-login-pinyin.tsv
```

预览应显示 `employees=31` 和 `PREVIEW ONLY`。旧账号/姓名不符、目标账号已被其他人占用、映射包含管理员、缺少数据库或文件被占用时都会失败，不得忽略错误继续操作。

6. 核对 31 行后输入统一密码并执行一次：

```bash
read -r -s -p '员工统一密码（本次约定值）: ' PANDORA_RESET_PASSWORD
printf '\n'
export PANDORA_RESET_PASSWORD
sudo -u pandora-app --preserve-env=PANDORA_DB_URL,PANDORA_DB_USER,PANDORA_DB_PASSWORD,PANDORA_RESET_PASSWORD \
  java --class-path '/opt/Pandora/current/lib/*' \
  /opt/Pandora/maintenance/employee-accounts/RenameEmployeeAccounts.java \
  pandora /opt/Pandora/maintenance/employee-accounts/employee-login-pinyin.tsv --apply
unset PANDORA_RESET_PASSWORD PANDORA_DB_PASSWORD PANDORA_DB_USER PANDORA_DB_URL
systemctl start pandora-backend
curl -fsS http://127.0.0.1:18080/health
```

只在看到 `COMMITTED` 后才认为成功。SQL 中途出错会整体回滚，包括临时账号名、密码和会话变更。预览与执行应在同一个停服窗口内完成。不要在成功后重复运行旧映射；已改名账号会导致旧账号校验失败。

7. 刷新 Web“人员与角色”，用管理员核对新账号；用员工端检查 `shenan` 等新账号和约定密码可登录、旧登录名不可用，既有任务和日报仍关联原人员。员工不使用 Web 管理员入口。

若执行失败，保留错误结果及备份，先确认事务已回滚，再启动原后端；如需从备份恢复，只能停服后由运维显式恢复并核对时间点，不能覆盖仍在使用的数据库。若缺少 H2/BCrypt 类，检查当前部署的 `lib/*` 和 Java 版本，不要改用其他来源的程序或绕过校验。

## 本机隔离验证

用包含 H2、spring-security-crypto、commons-logging 的 classpath 执行：

```bash
java --class-path '<上述依赖 JAR 的 classpath>' \
  backend/scripts/tests/EmployeeAccountMaintenanceTests.java \
  backend/scripts/RenameEmployeeAccounts.java
```

测试仅创建系统临时目录下的新 H2 文件，覆盖预览不写入、成功改名/BCrypt/会话撤销、ID/业务引用保留、重复执行拒绝、冲突、管理员保护、姓名变更、部分失败整体回滚、重复映射、缺失数据库拒绝。Windows classpath 分隔符为 `;`，Linux 为 `:`。
