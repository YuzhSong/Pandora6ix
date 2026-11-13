## Context

Android publish() 强制 tags 非空；后端 TagService.validate 已支持空标签和空主标签。日志条目存储纯字符串，预填待办使用 Markdown 列表，卡片与详情用 Text 原样显示。

## Goals / Non-Goals

**Goals:** 无标签可创建及保存日志；普通文本与 Markdown 共用编辑框；已发布内容按 Markdown 阅读；再次编辑保持源码。

**Non-Goals:** 不增加编辑器工具栏、实时预览、服务端 HTML 转换、数据库字段或其他端功能；不改变日志编辑权限和日期范围。

## Decisions

- 移除标签必选判断，并明确标签“可选”。新建继续传空标签/空主标签，编辑传空列表以支持清空标签。
- 使用 Markwon 4.6.2 core + tasklist + strikethrough，通过 AndroidView/TextView 渲染标题、段落、列表、引用、强调、代码、链接、任务列表和删除线。与手写解析相比可正确处理嵌套、转义和代码块；与 WebView 相比无需网页容器。
- 保留原始字符串存储，不对 DTO、草稿和编辑加载做格式转换；移除发布路径的 trim，避免破坏首行缩进代码等 Markdown 语义。普通文本单换行保持可见（SoftBreakAddsNewLinePlugin）。
- 共用 JournalMarkdown 组件用于卡片及详情；卡片保留三行省略，详情完整展示。阅读时任务复选框只读，不反向更新待办或日志。
- 绑定现有主题正文大小、颜色及行距；主题变更重建渲染器以更新任务列表颜色。不加入远程图片加载器和 HTML 插件。
- 渲染前仅为列表后的未缩进非列表行补充段落分隔，禁用该场景的 Markdown lazy continuation；保留显式缩进续行、嵌套列表和围栏代码内部内容。处理结果仅传给渲染器，保存与编辑仍使用源文。编辑器不显示 Markdown 能力说明。
- 通过自定义 BlockHandler 在块起止处仅 ensureNewLine，避免默认 blockEnd 的 ensureNewLine + forceNewLine 在列表、段落等嵌套块处叠加空行。保留块内部换行，并按字体真实 metrics 计算额外行距，避免把完整字号误当作原始行高。
- 编辑器分区标题统一移除前导装饰点，“标签”不附加括号说明。

## Risks / Trade-offs

- 新依赖需要下载 → 固定版本并通过 Android 构建验证。
- 原有文本包含合法 Markdown 符号时会被解析 → 编辑源文保持不变，用户可用 Markdown 转义。
- TextView 位于可点击卡片内 → 卡片不启用链接交互，详情启用链接；保证卡片点击仍打开详情。

## Migration Plan

仅客户端更新，无数据迁移。回滚组件及依赖即可恢复原文显示。
