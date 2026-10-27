# Spec Delta

## MODIFIED Requirements

### Requirement: 通知提醒开关

系统 SHALL 提供通知提醒开关，可切换开启或关闭。开启时，在用户授权系统通知权限后 SHALL 注册当前 Android 安装以接收远程通知；关闭时 SHALL 停用该安装的远程通知注册。系统 SHALL 保留现有本地提醒偏好行为。

#### Scenario: 切换通知

- **WHEN** 用户切换通知提醒开关
- **THEN** 开关状态更新

#### Scenario: 开启远程通知

- **WHEN** 用户开启通知且系统通知权限已授予
- **THEN** 当前安装注册为该账号的远程通知接收端

#### Scenario: 关闭远程通知

- **WHEN** 用户关闭通知提醒
- **THEN** 当前安装被停用，不再接收新的远程通知
