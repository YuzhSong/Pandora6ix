## Context

现有 TIMED 表示连续区间，task_assignment 以任务为单位保存完成状态，重复需生成独立任务才能复用状态、提醒与审批。

## Goals / Non-Goals

目标：日/周/月及 N 间隔、每次独立完成、清楚的时间语义、原有客户端兼容。非目标：无限重复、节假日规则、整组修改/审批。

## Decisions

- POST /tasks/recurring 接收 recurrence（旧服务端返回路由不存在，避免忽略规则）；POST /tasks 也接受可选 recurrence {frequency: DAILY|WEEKLY|MONTHLY, interval:1..365, weekdays:[1..7]}，日期为包含两端的生效范围。周以起始日期所在周的周一锚定，每月固定起始日，缺日跳过。重复定时必须结束分钟大于开始分钟，每次在同一天；全天每次一天。
- 一次事务生成最多 366 次，范围最多 10 年，空结果/过期单次截止拒绝，不部分保存。使用现有幂等层保证重试不会多建。
- 每次任务均走现有授权、审批、独立完成与提醒；批次使用同一 series_id，存储 recurrence_label 字符串供列表、详情与审批快照说明。编辑针对本次，不改变其他任务和规则元数据；UI 明示。
- 原有 TaskCreate Java 构造器保留重载，旧 JSON 无 recurrence 保持连续语义。客户端新增字段带默认值兼容旧数据；服务端先部署，Android 后发布。
- Android 独立重复开关与全天开关，日/周/月按钮及间隔输入，每周选择星期，预览生成日期/次数并校验与服务端一致。草稿保留重复配置。

## Risks / Trade-offs

每次独立审批产生多条审批 → 提交前明确次数与独立审批说明。批量写入规模 → 366 次上限。升级旧服务端 → 发布依赖先部署后端，不能将重复规则静默忽略。

## Migration Plan

V12 添加可空 series_id、recurrence_label 列，不改既有记录。API、生成器单测及事务集成测试通过后发布。

