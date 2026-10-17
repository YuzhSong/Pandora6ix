## ADDED Requirements

### Requirement: 日志编辑器三日日期选择
创建界面 SHALL 提供公司时区最近三个业务日期（今天、昨天、前天）的日期切换；今天已有日报进入编辑，昨天或前天已有日报只能查看，不存在时允许首次补交并明确提示提交后只读。重复补交冲突不得自动 PATCH。

#### Scenario: 切换日期
- **WHEN** 用户切换到昨天
- **THEN** 昨天未填写可新建，已提交时不能再次编辑

### Requirement: 编辑与删除入口
日志卡片 SHALL 仅在 workDate 为公司今天且服务端 allowedActions 允许时提供编辑和带确认的删除；昨天、前天的补交日报立即只读。客户端预判仅影响 UI，服务端 409 JOURNAL_EDIT_WINDOW_EXPIRED 为最终裁决。

#### Scenario: 删除今天的日报
- **WHEN** 作者在公司今日删除今天的日报并确认
- **THEN** 调用 DELETE 成功后列表移除该篇

#### Scenario: 补交入口只读
- **WHEN** 用户刚刚补交昨天或前天的日报
- **THEN** 不显示编辑或删除按钮，也不按发表后时长放宽限制
