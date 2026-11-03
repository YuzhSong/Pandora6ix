## Why

日志详情目前只能看内容，上级看完下属日志无法在系统里给出反馈；查看他人日志时也只显示“成员 xxxx”，看不出是谁、属于哪个部门。需要在日志详情下方提供上级留言区，并展示日志作者的姓名、部门与职位。

## What Changes

- 日志详情返回作者信息：姓名、职位、所属组织与部门。
- 新增日志留言：现有权限下能查看该日志的上级（管理范围内）可以留言，作者与这些上级可以查看留言；留言者可删除自己的留言。
- Android 日志详情：顶部显示作者姓名、部门与职位；页面下方新增“上级留言”区，含留言列表和较大的多行输入框（上级可见），作者看到只读列表。
- 删除日志时一并删除其留言。

## Capabilities

### New Capabilities
- `journal-manager-comments`：日志作者信息展示与上级留言的权限、接口和 Android 交互。

### Modified Capabilities

无。日志的创建、编辑窗口、删除权限与可见范围规则不变。

## Impact

- 后端：新表 `work_log_comment`（Flyway V16）、`JournalService`/`JournalController` 新接口、`Contracts.JournalComment`、`openapi.json`、集成测试。
- Android：`JournalModels`、`JournalRepository`、`MainActivity` 日志详情页。
- Web：不改动（日志详情新增字段为附加字段）。
