# calendar-task-entry Specification

## Purpose
Define shared real task entry and consistent all-day/timed scheduling presentation across Android calendar modes.
## Requirements
### Requirement: 紧凑月历
系统 SHALL 展示当月全部日期及首尾周相邻日期，以四至六周自适应高度显示；跨日任务同周连续、跨周分段，不重叠任务共用行，容量不足显示剩余数量并可进入周视图。

#### Scenario: 日期与重叠
- **WHEN** 月份需要六周，且同周包含多个重叠任务
- **THEN** 所有日期均展示，任务不覆盖，未展示任务数量可见。

### Requirement: 全天及定时任务
系统 SHALL 提供共用日期导航的日周视图，全天区放仅日期任务，时间轴放有自定义小时分钟的任务，日视图仅显示任务。时间任务跨日期须按每日实际区间显示，重叠任务须并排而非互盖。

#### Scenario: 切换日期
- **WHEN** 选择跨日任务覆盖的日期
- **THEN** 日视图出现同一任务，不展示工作日志。

### Requirement: 任务录入
系统 SHALL 根据服务端 createPersonalTasks 与 createDelegatedTasks 能力展示日周月视图共用的添加入口，并使用真实任务 API 保存名称、备注、日期、可选时间、当前支持的重要程度与接收人。仅本人执行时 SHALL 提交 PERSONAL；包含其他人的派发任务 SHALL 按服务端授权范围提交 DELEGATED。界面 SHALL 保留未保存草稿保护，不将本地草稿当作已生效任务。当前后端个人任务兼容能力与入口仍存在；后续移除个人任务入口的需求 SHALL NOT 被本次归档记录为已实现。

#### Scenario: 有效录入
- **WHEN** 有创建能力的用户填写有效任务并提交
- **THEN** 服务端返回真实任务，日周月视图共享其数据；创始人按后端规则直接生效，其他提交保留上级审批

#### Scenario: 无效录入与退出
- **WHEN** 名称或接收人缺失、时间倒置，或者用户关闭未保存内容
- **THEN** 无效内容不可保存，有修改的关闭须确认放弃

#### Scenario: 无创建能力
- **WHEN** 当前用户的 createPersonalTasks 与 createDelegatedTasks 均为 false
- **THEN** 添加入口不展示，服务端仍独立执行创建权限校验
