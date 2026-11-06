## Context

`HomeItemDetailScreen` 用同一个 16sp Text 展示拼接后的「标题：正文」。事件纪对应 `COMPANY_ITEM`，详情接口分别返回标题和正文。

## Goals / Non-Goals

**Goals:** 事件纪标题放大为 24sp；正文在标题下方独立展示，保留换行并按可用宽度自动折行。

**Non-Goals:** 不修改任务详情、接口、内容存储或首页列表。

## Decisions

- `COMPANY_ITEM` 单独渲染标题和正文，使用详情接口的最新标题，加载前以列表标题初始化。避免按字符串前缀拆分正文误删内容。
- 复用 `PandoraTypography.titleLarge`（24sp/32sp）和 `bodyLarge`（16sp/24sp），标题添加 heading 语义，正文 fillMaxWidth、softWrap 且不限制行数。
- 保持现有纵向滚动和 16dp 段间距。正文为空时只显示标题，避免原有空正文回退造成标题重复。

## Risks / Trade-offs

- 长标题和大字体会增加页面高度 → 标题不截断，继续使用页面滚动容器。
- 当前环境未连接可用设备时无法完成视觉验收 → 构建检查后明确记录人工验收项。
