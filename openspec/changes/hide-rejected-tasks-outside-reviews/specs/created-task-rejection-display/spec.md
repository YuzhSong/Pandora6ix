## REMOVED Requirements

### Requirement: Rejected created tasks have explicit labels
**Reason**: 用户要求普通任务界面隐藏创建审批已驳回的任务，仅在审批中心“我提交的”保留历史。
**Migration**: 使用以下 Rejected tasks are confined to submitted review history 规则；无需数据迁移。

## ADDED Requirements

### Requirement: Rejected tasks are confined to submitted review history

Android SHALL exclude tasks whose reviewStatus is REJECTED from ordinary task collections, including contacts subordinate task lists and previews, task summaries for mine/created/managed, and day/week/month/year task views. Rejected review records SHALL remain visible in approval center “我提交的”, retaining detail and existing resubmission capability. Pending and approved task behavior SHALL remain unchanged. Rejected completion requests SHALL NOT hide an otherwise approved task. Filtering SHALL retain pagination continuation metadata even when no visible items remain on the current page.

#### Scenario: Rejected task in ordinary scopes
- **WHEN** a task page for mine, created or managed includes a task with reviewStatus REJECTED
- **THEN** Android omits that task before supplying contacts, summaries and task views

#### Scenario: Submitted history remains accessible
- **WHEN** the author opens approval center “我提交的” after a task is rejected
- **THEN** the rejected review is still shown and can be opened or resubmitted under existing rules

#### Scenario: Completion rejection is different
- **WHEN** creation approval is APPROVED but a completion request has been rejected
- **THEN** the task continues to appear in ordinary task lists

#### Scenario: Page contains only rejected tasks
- **WHEN** the current task page contains only rejected tasks and has a nextCursor
- **THEN** the filtered page is empty but retains nextCursor so further visible tasks can still be loaded
