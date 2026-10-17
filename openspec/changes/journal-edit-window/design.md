# Design: journal-edit-window

## 合并前审查修正

删除条目前原子清理 personal_important_selection 引用，并递增选择状态版本；保留手动空集，拒绝旧版本回写。patch/delete 先锁作者 app_user，与 saveSelection 同序串行化，避免选择与删除竞态。通过已选条目 PATCH、整篇 DELETE 和旧选择版本冲突回归。

## 窗口判定

- 服务端为权威：`work_log.published_at`（NOT NULL DEFAULT CURRENT_TIMESTAMP）为创建时刻，`now <= published_at + 24h` 允许 PATCH/DELETE；超窗抛 409 `JOURNAL_EDIT_WINDOW_EXPIRED`
- 客户端 UI 用同一规则预判（解析 publishedAt 为 Instant 与设备时间比较），仅用于隐藏入口；最终以服务端错误为准
- 已有条目 `recorded_at` 由数据库插入时生成，服务端真实时间；条目内容修改不回写 `recorded_at`（保留首次记录时刻）

## 删除语义

- `DELETE /work-logs/{id}`：作者本人 + 窗口内；同事务删除 `work_log_entry`、`work_log_tag`、`work_log`；幂等语义遵循既有命令风格（404 RESOURCE_NOT_FOUND，超窗 409）
- `PATCH /work-logs/{id}` 请求体新增 `deleteEntryIds: string[]?`：仅删除属于该日志且属于作者的条目；同请求中同一 id 不得同时出现在 upsert 与 delete（校验）

## 创建窗口

- 后端 `create` 不新增日期限制（保持既有契约）；"最近三日"由客户端 UI 限定（今天/昨天/前天，业务时区 `identity.today`）
- 多日创建沿用唯一约束 `UNIQUE(enterprise_id, author_user_id, work_date)`：409 `DAILY_JOURNAL_EXISTS` + existingJournalId，客户端转入 PATCH 语义

## Android 编辑器

- 日期以三枚日期 chip 切换（今天/昨天/前天），切换即加载对应 journal（存在则进入编辑，不存在则新建）
- 编辑既有条目：drafts 保留 entryId，PATCH upsert 语义不变；删除条目：移出 drafts 并记录 entryId 到 deleteEntryIds
- 历史列表条目行显示 `recordedAt` 的 HH:mm（截取 ISO 前 16 位）；无 recordedAt 时不显示
