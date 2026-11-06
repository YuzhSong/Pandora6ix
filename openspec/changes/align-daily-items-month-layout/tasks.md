## 1. 布局实现

- [x] 1.1 提取并共享公司月视图的容量及行高尺寸，保持公司页行为。
- [x] 1.2 将每日事项圆点替换为固定三道文字预览和独立的 +N 行，保留当天清单入口及完成状态。

## 2. 验证

- [x] 2.1 执行 Android 构建与单元测试，检查差异及布局边界。
- [x] 2.2 执行 openspec validate align-daily-items-month-layout --strict。

验证记录：`:app:assembleDebug` 成功；`:app:testDebugUnitTest` 首次因中文用户目录编码导致 GradleWorkerMain 无法启动，使用临时参数 `-Dorg.gradle.jvmargs="-Xmx2048m -Dfile.encoding=GBK" --max-workers=2` 重跑成功，未改动项目编码配置。`git diff --check` 和 OpenSpec 严格校验通过。代码检查覆盖空日期、恰好三条、超过三条及删除后溢出计数边界；未进行设备上的视觉验收。
