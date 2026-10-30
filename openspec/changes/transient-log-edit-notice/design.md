## Context

LogsScreen 将过期日志提示存入 publishError，导致持续显示。

## Goals / Non-Goals

目标：短暂显示且重复点击重置计时。非目标：修改编辑权限或发布错误生命周期。

## Decisions

使用独立提示状态与递增事件编号，LaunchedEffect 在每次事件时取消旧计时并启动 2 秒计时。沿用原提示位置；有效日期切换或关闭编辑器清除提示。

## Risks / Trade-offs

重复相同文字不会触发状态变化 → 使用事件编号作为 effect key。错误信息优先显示，避免遮盖发布错误。
