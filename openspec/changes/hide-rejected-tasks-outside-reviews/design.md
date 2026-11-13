## Context

通讯录下属任务、任务汇总的 mine/created/managed 分页和年视图共用 CalendarRepository.tasks；日周月视图使用 calendar。后端 mine、导图派发任务及提醒已有 APPROVED 限制，但 created/managed 可返回 REJECTED。审批中心通过独立 TaskRepository.reviews 获取不可变审批快照。

## Goals / Non-Goals

**Goals:** 普通任务入口统一隐藏创建审批已驳回的任务；本人审批历史保留查询与重新提交；无需后端部署即可生效。

**Non-Goals:** 不删除历史、修改权限、改变待审批任务展示规则或隐藏验收驳回后仍需继续执行的有效任务。不调整审批结果通知。

## Decisions

- 采用数据层统一过滤，避免在每个页面分别判断造成预览、全文列表或日历模式遗漏。
- 使用 TaskPageDto/CalendarDto 的共用过滤函数，仅排除 reviewStatus == REJECTED；不根据 executionStatus、completionDecision 等字段判断。
- CalendarRepository 在返回任务页和日历数据前应用过滤；保留分页游标、moreUrl、服务端日期边界与统计元数据，避免过滤空页错误终止分页。当前视图不展示 CalendarDto.totalCount，汇总按实际加载项显示。
- 不修改审批数据获取和 task(id) 详情接口，以保留历史处理能力。导图、首页、提醒的数据源已限制有效任务，核对其规则。
- 针对真实 DTO 及序列化响应补充回归测试，验证被驳回任务隐藏、其他状态保留、游标保留及日历元数据不变；静态核对全部 scope/mode 入口与审批历史独立数据源。

## Risks / Trade-offs

- 一页可能全部被过滤 → 保留 nextCursor，已有分页页脚支持继续加载。
- 已打开页面可能仍持有旧数据 → 已有任务刷新版本信号会重新拉取，新增数据入口过滤在下一次加载生效。
- 全量单元测试可能存在既有编译阻断 → 如实记录，并独立执行本变更的针对性测试。
