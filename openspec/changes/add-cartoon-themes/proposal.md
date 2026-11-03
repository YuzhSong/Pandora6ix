## Why

课程设计中担任甲方的老师要求 Android 端参照“番茄ToDo”的卡通可爱风格，方便原有用户过渡并让界面更舒适。现有五套主题只有配色，没有插画、手绘图标和卡通描边。本变更新增七套卡通主题，原五套保留，待卡通界面验收后再决定是否删除。

## What Changes

- 新增七套卡通主题：奶油单车、凉风习习、农场乐园、春日郊游、冬日乐园、专心致志、童话公主；与原有暖米/薄荷/晴空/樱花/紫藤并存，共十二套，按账号保存方式不变。
- 卡通主题提供：配色 Token（参照素材取色并满足对比度要求）、底部场景插画、五个底部导航手绘图标、手绘“+”按钮、卡片深色描边；深色导航条（童话公主）使用白色导航文字。
- 卡通主题下底部导航使用手绘位图图标，导航名称与顺序（导图、视图、日志、AI地图、我的）不变；原五套主题继续使用矢量图标。
- 一级页面底部显示主题场景插画；视图页显示淡化插画，不遮挡日历内容。
- 主题选择弹窗增加卡通主题的插画缩略图。
- **素材来源**：插画与图标裁切自番茄ToDo 截图，仅用于课程设计演示，不上线、不商用；在 README 与本变更中注明来源。

## Capabilities

### New Capabilities
- `android-cartoon-themes`：卡通主题的配色、插画、手绘导航图标、“+”按钮与卡片描边规则。

### Modified Capabilities

无。原五套主题与 `android-design-system-implementation` 的 Token、对比度和组件规则不变。

## Impact

- Android：`ui/theme`（新增 `ThemeArt` 与七套配色）、`ui/components/PandoraPanel.kt`（卡片描边）、`MainActivity.kt`（导航图标、场景插画、主题选择）、`ui/calendar/CalendarScreen.kt`（“+”按钮）、`res/drawable-nodpi` 新增 50 个 WebP（约 410 KB）、单元测试。
- 不涉及：后端、Web、接口、数据库、业务流程。
