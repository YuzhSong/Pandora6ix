## Context

`TaskEntrySheet` 的开始/结束各自独立修改，只在提交时由 `validTaskInterval` 拦截；任务汇总通过 `summaryPersonName` 只在本人与下属名单中查找姓名，同级或上级执行人得到占位名“成员 xxxx”。

## Decisions

- 顺延逻辑写成纯函数 `followStart(TaskSpan, …)`（`CalendarLayout.kt`），只在开始的日期/时间选择器确认时调用，便于单测；修改结束时不反向调整开始。
- 只在结束失效时顺延，避免覆盖用户已设好的合法结束时间。
- 执行人姓名新增 `executorName(task, id, fallback)`，优先使用后端已有的 `recipientName`，不新增接口。

## Risks / Trade-offs

- [用户期望总是保持时长] → 当前选择保守策略；如需“始终保持时长”可只改 `followStart`。
