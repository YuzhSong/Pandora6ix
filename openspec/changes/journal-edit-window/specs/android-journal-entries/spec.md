## ADDED Requirements

### Requirement: 日志编辑器三日日期选择
创建/编辑界面 SHALL 提供最近三个自然日（今天、昨天、前天）的日期切换；选择某日后加载该日已有日报进入编辑，或以该日期创建新日报。

#### Scenario: 切换日期
- **WHEN** 用户在编辑器中切换到昨天
- **THEN** 加载昨天的日报（存在）或以昨天为业务日期新建（不存在）

### Requirement: 编辑与删除入口
日志卡片 SHALL 在日报创建后 24 小时内提供编辑与删除入口（删除带确认）；超窗后隐藏入口。客户端预判仅影响 UI，服务端 409 JOURNAL_EDIT_WINDOW_EXPIRED 为最终裁决并以友好文案呈现。

#### Scenario: 删除整篇日报
- **WHEN** 用户在窗口内删除某篇日报并确认
- **THEN** 调用 DELETE /work-logs/{id} 成功后列表移除该篇

#### Scenario: 超窗入口隐藏
- **WHEN** 日报创建已超过 24 小时
- **THEN** 卡片不再显示编辑/删除按钮
