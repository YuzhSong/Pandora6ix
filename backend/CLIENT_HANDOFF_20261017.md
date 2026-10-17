# Web / Android 联调交接（2026-10-17）

dev 已合并 Web #12 与 Android #13，云端运行 a493dfe（Actions run 6 成功）。backend-syz 已快进到该 dev 后继续实施 `complete-client-administration-backend`。以下新契约在 backend-syz，需经新 PR 非作者审查后合入 dev 才自动上线；不能把原 #13 的 24 小时云端版本当作最新日期规则。

## 管理员人员接口

沿用 POST /api/v1/admin/users，一次事务保存账号、资料与角色；不增加注册入口。保留 loginName/password/displayName/orgUnitId/supervisorUserId/jobTitle/roles，新增：

| 字段 | 契约 |
| --- | --- |
| gender | 新建必填 MALE 或 FEMALE；旧资料 null |
| idNumber | string，新建必填；15 位数字或 17 位数字加数字/大写 X；仅基本格式校验 |
| phoneNumber | string，新建必填；6～24 字符，可用前置 +、数字、空格、括号和短横线，至少六位数字 |

GET /admin/users、创建及 PATCH 响应始终含三键。PATCH 可传三字段更新；省略或 null 都保留原值，避免只改角色清空资料。空白或非法值返回 422；版本冲突与同公司重复登录名返回 409，失败不留下半个账号。Flyway V3 为历史行新增可空列，不伪造历史资料。

这些字段仅在受授权的管理员人员响应中返回；登录 /me、下属及业务候选投影不带原始证件或电话。业务日志不复制证件、密码或令牌。

## 组织类型与权限

PATCH /admin/org-units/{id} 继续传 expectedVersion/name/kind/parentId。部门直属公司，团队直属部门；叶节点可部门↔团队，保留 ID。根节点改类型、带子组织、循环、跨公司或负责人角色不匹配均拒绝，整笔回滚。角色调整与组织转换共享企业锁。

公司管理员只能维护所属公司，不能凭 ADMIN 会话调用初始化开通其他公司。初始化需要部署端 bootstrap secret；云端现有 bootstrap 已关闭。纯 ADMIN 的标签读写、任务、日报、统计均返回 403；公司事项维护保留。业务创始人和部门负责人可维护同公司标签。

## 日报日期规则（用户最终确认）

- 公司时区今天：可创建，并在今天内修改、增补或删除；午夜后立即只读。
- 昨天、前天：尚未填写时各允许提交一篇；补交成功后不能再修改、增补或删除。
- 其他日期：新建返回 422 JOURNAL_DATE_OUT_OF_RANGE。历史查询和 D～D-9 的重要事项候选保留。
- 已有同日日报仍返回 409 DAILY_JOURNAL_EXISTS 与 existingJournalId。补交冲突不得自动 PATCH。
- 非今日 PATCH/DELETE 返回 409 JOURNAL_EDIT_WINDOW_EXPIRED；保留错误码以兼容 Android，提示已改为日期语义。
- 日报读模型增加 businessToday 和 allowedActions（作者当日 EDIT/DELETE，否则空集）。条目删除走 deleteEntryIds，原条目修改保留 entryId；删除已选重要条目会原子移除选择引用并递增选择版本，不自动补选。

Android 同步了公司时区日期、只读入口、补交提示、过期草稿过滤与午夜测试。需要成员安装新 Android 构建；服务器不信任设备日期。

## 接收人选择与 test 账号

GET /tasks/recipient-candidates 保留原字段并增加可空 departmentId/departmentName，团队成员返回其父部门。Android 按稳定部门 ID 分组，遍历 nextCursor，避免同名部门合并或只显示首批 50 人。

用户确认后已通过云端管理员接口把现有 test 账号设为 DEPARTMENT_MANAGER（等级 1），归属产品研发部，直属上级为创始人，保留原账号与密码。实际登录确认 createDelegatedTasks/manage 为 true，候选共 16 人；没有为了验证创建测试任务。App 重新登录以更新 /me 能力。

## 本轮暂缓

系统操作日志仍待甲方确认文件 log 或数据库、保留与查询方案；本轮没有新建审计表、采集器或查询接口。任务和日报增删改的统一采集要求已保留，不宣称现有请求日志等同系统操作日志。头像保存接口也未在本轮扩充。
