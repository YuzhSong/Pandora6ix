## Why

全公司任务需要在员工日常使用的任务列表和日历里可见。用户打开相关任务或审批详情后，消息未读状态应反映已经查看的内容，审批角标应继续反映待处理事项。 [PR #57](https://github.com/YuzhSong/Pandora6ix/pull/57)

## What Changes

- 将已通过审批的全公司任务纳入同企业业务用户的「我的任务」与日历，执行人仍由发布者显式选择，写操作仍按角色和执行关系授权。
- 在可读任务中展示完整的当前执行人列表及其执行记录。任务详情展示业务时区的创建和最近生效编辑时间，待验收状态使用「待验收」，创始人创建入口使用「发布任务」。
- 查看任务或审批详情时同步相关通知已读状态，审批中心角标仅合计待审批与待验收。可访问的审批结果消息直接打开该审批，关闭后清理临时定位。
- 移除 Android 任务汇总和通讯录工具栏的手动刷新按钮，以及日志留言区的删除入口。

## Capabilities

### New Capabilities

### Modified Capabilities

- `company-task-workflow-completion` 增加全公司任务的日常可见入口、完整执行列表及详情时间展示。
- `notification-return-navigation` 增加详情已读同步、审批结果定位和待办角标口径。
- `android-task-summary` 记录汇总和通讯录工具栏的刷新入口调整。
- `android-journal-entries` 记录 Android 留言区移除删除入口。

## Impact

- 后端影响任务列表、日历关联查询及任务详情，详情新增可选 `editedAt` 字段。
- Android 影响任务详情、任务发布、审批中心、通知、任务汇总、通讯录和日志留言区。
