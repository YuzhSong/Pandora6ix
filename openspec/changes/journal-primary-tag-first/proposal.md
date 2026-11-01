## Why

日志标签按目录或接口顺序展示时，主标签可能不在首位，且编辑回填可能误用第一个次标签作为主标签。

## What Changes

- 日志展示、编辑回填及选择器中，主标签始终排在已选标签第一位。
- 次标签维持原顺序，切换主标签后立即更新排列。

## Capabilities

### New Capabilities
- `journal-tag-order`: 日志主标签优先展示与编辑保留。

### Modified Capabilities

无。

## Impact

仅 Android 日志界面的标签排列；复用现有 primaryTagId，无 API 或数据库变更。
