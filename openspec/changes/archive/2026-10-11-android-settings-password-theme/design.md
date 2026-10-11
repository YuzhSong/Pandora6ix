# 设计说明

## 主题机制

- `AppTheme(key, label, background, backgroundDeep, dashboard)` 预置 5 套：暖米（默认，即既有 Cream/CreamDeep/WarmDashboard）、薄荷、晴空、樱花、紫藤。
- 当前主题以 key 存 `rememberSaveable`，在 `PandoraApp` 解析后通过 `staticCompositionLocalOf` 的 `LocalAppTheme` 下发，避免层层传参。
- 生效点：`Scaffold.containerColor`、`NavigationBar.containerColor` 与选中指示色、内容 `Surface`、导图页与设置页仪表盘背景（原硬编码 WarmDashboard）。弹窗/卡片继续沿用暖色组件（WarmCard 等），保证前景组件视觉稳定。
- 主题选择对话框每项展示色样圆（background 填充 + backgroundDeep 描边）+ 名称 + 单选按钮，点击即应用并关闭。

## 修改密码

- 对话框三个密码输入框（`PasswordVisualTransformation`），确定时按顺序校验：当前密码非空 → 新密码 ≥ 6 位 → 两次一致；失败原因内联展示，成功后关闭并 toast「密码已修改（演示）」。
- 低保真原型不接真实鉴权，仅演示交互与校验规则。

## 备选方案

- 逐页传参替代 CompositionLocal：侵入面大（HomeScreen/SettingsScreen/HomeItemDetailScreen 签名都要改），弃用。
- 主题持久化到本地存储：原型阶段无持久层，用 rememberSaveable 会话级即可，留待后续接真实设置存储。
