## 1. 页面调整

- [x] 1.1 导图命名、移除总览刷新、压缩条目间距。
- [x] 1.2 修复视图菜单与内容高度边界。
- [x] 1.3 日志仅保留工作日志，移除任务管理交互。
- [x] 1.4 完善我的基础资料布局。

## 2. 验证

- [x] 2.1 编译 APK、OpenSpec 严格验证。
- [x] 2.2 模拟器验证五页及日周月菜单，记录结果。

验证记录（2026-10-09）：使用 C:\Users\34755\.jdks\jbr-21.0.11 编译 assembleDebug 成功；OpenSpec strict、git diff --check 通过。APK 安装成功，am start -W 报告 COLD 启动成功。UI hierarchy 可见导图及四个面板，面板高度为 951/952px（像素取整）。当前模拟器截图黑屏且注入点击未可靠更新 hierarchy，dumpsys input 出现 FocusedWindows none / NO_WINDOW，尚未完成日周月、日志、我的的实际点击验证；本项保持未完成。Studio 内置 JBR 25 与本工程现有 Gradle 不兼容，本次使用现有 JBR 21，未修改机器全局配置。

补充验证（同日）：在模拟器已停止后使用 -no-snapshot-load -no-snapshot-save 冷启动 Pixel_9a，重新安装同一 APK，画面恢复。实际点击验证周视图菜单、切换日视图、日视图再次展开菜单切换月视图成功；我的页显示个人与组织资料；日志页显示今日日志和历史日志；AI 地图显示规划中；返回导图并滑动个人重要事项面板。未执行 Wipe Data，未修改 AVD 配置。此前的黑屏及点击阻塞已恢复，本项完成。
