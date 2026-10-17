# 后端验证记录

2026-10-17：最新增量验证、PR 合并与云端边界见 [本轮验证](../openspec/changes/complete-client-administration-backend/validation.md)，接口交接见 [CLIENT_HANDOFF_20261017.md](CLIENT_HANDOFF_20261017.md)。后端完整包 58 项及另增公司时区边界 1 项通过，Android 18 项、Web 66 项通过；112 次真实 HTTP 与 Web 仓库真接口验证通过。云端仅已部署原 #12/#13，新的公司日期规则需本次 PR 合并后发布。

## 2026-10-15：Android 十模块与真实任务闭环

分支 `backend-syz` 已将 `dev ec288d4` 合入，本地合并提交 `2334626`。本轮业务修改随后按用户明确确认随云端部署批次同步 dev，不单独创建 PR，云端发布记录见 [部署说明](../deploy/README.md)。依据当前 Android-lj/Android-swj（均已是该 dev 的祖先）、用户十模块清单及本轮明确确认的直属上级审批实施；现行契约见 [ANDROID_API_V0.2.md](ANDROID_API_V0.2.md)，OpenSpec 记录在 complete-android-backend-workflows 变更（合并后按流程归档）。

| 检查 | 实际结果 |
|---|---|
| JDK 21 / `mvnw.cmd -q package` | 退出码 0；独立后端打包成功 |
| Maven 全量自动测试 | 50 项通过，0 失败、0 错误、0 跳过：业务集成 41、错误契约 6、健康启动 1、任务枚举 2 |
| `python scripts/android-smoke.py` | 退出码 0；97 次真实 HTTP 请求，十模块通过；三个实际后端进程依次启动，使用独立随机文件 H2 与随机凭据，无 SQL 批准夹具 |
| V1→V2 升级与重启 | 先以 Flyway target=1 建库并经 API 写入企业/人员/标签/日报/待审任务；重启升级 V2 后真实批准和验收；再次重启，审核审计、完成事实、会话、稳定 entryId 与手动空集保持 |
| OpenAPI 导出与检查 | 44 个业务操作、33 个路径；三条审核接口及命令/读模型引用有效；导出文件与源码一致 |
| OpenSpec | `openspec validate complete-android-backend-workflows --strict` 通过；`openspec validate --all --strict` 共 21 项通过 |
| 范围/格式 | `git diff --check` 通过；相对合并后 HEAD 的 Android/Web 源码无差异。仅修改 backend 和本 OpenSpec change，未触及用户已有未跟踪文件 |

新增自动测试验证：

- `/me` 中员工可创建个人任务，管理者可派发/审核；ADMIN-only 不获得业务能力。
- 公开 API 完成个人任务申请→上级批准→100% 进度仍 ACTIVE→本人 SELF 完成，版本使用真实审批返回值。
- 多人派发批准→接收人申请→创建者逐人验收；上级审核人不自动成为完成验收人；其他接收人保持 ACTIVE。
- 驳回保存意见与审计，不激活任何接收人；旧 attempt 快照不可变、当前队列只取最新有效版本；新编辑快照与首次提交字段统一。
- 仅指定审核人/提交者读取；他人、ADMIN-only、跨企业读写均不可越权；非法决定/缺版本返回 422，旧版本返回 409。
- 当前上级变化、接收人停用时，审批失败且无部分更新；scope/actor 绑定的完整审核分页。
- 审核幂等重放、同键不同命令冲突；并发 APPROVE/REJECT 只有一个成功；待审编辑与审批并发不能批准已经被替换的内容。

真实 HTTP 另覆盖：全部四面板与单条全文、真实接收人搜索/ID、多标签 AND、日周月跨页查询及企业时区、日报发表/同日冲突/增补/编辑版本冲突、12 条候选完整分页、最多十条与空选择、进度与完成分离、拒绝验收后再申请、本人统计、改密只撤销当前会话、退出后 401。

本次运行证据保存在忽略目录：`target/android-package.log`、`target/surefire-reports/`、`target/android-http-smoke.log`、`target/android-smoke/8186163c-5964-45b9-92e8-e7aefb0f958e/result.json` 及同目录三个服务日志；不含业务凭据输出。smoke 结束已停止自身进程。未宣称 Android 应用/真机验收、MySQL 部署或固定规模性能验收通过。Flyway 对现有 H2 版本仍有兼容提示，但实际两版迁移和重启流程通过。

## 2026-10-13 历史基线

以下原始记录只描述当时的 39 项测试与 Reserved 范围；审批实现状态以本页上方和当前 V0.2 契约为准。

2026-10-13；`backend-syz` 基于 `dev a8adf5d`。实现范围与未决边界见 [README](README.md)，接口契约见 [OpenAPI](openapi.json)。

| 检查 | 实际结果 |
|---|---|
| JDK 21 / `mvnw.cmd package` | 退出码 0，独立打包成功 |
| Maven 自动测试 | 39 项通过，0 失败、0 错误、0 跳过：业务集成 30、基础错误契约 6、健康启动 1、任务枚举 2 |
| `python scripts/generate-openapi.py --check` | 41 个业务操作、30 个路径；命令/路由与导出文件一致，所有本地组件引用可解析 |
| `scripts/smoke.ps1` | 退出码 0；真实 jar、独立随机 H2 文件库、两次进程启动；确认迁移/初始化/登录、日报幂等、重启持久化、entryId 稳定、手动空集、导图与退出撤销 |
| `openspec validate implement-backend-confirmed-apis --strict` | 通过 |
| `git diff --check` | 通过 |
| 作用域 | 修改仅在 backend 与本次 OpenSpec change；Web/Android 和原始需求设计文档未修改 |

业务集成测试覆盖：

- 受控企业初始化及幂等，不回传秘密；错误凭据/缺失身份；本人资料。
- 当前密码校验、只撤销当前改密会话、退出幂等、会话过期、停用撤销及重新启用旧令牌仍无效。
- 仅 ADMIN 不获得业务权限；请求不能伪造 creatorId；跨企业读取与目录引用拒绝；组织/直属上级循环、角色限制和版本冲突。
- 数据库 seek 分页：人员、日报、重要条目、公司事项、六周历史日历；游标条件绑定；标签 AND、主标签与颜色。
- 管理范围内直属/跨级候选；无上级政策拒绝；混入范围外/停用接收人时整笔拒绝；待审快照保留，编辑重查当前范围与直属审核人。
- 未审批不派发/不执行；分钟格式和未决端点；进度追加、100% 不自动完成；个人 SELF 与派发 CREATOR_ACCEPTANCE 分离；逐人验收/拒绝后再申请；幂等不重复完成。
- 并发同键重放及同日并发发表；每天唯一日报返回 existingJournalId；修改/新增保留 ID 和遗漏条目；无权代改；非法补丁事务回滚；空条目返回字段错误。
- D～D-9 候选、十条上限、默认选择/超过十条要求手选、手动清空持久化、无效作者选择整笔回滚、已有过期引用保持而新过期候选被拒绝。
- 企业时区未配置/非法配置；事项权限/全文/版本；导图四面板及本人完成事实统计。

审批路由和消息路由为 Reserved。测试准备 APPROVED 状态时明确操作测试数据库夹具，不存在生产测试审批接口；新申请在生产 API 中保持 PENDING。

以上结果不包含固定企业规模下的性能验收、MySQL 方言/部署、真实 Web/Android 业务联调、Reserved 服务或 Q-01～09 规则确认。H2/Flyway 的运行输出有新版本兼容提示，当前迁移、事务测试和文件重启检查实际通过；没有据此宣称生产数据库认证完成。
