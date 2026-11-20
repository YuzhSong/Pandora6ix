# Tasks

## 1. Android

- [x] 1.1 `MainActivity` 展开浮层：事件纪面板列表截断为前 10 条
- [x] 1.2 事件纪浮层隐藏「加载更多」按钮，副标题条数按实际显示条数计算

## 2. 测试与验证

- [ ] 2.1 Android 相关单元/设备测试通过（本机无 JDK 21 toolchain，无法本地编译；改动为纯 UI 截断逻辑，待有 JDK 21 环境时验证）
- [x] 2.2 `openspec validate limit-home-company-events-to-ten --strict` 通过
