# calendar-view Specification

## Purpose
Define period navigation, continuous year scrolling, date selection and pinch transitions for the existing Android calendar prototype.
## Requirements
### Requirement: 无箭头按钮的日期标题

视图页日期行必须（SHALL）仅显示当前周期标题（年 / 月区间 / 日含星期），不得提供任何周期切换按钮。

#### Scenario: 日期行呈现

- **WHEN** 用户处于任一视图级别
- **THEN** 日期行右侧不出现左右箭头按钮，仅显示周期标题

### Requirement: 年视图连续滚动切换年份

年视图必须（SHALL）提供跨多年连续滚动的月份网格：上下拖动跟手、跨年自然衔接、不分片跳年；顶部标题年份必须随滚动实时更新。

#### Scenario: 拖动跨年

- **WHEN** 用户在年视图连续上拉越过年份边界
- **THEN** 月份卡片连续滚动进入下一年，标题年份实时跟随，无分片跳变

#### Scenario: 近似无限滚动

- **WHEN** 用户滚动接近列表任一边缘
- **THEN** 网格静默重新锚定并保持当前视觉位置，可继续朝同方向滚动

#### Scenario: 回到今天

- **WHEN** 用户点击「回到今天」且当前浏览年份距今年超过一年
- **THEN** 网格定位到今天所在年份的一月

### Requirement: 月视图上下滑动切换月份

月视图必须（SHALL）支持单指上下滑动切换月份，上滑为下一月，下拉为上一月。

#### Scenario: 切换月份

- **WHEN** 用户在月视图上滑超过阈值
- **THEN** 显示下一月；下拉则显示上一月

### Requirement: 周 / 日视图左右滑动切换日期

周视图与日视图必须（SHALL）支持单指左右滑动切换日期，左滑为下一周期，右滑为上一周期；必须与时间网格的纵向滚动互不干扰。

#### Scenario: 周视图滑动

- **WHEN** 用户在周视图左滑超过阈值
- **THEN** 显示下一周；右滑则显示上一周

#### Scenario: 日视图滑动

- **WHEN** 用户在日视图左滑超过阈值
- **THEN** 显示下一天；右滑则显示上一天

#### Scenario: 纵向滚动不误触

- **WHEN** 用户在周 / 日视图纵向滚动时间网格
- **THEN** 时间网格正常滚动，不发生日期切换

### Requirement: 周视图点按日期进入日视图

周视图的星期条上点击某天时，必须（SHALL）跳转到该天的日视图。

#### Scenario: 点按星期条日期

- **WHEN** 用户在周视图点击星期条上的某一天
- **THEN** 视图切换为日视图，且定位到该天

### Requirement: 与双指缩放共存

单指滑动导航必须（SHALL）与既有的双指缩放切换视图级别能力互不干扰。

#### Scenario: 双指缩放优先

- **WHEN** 用户在视图页使用双指缩放
- **THEN** 由缩放手势处理视图级别切换，单指滑动导航放弃本次手势
