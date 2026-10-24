## ADDED Requirements


### Requirement: 企业登录
Android SHALL 提供企业域/登录名/密码登录页，调用 `POST /auth/login` 保存 accessToken，登录成功后进入导图；凭据不写入日志或版本库。

#### Scenario: 登录成功
- **WHEN** 输入合法业务账号
- **THEN** 保存会话并进入导图，"我的"展示 `GET /me` 的真实姓名/部门/职位/组织/时区/角色

#### Scenario: 登录失败
- **WHEN** 密码错误或账号不存在
- **THEN** 显示服务端错误信息，停留在登录页

### Requirement: 改密与退出
"我的" SHALL 调用 `POST /me/password`（newPassword 至少 10 字符、最多 72 个 UTF-8 字节）与 `POST /auth/logout`；改密成功 204 后当前会话失效，须重新登录。

#### Scenario: 改密成功
- **WHEN** 提交合法的当前密码与新密码
- **THEN** 提示成功并回到登录页重新认证

#### Scenario: 改密校验
- **WHEN** 新密码少于 10 字符
- **THEN** 客户端先行提示，不发起请求；服务端 422 时展示字段错误

### Requirement: 能力驱动界面
界面可用操作 SHALL 依据 `/me` 的 roles/capabilities（createPersonalTasks/createDelegatedTasks/reviewTasks）区分，替换原 DemoRole 一刀切禁用；能力不代表绕过服务端校验。

#### Scenario: 普通员工
- **WHEN** 员工登录
- **THEN** 可见本人个人任务创建入口，无派发与审核入口
