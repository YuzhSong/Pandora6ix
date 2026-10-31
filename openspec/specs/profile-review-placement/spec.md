# profile-review-placement Specification

## Purpose
TBD - created by archiving change replace-profile-statistics-with-reviews. Update Purpose after archive.
## Requirements
### Requirement: Approval center replaces profile statistics

Android 的“我的”页 SHALL 删除任务统计面板，将审批中心作为独立面板放在个人资料之后、组织信息之前。审批中心 SHALL 只有一个入口，并保留原有能力条件、真实角标、导航回调和返回滚动状态。客户端 SHALL 不再为该删除面板发起登录时的统计请求。

#### Scenario: Authorized profile user
- **WHEN** 具有原审批入口能力的用户打开“我的”
- **THEN** 在个人资料后、组织信息前显示审批中心，不显示任务统计，设置分组内不再重复显示审批入口

#### Scenario: Open and return
- **WHEN** 用户点击移动后的审批中心并返回
- **THEN** 打开现有审批中心并通过现有状态返回“我的”，入口角标继续使用原始审批计数

#### Scenario: No approval entry capability
- **WHEN** 无原审批入口能力的用户打开“我的”
- **THEN** 不显示审批面板或任务统计占位，其余资料及设置保持可用
