## Context

ProfileScreen 在个人资料后按统计响应显示任务统计，然后显示组织信息；审批中心原本位于下面的设置分组中。MainActivity 登录时仅为该面板请求 statistics。

## Goals / Non-Goals

**Goals:** 审批中心占据原统计面板的位置，入口唯一，保持原有权限、角标和导航。

**Non-Goals:** 修改审批逻辑、其他页面、后台统计 API 或导航项。

## Decisions

- 在原统计卡片位置复用 SettingsCard 和 SettingsRow 显示独立审批入口，保持 Pandora 组件风格。
- 移动现有 showReviewCenter 条件、角标和回调代码，避免重新实现计数或改变访问权限。
- 删除 ProfileScreen 的 statistics 参数、MainActivity 的 statistics 状态及登录时请求；保留仓库方法和服务端 API，不扩大删除范围。
- 无需为纯布局搬移新增镜像测试，运行 Android 构建及现有测试，并通过当前模拟器检查位置和导航。

## Risks / Trade-offs

- 页面缩短会改变原滚动范围 → 继续使用现有外部 profileScrollState，由 Compose 约束滚动偏移。
- 无审批入口能力的账号 → 按原条件隐藏审批面板，不保留统计占位。
