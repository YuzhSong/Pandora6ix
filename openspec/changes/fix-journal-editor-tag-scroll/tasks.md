## 1. Implementation

- [x] 1.1 将日期、标签和工作条目合并为一个有界滚动表单，保留固定标题和操作栏
- [x] 1.2 以实际光标矩形主动请求外层表单滚动，保留输入法组合、手动回看和中间编辑
- [x] 1.3 移除键盘弹出时隐藏日期标签的分支，打开选择器前清除输入焦点
- [x] 1.4 日志标签选择器支持设为主，操作区固定占位并沿用任务样式

## 2. Validation

- [x] 2.1 差异检查及 OpenSpec 严格校验
- [x] 2.2 Android 调试构建（含日志标签设为主）
- [ ] 2.3 设备验证全部标签、小屏、键盘、多条目及编辑已有日志
- [ ] 2.4 设备验证连续输入、换行、粘贴超过六行、手动回看和中间编辑时光标可见
- [ ] 2.5 设备验证先写长正文、键盘显示时回滚选标签及正文保留
- [ ] 2.6 设备验证切换、取消主标签及发布后主标签正确

验证记录：`git diff --check` 和 `openspec validate fix-journal-editor-tag-scroll --strict` 通过。`:app:assembleDebug` 因用户 Gradle 缓存锁文件无写权限无法启动，提权请求被用户拒绝；本次构建和设备验证尚未完成。

后续验证：改为实际光标矩形 + BringIntoViewRequester 后，获准运行 `:app:assembleDebug`，BUILD SUCCESSFUL。此构建覆盖当前所有未提交 Android 修改；设备交互验证仍未执行，不能据此确认真实设备上的自动滚动效果。

取消键盘隐藏日期标签分支后再次运行 `:app:assembleDebug`，BUILD SUCCESSFUL；`git diff --check` 与 OpenSpec 严格校验通过。先写内容后选标签的设备交互仍待实测。

日志标签选择器增加“设为主”后，`git diff --check`、OpenSpec 严格校验与 `:app:assembleDebug` 再次通过；创建和更新链路均使用重排后的首个标签生成 primaryTagId。设备提交结果仍待验证。
