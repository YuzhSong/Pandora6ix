## ADDED Requirements

### Requirement: Maintain customer profiles
系统 SHALL 按企业记录个人或企业客户、分类、联系方式、负责人和相关业务历史。

#### Scenario: Create and query a customer
- **WHEN** 企业管理员通过 Web 创建客户并为其链接业务
- **THEN** 管理员能通过搜索找到客户并查看相关业务历史

### Requirement: Record business and employee involvement
系统 SHALL 独立记录业务类型、客户、负责人、开始时间、完成状态、员工和职责。

#### Scenario: Employee sees assigned activity
- **WHEN** 管理员为业务添加员工及职责
- **THEN** 员工可在 Android「我参与的业务」看到该业务和职责，Web 管理员可查看参与情况

### Requirement: Track membership and entitlement use
系统 SHALL 保存会员类型及有效期、权益条目及逐次使用与服务完成情况。

#### Scenario: Track a delivered service
- **WHEN** 管理员登记会员权益的服务使用并标为完成
- **THEN** 管理员能查询使用日期和服务状态

### Requirement: Maintain protected employment contracts
系统 SHALL 保存员工劳动合同期限和附件并在管理页面提示临期或已到期合同。

#### Scenario: Contract expiration and attachment
- **WHEN** 管理员查看 30 天内到期合同
- **THEN** 显示临期提示，并可通过授权接口下载附件

### Requirement: Apply CRM data scopes on the server
系统 SHALL 根据认证用户所属企业、角色及记录关系约束列表、详情、关联写入和附件下载。CRM 写入 SHALL 记录最小必要审计字段。

#### Scenario: Unrelated or external account requests sensitive records
- **WHEN** 非授权员工或其他企业用户请求客户或合同详情和附件
- **THEN** 服务端拒绝并且列表不返回该记录

### Requirement: Isolate phase two execution
CRM SHALL 仅在二期配置加载迁移及端点，使用独立 H2、端口及附件目录，Android 演示包 SHALL 指向二期服务。

#### Scenario: Start phase one with default configuration
- **WHEN** 使用一期默认配置启动且连接测试库
- **THEN** 仅应用一期迁移且一期 API 保持可用
