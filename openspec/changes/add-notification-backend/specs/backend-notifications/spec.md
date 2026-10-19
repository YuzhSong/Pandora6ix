## ADDED Requirements

### Requirement: 通知记录与可见性
后端 SHALL 以 `notification` 表保存每位用户的提醒记录，字段包含企业、接收人、类型（`NEW_TASK` / `DEADLINE` / `STATUS`）、关联任务与执行关系、标题、正文、去重键、创建时间与已读时间。通知 SHALL 只能被其接收人本人读取；其他用户与其他企业 SHALL 收到一致的 404，管理员权限本身 SHALL NOT 授予他人通知的读取权。重复生成同一事件 SHALL 由 `(enterprise_id, user_id, dedupe_key)` 唯一约束拦截，不产生第二条记录。

#### Scenario: 他人读取通知
- **WHEN** 非接收人用户或另一企业账号请求该通知的已读接口
- **THEN** 返回 404，该通知保持未读

#### Scenario: 重复事件
- **WHEN** 同一任务对同一用户重复触发同一类型同一档位的提醒
- **THEN** 只保留一条通知，后续触发被唯一约束静默拦截，不报错

### Requirement: 审批生效产生新任务通知
任务审批 APPROVE 原子激活执行关系时，后端 SHALL 在同一事务内为每位被激活的接收人生成一条 `NEW_TASK` 通知；个人任务只为其本人生成。REJECT SHALL NOT 生成新任务通知。任务创建但未批准 SHALL NOT 生成通知。

#### Scenario: 派发任务批准
- **WHEN** 直接上级批准一个多人派发任务
- **THEN** 每位接收人各得到一条新任务通知，内容为任务名称与其生效事实

#### Scenario: 驳回不通知
- **WHEN** 审批结果为 REJECT
- **THEN** 不生成任何新任务通知，执行关系保持未激活

### Requirement: 执行状态变化产生状态通知
验收决定（ACCEPT / REJECT）、个人任务完成、进度更新等改变执行状态或审批状态的写操作 SHALL 为相关人生成 `STATUS` 通知：派发任务通知任务创建人与该执行关系的接收人，个人任务通知本人。通知 SHALL 使用服务端状态事实，不合成状态文案。

#### Scenario: 发起人验收接收人完成
- **WHEN** 发起人 ACCEPT 某接收人的完成申请
- **THEN** 该接收人与其发起人各得到一条状态通知，反映该接收人已完成

#### Scenario: 进度更新
- **WHEN** 接收人记录新的进度
- **THEN** 相关人收到状态通知，重复同样进度不产生重复记录

### Requirement: 截止时间扫描与去重
后端 SHALL 按企业业务时区定期扫描：任务已批准（APPROVED）、执行关系为 ACTIVE 或 AWAITING_ACCEPTANCE、且任务截止时刻落在 `(业务现在, 业务现在 + 24 小时]` 窗口内时，为每个未完成接收人生成 `DEADLINE` 通知。服务端 SHALL 按任务与接收人只生成一次，并返回 dueAt，供客户端按本地提前量决定展示时机。未批准或已驳回的任务、已完成或已取消的执行关系 SHALL NOT 产生截止提醒；截止时刻已过去的 SHALL NOT 补发。

#### Scenario: 未批准任务不泄露
- **WHEN** 尚未批准的派发任务进入截止窗口
- **THEN** 接收人不收到该任务的通知，任务名称也不通过通知列表泄露

#### Scenario: 临近截止
- **WHEN** 某未完成任务在 24 小时内截止
- **THEN** 其未完成接收人各得到一条截止提醒，再次扫描不重复生成

#### Scenario: 已完成任务不再提醒
- **WHEN** 某接收人的执行关系已 COMPLETED 或任务已 REJECTED
- **THEN** 即使截止时刻进入窗口也不生成截止提醒（US29）

### Requirement: 通知列表、未读数与已读
后端 SHALL 提供 `GET /notifications`（游标分页，按创建时间倒序，游标绑定操作者、企业与 scope）、`GET /notifications/unread-count`（本人未读数）与 `POST /notifications/{id}/read`（幂等标记已读）。已读接口对已读通知重复调用 SHALL 返回同一结果且不报错；未读数为未读通知条数，已读后递减。

#### Scenario: 分页与游标
- **WHEN** 用户用上一页返回的 nextCursor 请求下一页
- **THEN** 返回后续通知且不与上一页重复；游标与其查询条件不匹配时返回可读的无效游标错误

#### Scenario: 标记已读
- **WHEN** 用户标记一条通知为已读并再次提交同一请求
- **THEN** 通知保持已读、未读数不变，接口不报错
