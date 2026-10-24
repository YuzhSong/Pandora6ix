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
# Rebase and integration review — 2026-10-18

PR 20 was rebased from `2138c6a520be73eecf2799f975b7a200cab2979b` onto latest dev `11c96fe0f5e9b0005b0d9d089e8b8460e2884310`. Resolved conflicts in backend README, Web feedback and AdminEditor by retaining administrator login-name editing from PR 19 alongside PR 20's partial legacy-profile editing, account handoff and new error messages. Updated the archived login-name proposal link.

The automatically merged real-backend integration script still attempted password recovery using the pre-rename login `staff`. It now uses the current account login name, verifying rename followed by password reset in the same run.

| Check | Result |
| --- | --- |
| Java 21 backend package | 80 tests passed, no failures/errors/skips |
| Web unit tests / lint / production build | 88 tests passed; lint has the two existing App.tsx effect warnings; TypeScript/Vite build passed |
| OpenAPI / OpenSpec / diff | 51 operations verified; all three PR 20 changes passed strict validation; no whitespace errors |
| Spring + file-H2 + Vite + HTTP repository | Passed creation/login, profile preservation and single-field legacy supplementation, role edits, rename, organization conversion/deletion, password reset/session revocation and CSV creation |
| Offline maintenance test | Disposable H2 preview, commit, independent BCrypt salts, identity/reference preservation, collision/admin guards, rollback, duplicate mapping and missing-file refusal passed |

Rendered target flow: local ADMIN login → edit existing employee login name → retain duplicate-name draft → save/reload on desktop and narrow screen → reset the renamed employee password → receive the current account handoff and invalidate the employee's old session. Browser plugin not available; regular Playwright 1.62.1 used the existing Chromium revision 1223 without installing project dependencies. At `http://127.0.0.1:53498`, desktop 1365×900 and narrow 390×844 both passed page identity, nonblank page, no framework overlay, no unexpected console/page errors, usable editor, save/reload and historical empty-profile editing. Reset after rename succeeded; the replacement password was absent from localStorage. Screenshots were captured and inspected for the narrow editor and desktop reset handoff.

Evidence and executable QA script are outside Git at `%LOCALAPPDATA%/Pandora/pr20-rebase-qa/`: `verification.json`, `duplicate-name.png`, `renamed-employee.png`, `mobile-editor.png`, `mobile-saved.png`, `reset-after-rename.png`, `verify-rebase.mjs`. Local test processes were stopped. No offline maintenance script or credential update was run against the cloud database.
