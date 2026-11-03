# Pandora Android

Kotlin + Jetpack Compose 客户端。构建与测试：

```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

## 卡通主题素材来源

设置 → 切换主题中的「奶油单车」「凉风习习」「农场乐园」「春日郊游」「冬日乐园」「专心致志」「童话公主」七套卡通主题，其场景插画、底部导航图标与“+”按钮（`app/src/main/res/drawable-nodpi/toon_*.webp`）裁切自番茄ToDo 的应用截图，**仅用于课程设计演示，不上线、不商用**，著作权归原作者所有。

素材由 `docs/tools/crop_cartoon_assets.py` 从本地参考截图（`素材/`，不提交）按固定坐标裁切、去背景生成；变更说明见 `openspec/changes/add-cartoon-themes`。
