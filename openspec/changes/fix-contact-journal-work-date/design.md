## Context

JournalRow 被通讯录全部下属日志、成员日志与最近日志预览复用。当前以 publishedAt 优先、workDate 兜底的方式展示，导致有发布时间时隐藏工作日期。

## Goals / Non-Goals

**Goals:** 始终能辨认日志所属工作日期，并明确区分发布时间。

**Non-Goals:** 不改变接口、筛选、分页或日志详情。

## Decisions

- 在姓名下独立显示“日志日期：YYYY-MM-DD”，直接使用 workDate，不做时区转换；空值显示“未知”。
- publishedAt 继续使用 businessDateTimeLabel 按业务时区格式化，在独立行标注“发布时间：”；无有效展示值则隐藏该行。
- 不继续把日期挤在姓名右侧，以留出窄屏与长姓名的空间。使用现有共用组件覆盖全部入口。

## Risks / Trade-offs

- 行高增加 → 沿用现有 Column 间距和可滚动列表，保留摘要单行。
- 老数据缺失 workDate → 显示“未知”，避免把发布时间误当工作日期。
