## 1. 详情排版

- [x] 1.1 分离事件纪详情标题和正文，使用 24sp 标题与 16sp 自动换行正文，保护空正文和其他详情展示。

## 2. 验证

- [x] 2.1 运行 Android debug 构建、差异空白检查和 OpenSpec 严格校验，记录结果及视觉验收边界。

验证结果：
- `android/gradlew.bat :app:assembleDebug`：通过（已有弃用及恒真条件警告）。首次因用户 Gradle 缓存目录权限受限失败，获授权后重跑成功。
- `git diff --check`：通过。
- `openspec validate improve-home-event-detail-typography --strict`：通过。
- 未做设备视觉验收；待人工确认长标题、多行正文、空正文及系统大字体下的完整展示与滚动。
