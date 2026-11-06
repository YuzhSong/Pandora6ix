## Why

记录日志时需要重复输入每日事项。将所选日志日期的全部 todo 作为可编辑的 Markdown 默认正文，用户可按实际完成情况删改，并继续追加其他工作内容。

## What Changes

- 无草稿及已发布日志时，将当前账号所选日期的全部每日事项预填为 Markdown 无序列表，不按完成状态筛选。
- 预填内容可直接编辑或删除，末尾留空行供自由追加日志。
- 草稿和已有日志优先，不重复注入；同次编辑切换日期返回时保留已删改内容和标签。

## Capabilities

### New Capabilities

无。

### Modified Capabilities

- `android-journal-entries`: 增加按日期预填 todo 和保护用户正文的要求。

## Impact

- Android 日志编辑初始化、每日事项本地读取和草稿管理；新增纯 Kotlin 默认内容及优先级测试。
- 复用已有正文发布接口，不新增后端接口或数据表，不改变 todo 完成状态。
