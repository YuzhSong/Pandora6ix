## Why

设备崩溃记录确认 JournalEntryField.getCursorRect 使用越界 offset，连续输入会退出应用。

## What Changes

- 等待新帧后重新读取输入与排版，只有文字一致才计算光标位置。
- 回归覆盖旧排版遇到新输入、删除和边界光标。

## Capabilities

### New Capabilities

- `journal-cursor-safety`: 日志连续输入期间安全的光标跟随。

### Modified Capabilities

无。

## Impact

Android JournalEntryField；不改业务接口或草稿格式。
