## 1. 素材

- [x] 1.1 裁切四套主题的场景插画、导航图标、“+”按钮与视图页插画，输出到 `res/drawable-nodpi`
  - `docs/tools/crop_cartoon_assets.py` 可从 `素材/` 重新生成，共 29 个 WebP 约 241 KB；“+”按钮在奶油单车取自首页大图，其余取自主题预览。
- [x] 1.2 README 注明素材来源与课程演示用途（`android/README.md`）

## 2. 主题与组件

- [x] 2.1 新增 `ThemeArt`，`AppTheme` 增加 `art`；四套卡通配色与 `cardBorder` Token
- [x] 2.2 `PandoraPanel`/`PandoraItemContainer` 在卡通主题下使用 2dp 深色描边（`pandoraCardBorder()`）
- [x] 2.3 底部导航在卡通主题下使用手绘图标与 `navBand` 背景
- [x] 2.4 一级页面底部场景插画；视图页淡化插画（奶油单车使用素材自带淡彩插画）
  - AI 探索馆首页在卡通主题下底部多留 160dp，滚到底时免责声明不压在插画上。
- [x] 2.5 日历“+”按钮在卡通主题下使用手绘按钮
- [x] 2.6 主题选择弹窗显示九套主题与卡通缩略图，弹窗可滚动；说明文字改为“更改界面外观（配色、图标与插画）”

## 3. 验证

- [x] 3.1 单元测试：九套主题 key/顺序、对比度、卡通主题资源完整
- [x] 3.2 构建与全部 Android 单测通过（128 项）；`openspec validate add-cartoon-themes --strict`
- [x] 3.3 模拟器截图：四套卡通主题的导图、视图、AI地图（农场乐园为导图、视图、日志），暖米回归，主题选择弹窗
