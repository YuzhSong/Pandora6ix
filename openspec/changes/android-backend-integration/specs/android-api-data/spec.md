## ADDED Requirements


### Requirement: 导图四面板真实读取
导图 SHALL 调用 `GET /dashboard` 读取公司重要事项/公司派发任务/个人重要事项/日报工作条目四面板，保留各条目稳定 ID（itemId/taskId/entryId），并按面板 moreUrl/nextCursor 读取更多；空数据、加载失败、加载中三种状态分开显示。

#### Scenario: 面板分页
- **WHEN** 面板摘要不足且有 nextCursor
- **THEN** 可继续读取更多条目，不以本地十条为总量上限

### Requirement: 单条详情按稳定 ID
点击任一摘要 SHALL 用对应类型 ID 读取全文：`/company-important-items/{id}`、`/tasks/{id}`、`/work-log-entries/{entryId}`、`/work-logs/{journalId}`；不使用列表下标或拼接字符串定位。

#### Scenario: 打开工作条目
- **WHEN** 点击日报面板某条工作条目
- **THEN** 展示该 entryId 的全文，entryId 不被当作 journalId

### Requirement: 日历与任务列表接口化
日/周/月视图 SHALL 调用 `GET /calendar?mode=&anchorDate=&tagIds=&cursor=&limit=`，采用服务端 rangeStart/rangeEnd/today/businessTimezone，不按设备时区重算业务日；任务列表调用 `GET /tasks?scope=mine|managed|created`；标签多选为 AND；日历不混入日志。

#### Scenario: 周视图
- **WHEN** 选择某周
- **THEN** 展示服务端该区间的已批准本人任务，跨格同 taskId 保持同一任务

### Requirement: 企业标签与统计
标签 SHALL 使用 `GET /tags` 的 id/name/colorHex/version 与独立 primaryTagId，任务颜色用 displayColor，不以名称为键；本人统计读取 `GET /statistics`（total/completed/uncompleted），不自行按进度或截止计算完成。

#### Scenario: 标签着色
- **WHEN** 任务设置了主标签
- **THEN** 视图任务颜色使用服务端 displayColor
