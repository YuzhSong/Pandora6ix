# Validation — 2026-10-18

## Git baseline

- 使用系统现有本机代理执行一次性 git fetch（不修改全局配置），origin/dev = fff4e6e5929688c18f0c8e77c302c58d0040ff1f。
- 本地 Web-cy 从 e18b4e5 快进 14 个提交到最新 dev，无合并冲突。保留三份未跟踪业务阅读草稿。
- backend/、android/ 变化全部来自上游；本次实现未编辑这两个目录。

## Checks

- 组织筛选加宽复验：真实云端资料中 Android 开发组等选项为单行，选项高度 38px，菜单宽度受视口约束；构建通过，截图 web/.integration/organization-filter-wide-20261018.png。

- 受版本控制的 Web 测试：68/68 通过（不混入本机未挂载业务阅读草稿的测试）。
- `npm run build`：通过。
- 受版本控制的源码、测试和脚本 oxlint：无错误，App.tsx 保留两项已有 effect 警告。
- `openspec validate integrate-web-personnel-backend --strict`：通过。
- `git diff --check`：通过，仅现有换行转换提示。

## Actual backend integration

JDK 23 构建未修改的后端 4809241 副本，输出在忽略目录 web/.integration/backend-4809241。`JAVA_HOME=D:/Java npm run test:integration` 启动本机 Spring、隔离全新 H2 与随机端口 Vite，验证通过：

- 历史管理员只补电话号码，重新读取仍保留性别/身份证为 null。
- 新员工一次创建后资料持久化，真实账号登录成功。
- 员工访问管理接口返回 403，非管理投影不带身份证号/电话。
- 普通编辑、角色修改后个人资料保留，版本冲突不覆盖。
- 团队 → 部门 → 团队保存并重新加载，同一 ID 和正确上级保留。
- 标签管理不可写、事项幂等/版本、改密及退出撤销通过。

测试结束自动停止进程；没有云端写入测试人员/组织，没有手动部署。真实系统日志存储、采集和头像保存接口仍未实现，不能算本轮已完成事项。

## Account delivery, bulk creation and password recovery follow-up

- Web tests now pass 92/92; production build passes. Lint exits 0 with the same four existing warnings (two App, two unrelated untracked retired BusinessPages draft); no new warnings.
- Isolated integration was rerun with the freshly packaged working backend via PANDORA_INTEGRATION_JAR. Actual Spring/H2/Vite HTTP requests pass employee reset, new-password login, old-password rejection, old-session 401, preserved profile and CSV sequential creation of two employees. Temporary processes stop after verification.
- Unit coverage includes CSV quoting/BOM/newlines, duplicate/current logins, invalid fields/password bytes, row/size/header bounds, organization ambiguity, supervisor authority, partial failure without retry, stop-after-current behavior, wrong success response and password reset command/response/version checks.
- Browser on the actual logged-in Web verified CSV two-row preflight without submitting cloud creation, employee reset form without changing cloud credentials, complete identity number in admin details, known shared password registration, and a 31/31 account handoff list with successful one-click copy. Screenshot: web/.integration/bulk-account-handoff-20261018.png. A current-login credential TXT and maintenance ZIP were prepared only in ignored .integration.
- User separately authorized filling existing cloud profiles with arbitrary placeholders and requested no test labels. UI saved all 32 missing gender/ID/phone profiles, preserving other fields. Fresh reads visibly confirm saved sample profiles. These are not verified real personal details. No cloud login names or passwords were changed.
- User confirmed that the team lead will execute the offline 31-employee pinyin mapping/password maintenance; tool and instructions are tested and delivered separately. Shared handoff password is administrator-entered, never hardcoded into public source, backend API output or browser persistent storage. New/reset credentials remain in current-page React state and clear on logout/reload. The current database does not retain recoverable initial passwords.
- Both new OpenSpec changes validate strictly, OpenAPI generator --check verifies 48 operations, and git diff --check passes. No commit, push, deployment or cloud credential reset has been performed.

- 按最新界面要求，移除统一初始密码登记入口与弹窗，并精简批量交付说明文字；保留新建/重置成功后的交付记录。Web 生产构建通过。
