# app-settings Specification

## Purpose
描述 Android 低保真原型的基础设置，包括全局字号缩放、通知开关、缓存清理反馈、关于信息与退出确认，并规定从导图及我的页进入设置的统一入口和暖色视觉风格。
## Requirements
### Requirement: 设置入口

系统 SHALL 在导图页右上角与「我的」页提供进入基础设置页的入口。

#### Scenario: 从导图进入设置

- **WHEN** 用户点击导图页右上角的设置图标
- **THEN** 打开基础设置页

#### Scenario: 从我的进入设置

- **WHEN** 用户在我的页点击「全部设置」
- **THEN** 打开基础设置页

### Requirement: 字号大小

系统 SHALL 允许在设置中选择小、标准、大三档字号，并在整个应用内即时生效。

#### Scenario: 调整字号

- **WHEN** 用户选择「大」
- **THEN** 应用内文字按所选档位放大显示

### Requirement: 通知提醒开关

系统 SHALL 提供通知提醒开关，可切换开启或关闭。

#### Scenario: 切换通知

- **WHEN** 用户切换通知提醒开关
- **THEN** 开关状态更新

### Requirement: 数据与关于

系统 SHALL 提供清除缓存（带确认与结果反馈）、关于我们（显示版本）与退出登录（带确认）操作。

#### Scenario: 清除缓存

- **WHEN** 用户点击清除缓存并确认
- **THEN** 显示清除完成的反馈

#### Scenario: 退出登录

- **WHEN** 用户点击退出登录并确认
- **THEN** 关闭设置页

### Requirement: 设置页样式统一

设置页 SHALL 使用与导图页一致的暖色卡片、分隔线与强调色。

#### Scenario: 查看设置页

- **WHEN** 用户打开设置页
- **THEN** 分组标题、设置行图标与分隔线风格与导图页保持一致
