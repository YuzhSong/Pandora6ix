## Context

详情创建时间错误使用 workDate 作为日期，publishedAt 只提供时分。

## Goals / Non-Goals

目标：日期与时分均来自实际创建时刻。非目标：调整日志所属日期、后端时间戳或编辑权限。

## Decisions

复用 businessDateTimeLabel，按公司业务时区将 publishedAt 转为 MM-dd HH:mm。空值或解析失败展示“未知”，避免错误地退回 workDate。

## Risks / Trade-offs

UTC 跨日 → 复用现有时区转换方法，检查跨日输入。沿用现有月日精度。
