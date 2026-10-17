## ADDED Requirements

### Requirement: 日报公司业务日期编辑窗口
日报 SHALL 仅允许作者在 workDate 等于企业时区 today 时通过 PATCH 修改或 DELETE 删除；跨公司日期或补交日报立即返回 409 JOURNAL_EDIT_WINDOW_EXPIRED，非作者请求返回 403。删除 SHALL 原子清理条目、标签与个人重要事项引用，并递增受影响选择版本。

#### Scenario: 今天内修改与删除
- **WHEN** 作者修改或删除今天的日报
- **THEN** 操作成功，条目稳定 ID 保持，删除解除同日唯一约束

#### Scenario: 补交及跨日后只读
- **WHEN** 作者修改或删除昨天、前天或更早的日报，即使刚提交
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
创建界面 SHALL 允许在最近三个自然日（今天、昨天、前天）中选择业务日期创建日报；服务端 create 也仅允许该三个公司业务日期，其他日期返回 422 JOURNAL_DATE_OUT_OF_RANGE；昨天/前天仅可补交一篇，提交后不可修改或删除，同日唯一约束维持 409 DAILY_JOURNAL_EXISTS + existingJournalId 语义。

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
