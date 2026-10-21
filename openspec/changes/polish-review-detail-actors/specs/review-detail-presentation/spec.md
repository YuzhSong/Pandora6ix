## ADDED Requirements

### Requirement: 审批详情分区展示

审批详情 SHALL 按语义分区呈现，而不是把所有字段平铺在一张卡片里：

- **任务信息**：标题 + 决策徽章 + 类型 chip + 重要程度（彩色星级 + 文案）+ 说明
- **时间安排**：日期范围（`开始 – 结束`，同一天只显示一天）+ 时间（`全天` 或 `HH:mm – HH:mm`）
- **人员**：请求人、审批人、执行人（见下条）
- **标签**：胶囊 chip 展示标签名，**底色使用服务端 `colorHex` 的真实标签颜色**，主标签标注「· 主」；无标签时不展示该分区
- **审批结果**（已决定时）：状态文案 + 决策徽章 + 审批人 + 批注 + 决定时间

分区 SHALL 使用同一套「小标题 + 主题卡片」样式，卡片底色跟随当前主题；**标签除外**——标签 SHALL 显示为标签自身颜色（服务端 `colorHex`），SHALL NOT 用主题色或主题派生色作为标签底色，与任务详情、日志中的标签展示保持一致。

#### Scenario: 打开审批详情
- **WHEN** 用户从队列点开一条申请
- **THEN** 依次看到任务信息、时间安排、人员、标签（若有）与审批结果（若已决定）分区，而不是一列平铺字段

### Requirement: 人员显示真实姓名、职位与头像

审批详情 SHALL 为请求人、审批人、执行人显示**真实姓名**（`display_name`）与**真实职位**（`job_title`），并 SHALL 显示**真实头像**（服务端头像，见 `add-user-avatar`），SHALL NOT 只显示「接收人 N 人」或使用首字占位冒充头像。
姓名缺失（旧数据、已停用账号）时 SHALL 回退显示 `成员 <ID 后四位>`；职位未设置时 SHALL 不显示职位标签；头像未设置或加载失败时 SHALL 回退到姓名首字圆形占位（明确不是照片）。
后端未返回执行关系时 SHALL 回退到快照口径（`本人执行` / `接收人 N 人`）。
审批队列卡片 SHALL 同时显示「类型 · 请求人 姓名」。

#### Scenario: 待我审批的申请
- **WHEN** 审批人打开一条下属提交的个人任务申请
- **THEN** 人员分区显示「请求人 <提交人姓名>（第 N 次提交 · 提交时间）」「审批人 <审批人姓名>」「执行人 <提交人姓名>」

#### Scenario: 派发任务多人执行
- **WHEN** 申请是派发给 3 人的任务
- **THEN** 执行人逐个以「头像 + 姓名 + 职位」胶囊列出，不合并成「接收人 3 人」

### Requirement: 后端提供人员档案

`GET /task-reviews` 与 `GET /task-reviews/{id}` 的返回 SHALL 附带人员档案：`submittedByName` / `submittedByJobTitle`（请求人）、`reviewerName` / `reviewerJobTitle`（审批人）、`recipients[]`（`{id, displayName, jobTitle}`，取自**本条申请提交的快照**；快照缺失的历史数据才回退任务当前执行关系）。
展示名 SHALL 取自本企业 `app_user.display_name`、职位取自 `app_user.job_title`；用户不存在或字段为空时 SHALL 为 null，由客户端回退。头像 SHALL NOT 内嵌在审批响应里，客户端按 `GET /users/{id}/avatar` 单独取。

#### Scenario: 读取审批详情
- **WHEN** 客户端请求一条审批记录
- **THEN** 响应包含 `submittedByName`、`submittedByJobTitle`、`reviewerName`、`reviewerJobTitle` 与 `recipients[].displayName`/`jobTitle`，值为对应人员的企业内真实姓名与职位

#### Scenario: 任务改派后查看旧审批
- **WHEN** 任务执行关系已变化（改派/执行人被移除），再打开当时的审批记录
- **THEN** 执行人仍是提交快照里的人员与姓名，SHALL NOT 变成当前执行关系，也 SHALL NOT 退化成「接收人 N 人」

### Requirement: 审批界面背景与设置页一致

审批中心与审批详情的背景 SHALL 使用主题 `dashboard` 色（`WarmDashboard`），与设置页同一底色，SHALL NOT 使用主题 `background` 色导致同一主题下不同页面深浅不一。

#### Scenario: 暖米主题下切换页面
- **WHEN** 当前主题为暖米，用户在设置页与审批中心之间切换
- **THEN** 两页背景色相同（`#FFF1C7`），不出现一页偏白一页偏黄的跳色

#### Scenario: 切换主题后查看标签
- **WHEN** 同一条申请分别用暖米与深色主题打开
- **THEN** 标签胶囊仍是该标签在服务端定义的颜色（如 `#F2A65A`），SHALL NOT 随主题变成奶油色或深灰
