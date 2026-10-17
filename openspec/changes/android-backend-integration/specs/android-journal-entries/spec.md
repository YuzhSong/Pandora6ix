## ADDED Requirements


### Requirement: 日报一篇多条
日报 SHALL 调用 `GET/POST /work-logs`、`GET/PATCH /work-logs/{id}`：一篇包含多条工作条目，新条目携带 clientEntryKey，已有条目携带 entryId，upsertEntries 不删除遗漏项；编辑携带 expectedVersion，409 版本冲突时重新读取并不自动覆盖。

#### Scenario: 当日补充
- **WHEN** 当日已有日报再次发布
- **THEN** 同日重复创建返回 409 时按 existingJournalId 进入同一篇继续补充，不新增第二篇

#### Scenario: 版本冲突
- **WHEN** 提交的 expectedVersion 已过期
- **THEN** 提示冲突并重新读取最新内容，不覆盖他人修改

### Requirement: 个人重要事项 entryId 选择
个人重要事项 SHALL 从 `GET /me/important-entry-candidates`（D～D-9 已发布工作条目）勾选 entryIds，经 `PUT /me/important-entries` 保存 expectedVersion；最多十条，空集合是合法的主动清空，requiresSelection/hasManualSelection 区分未选择与已清空。

#### Scenario: 保存空选择
- **WHEN** 用户清空全部选择并保存
- **THEN** 保存空集合并持久化，导图相应更新，不回退为默认全选

#### Scenario: 超出上限
- **WHEN** 勾选超过十条
- **THEN** 客户端阻止提交并提示上限
