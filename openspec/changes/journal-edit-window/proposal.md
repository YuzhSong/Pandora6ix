# Proposal: journal-edit-window

## Why

当前日报一经创建即可无限期修改，且无法删除；创建界面固定为"今天"；补充内容只能追加新条目，无法在原条目中删改；条目时间未向用户展示。需要把日报的生命周期收紧为：创建后 24 小时内可改可删，创建时可在最近三个自然日内选择日期，补充时在原同一条日志的既有条目上删改，并展示真实记录时间。

## What Changes

- 后端 `PATCH /work-logs/{id}`：新增 24 小时编辑窗口校验（自 `published_at` 起算），超窗返回 409 `JOURNAL_EDIT_WINDOW_EXPIRED`；请求体新增 `deleteEntryIds`，支持在原日志内删除既有条目
- 后端新增 `DELETE /work-logs/{id}`：仅作者本人，24 小时窗口内可删，级联删除条目与标签关联；同样受窗口校验
- Android 编辑器：日期从固定"今天"改为最近三日（今天/昨天/前天）可选
- Android 编辑器：既有条目支持逐条删除（携带 `deleteEntryIds`），修改走原条目 upsert，不再以新条目追加代替删改
- Android 日志列表/详情：展示条目真实记录时间（`recorded_at` 的 HH:mm，来自服务端，非客户端生成）
- Android：日志卡片在 24 小时窗口内提供"编辑/删除"入口，超窗隐藏并提示

## Impact

- Affected specs: `backend-journals-dashboard`（PATCH/DELETE 语义）、`android-journal-entries`（编辑器与展示）
- Affected code: `backend/src/main/java/com/pandora6ix/backend/{controller/JournalController,application/journals/JournalUseCases,service/JournalService,api/Contracts}.java`、`backend/openapi.json`、`android/.../data/Journal{Models,Repository}.kt`、`android/.../MainActivity.kt`（LogsScreen）
