# 人员离职与岗位调整

本功能只管理人员和账号，不读取或改动任务、日报、审批等业务内容。V6 须在 V5 后部署；本轮仅本地验证，云端部署走 dev 审批。

- `POST /api/v1/admin/users/{id}/departure`：`expectedVersion`、`departureDate`（YYYY-MM-DD）。即时办理，实际日期不能晚于公司时区的今天；不支持预约。记录 DEPARTED，账号 DISABLED，清除直属上级，撤销全部会话和可交付初始密码。保留 ID、登录名、认证哈希、资料、历史组织和角色。当前登录人不能给自己办理，仍有在职下属时先调整人员关系。
- `POST /api/v1/admin/users/{id}/job-change`：`expectedVersion`、`jobTitle`、`orgUnitId`（可空）、`supervisorUserId`（可空）、`roles`。即时调整，校验组织、角色、上下级与现有下属，保留账号及个人资料，撤销旧会话，原密码可重新登录。
- `GET /api/v1/admin/users/{id}/personnel-changes`：返回 `items`，含操作人、时间、生效日期及岗位/在职状态前后值。仅当前公司管理员；不含密码、身份证、电话或业务信息。
- `POST /api/v1/admin/users/{id}/manager-replacement`：传原负责人 `expectedVersion`、`successorUserId`、`successorExpectedVersion`、`successorJobTitle`、`formerJobTitle`，返回 `items`（全部受影响人员的新版本）。一次事务任命接任人、转移在职直属下属、将原负责人改为普通员工并向接任人汇报，保留原 ADMIN 权限。账户、密码、个人资料和业务数据不变。接任人须启用且在职，可为无管理职责的普通员工或本组织同级负责人；当前登录管理员不能作为原负责人或接任人。校验任一版本失败时全部回滚。所有受影响人员会话撤销，逐人写人员履历。

组织仍有人员或下级组织时，不能取消最后一名启用在职负责人；有部门/团队的公司须保留创始人。普通 PATCH、岗位调整、停用和离职均执行此保护，不能绕过。“更换负责人”通过同一事务避免中间空缺。空团队没有其他在职人员时可办理其负责人离职。

人员查询新增 `employmentStatus: IN_SERVICE | DEPARTED` 与 `departureDate`。历史人员（含临时停用账号）迁移为 IN_SERVICE。已离职人员禁止通过普通编辑、改角色、岗位调整或重置密码重新启用。返聘与未来日期调度不在本轮范围。

人员变动记录是此功能的事务性履历，不代表已决定通用系统操作日志采用数据库还是文件 log；该方案仍待甲方确认。岗位调整不涉及业务交接。数据库回退须保留离职账号停用状态和历史数据。
