## Why

日报需要按公司业务日期限制编辑与删除，并支持今天、昨天、前天的缺失日报补交及稳定条目删改。2026-10-17 用户明确：昨天和前天补交后不可再次编辑或删除，今日的日志仅在今日可编辑删除；此决定取代原 PR 的滚动 24 小时规则。

## What Changes

- 服务端按 workDate 与公司时区 today 校验 PATCH/DELETE；超期返回 JOURNAL_EDIT_WINDOW_EXPIRED。
- POST 仅允许最近三个公司业务日期；保持每天一篇与稳定 entryId，补交后只读。
- 删除条目时清理其个人重要事项选择引用与版本；不自动补选。
- Android 日期选择按公司时区，卡片与编辑器只读状态、重复补交冲突、草稿恢复同步；真实条目时间展示保留。

## Impact

backend、Android、OpenAPI 与验证；最新实现与证据在 complete-client-administration-backend，真机联调仍待验收。
