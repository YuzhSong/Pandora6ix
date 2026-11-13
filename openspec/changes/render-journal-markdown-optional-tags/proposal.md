## Why

日志发布被客户端标签必选校验阻止，且从待办预填的 Markdown 列表在发布后仍以源码显示。用户需要无标签发布，并在阅读已提交日志时看到格式化内容。

## What Changes

- 日志标签可选，保留选择、主标签切换及清空标签能力。
- 编辑框支持普通文本和 Markdown 源文，发布后卡片和详情渲染 Markdown。
- 保留服务端原文，重新编辑时仍显示原文；普通文本正常展示。

## Capabilities

### New Capabilities

- `journal-markdown-display`: 日志正文在阅读时渲染 Markdown，编辑时保留源码。

### Modified Capabilities

- `journal-tag-order`: 明确日志创建与编辑不要求标签。

## Impact

Android 日志发布校验、编辑提示、卡片和详情正文。引入 Android 原生 Markdown 渲染依赖；后端已有空标签支持，无需修改 API 或数据库。通讯录紧凑摘要保持单行，完整阅读使用日志详情。
