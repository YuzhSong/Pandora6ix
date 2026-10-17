## ADDED Requirements

### Requirement: 日报 24 小时编辑窗口
日报 SHALL 仅允许作者在其创建时刻（published_at）起 24 小时内通过 `PATCH /work-logs/{id}` 修改或通过 `DELETE /work-logs/{id}` 删除；超窗请求返回 409 `JOURNAL_EDIT_WINDOW_EXPIRED`，非作者请求返回 403。删除 SHALL 级联移除该日志的全部条目与标签关联。

#### Scenario: 窗口内修改与删除
- **WHEN** 作者在创建后 24 小时内修改内容或删除整篇日报
- **THEN** 操作成功，删除后条目与标签关联一并移除，且当日唯一约束解除（可重新创建同日日报）

#### Scenario: 超窗拒绝
- **WHEN** 创建已超过 24 小时后请求 PATCH 或 DELETE
- **THEN** 返回 409 JOURNAL_EDIT_WINDOW_EXPIRED，内容保持不变

#### Scenario: 非作者操作
- **WHEN** 非作者请求 PATCH 或 DELETE 该日报
- **THEN** 返回 403，内容保持不变

### Requirement: 日报条目删除
`PATCH /work-logs/{id}` SHALL 支持 `deleteEntryIds`：删除属于该日志的既有条目；同一请求中同一 entryId 不得同时出现在 upsertEntries 与 deleteEntryIds。

#### Scenario: 原条目删改
- **WHEN** 用户在窗口内补充或整理当日日报
- **THEN** 修改落在原条目上（upsert 原 entryId），删除的条目从该篇移除，不以追加新条目代替删改

#### Scenario: 删除已选重要条目
- **WHEN** 作者删除已选为个人重要事项的条目或其所属日报
- **THEN** 同一事务移除对应选择引用并递增选择版本，保留手动选择状态和其他条目，不补选；旧版本保存返回 409

### Requirement: 日报创建日期窗口
创建界面 SHALL 允许在最近三个自然日（今天、昨天、前天）中选择业务日期创建日报；服务端 create 不限制历史日期，同日唯一约束维持 409 DAILY_JOURNAL_EXISTS + existingJournalId 语义。

#### Scenario: 为昨天创建日报
- **WHEN** 用户在创建界面选择昨天并发布
- **THEN** 以该日期创建独立一篇日报，列表按日期归位

#### Scenario: 同日重复
- **WHEN** 所选日期已有日报再次创建
- **THEN** 409 DAILY_JOURNAL_EXISTS 携带 existingJournalId，客户端转入编辑该篇

### Requirement: 日报条目真实时间
日报条目 SHALL 展示服务端记录的真实时间（recorded_at 的小时与分钟）；客户端不得生成或伪造时间。

#### Scenario: 历史条目时间展示
- **WHEN** 用户查看日志列表或详情
- **THEN** 每条条目显示其 recorded_at 对应的 HH:mm，无该字段时不显示时间
