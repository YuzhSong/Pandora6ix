## Why

日志当前在创建业务日结束时关闭编辑，无法满足作者从创建时刻起连续 24 小时可修改、编辑和删除的要求。

## What Changes

- 作者在 published_at 起未满 24 小时可编辑与删除，达到 24 小时关闭；跨午夜、公司时区调整和 workDate 不影响窗口。
- Android 按服务端 allowedActions 编辑已有日志，三天日期限制仅约束新建；同步提示文案。
- 保留作者权限、版本校验、新建日期范围与删除清理规则。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `backend-journals-dashboard`: 将创建当天窗口改为连续 24 小时。
- `android-journal-entries`: 已有日志编辑不受新建日期范围限制，提示 24 小时规则。

## Impact

JournalService、Android 日志编辑和 API 错误提示、后端测试；无新接口、数据库迁移或依赖。
