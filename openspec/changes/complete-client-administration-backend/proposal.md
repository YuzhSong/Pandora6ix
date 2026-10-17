## Why

Web PR #12 已开放组织类型编辑与人员资料表单，但后端拒绝类型转换且缺少人员字段持久化，阻塞真实保存。用户已确认单公司管理边界，并要求接续 Android PR #13 后端联调与云端生效验证。

## What Changes

- 支持部门与团队安全转换，事务校验根节点、上下级、子组织、负责人角色、版本及企业隔离。
- **BREAKING** 管理员 POST /admin/users 新增必填 gender、idNumber、phoneNumber，账号/资料/角色原子创建；历史资料允许空，角色编辑省略或 null 不清空已有资料。
- 仅管理员人员接口返回证件与联系方式；通用本人和业务用户投影不泄露新增字段。
- **BREAKING** 公司管理员不能经企业初始化接口开通其他公司；初始化仅接受部署端 bootstrap 授权。纯 ADMIN 不获得标签、任务、日报或统计权限；Android 原有业务角色保持按服务端授权。
- **BREAKING** 日报按公司日期校验：仅今日可改删，昨天/前天各可补交一篇，提交后只读；拒绝三日外新建。
- 接收人候选增加稳定部门 ID/名称供 Android 按部门分组，保留候选范围校验与分页。
- 记录接口契约、迁移与回归证据；系统操作日志存储、查询、头像接口与新服务均不在本轮实施。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `backend-identity-directory`: 组织类型转换、必要人员资料、受控初始化与敏感字段投影。
- `backend-journals-dashboard`: 公司日期改删窗口、三日补交约束与服务端可操作动作。
- `android-journal-entries`: 日期选择、补交只读、公司时区和提示同步。
- `backend-task-workflow`: 纯管理员标签权限收敛与接收人部门元数据。

## Impact

backend Java/JDBC/Flyway V3、OpenAPI、测试与联调脚本；Android 已有候选分组读取部门元数据。web 独立构建保持可用。先合并已审核的 #12/#13 到 dev，再同步 backend-syz 开发本变更；新后端提交另提 PR，未审核前不自行合入 dev。云端账号操作只针对已核验的 test 账号。
