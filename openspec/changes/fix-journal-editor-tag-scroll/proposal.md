## Why

日志编辑器的已选标签位于滚动区域之外，全选大量标签会挤占工作内容输入区，导致无法向下填写。

## What Changes

- 日期、标签和工作条目统一纵向滚动，避免标签挤占输入区。
- 标题及底部取消、发布操作保持固定，沿用键盘避让。
- 输入、换行、移动光标及键盘高度变化后，主动将光标所在行滚动到表单可见区域，保留全文及手动查看前文能力。
- 键盘弹出时日期、标签和选择标签按钮仍保留在表单中，支持先写内容再选标签。
- 日志标签选择器沿用任务的固定操作区与低调样式，支持点击“设为主”切换主标签。

## Capabilities

### New Capabilities
- `journal-editor-scroll`: 日志表单在多标签、长内容和键盘弹出时可滚动填写。

### Modified Capabilities
无。

## Impact

仅 Android MainActivity.kt 的日志创建和编辑弹层及 ui/logs/JournalEntryField.kt 输入组件；无 API 或依赖变更。继续在 Android-swj 修改。
