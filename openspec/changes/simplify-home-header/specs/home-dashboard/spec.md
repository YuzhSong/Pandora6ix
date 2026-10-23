## ADDED Requirements

### Requirement: 首页头部只保留日期与消息

导图（首页）头部 SHALL 只显示日期标题与「消息」按钮，SHALL NOT 在首页提供进入设置页的按钮或入口。

#### Scenario: 打开首页

- **WHEN** 用户进入导图页面
- **THEN** 头部只有日期（如「10月23日 周五」）和一个消息按钮

#### Scenario: 进入设置

- **WHEN** 用户需要改设置
- **THEN** 从「我的」页的「全部设置」或「消息通知」进入，首页不再直达设置页
