## Context

日历的我创建的范围已有 reviewStatus，但任务标题未展示审批结果；汇总通过 reviewStatusLabel 显示“已驳回”。

## Goals / Non-Goals

**Goals:** 被驳回的任务清楚标注“被驳回”。

**Non-Goals:** 修改审批流程、执行状态或任务筛选规则。

## Decisions

- 日历各视图及任务选择列表为 REJECTED 标题添加“被驳回”前缀，短任务条也优先显示标记。
- 共享审批状态文案统一使用“被驳回”，汇总和详情保持一致。
- 仅根据 reviewStatus 判断，不根据 CANCELLED 推断驳回。

## Risks / Trade-offs

短任务条标题空间减少 → 标记放首位，完整标题仍可在详情中查看。
