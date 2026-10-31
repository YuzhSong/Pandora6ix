## Why

Android 日志创建界面点击已过编辑期限的日志日期后，提示持续停留。用户需要每次点击后短暂提示并自动消失。

## What Changes

- 编辑期限提示显示 2 秒后自动消失，再次点击重新计时。
- 保持日期、草稿及发布错误处理行为。

## Capabilities

### New Capabilities

- `android-journal-edit-notice`: 已过编辑期限日志的短暂点击反馈。

### Modified Capabilities

无。

## Impact

仅 Android LogsScreen；无 API 或依赖变更。
