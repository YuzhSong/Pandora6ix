## Context

LoFi 使用 Compose、模拟数据与五页底部导航。导图页四面板此前各用一色，展开浮层样式简单；个人事项此前缺少完整的编辑与排序能力；应用没有设置入口。全部数据为内存态演示数据，不持久化。

## Goals / Non-Goals

Goals：统一导图页视觉、让个人事项可编辑可排序、补齐基础设置并保持入口与样式一致。

Non-Goals：不引入持久化（SharedPreferences / DataStore / Room）、不连接后台、不改变四面板业务内容与任务派发审核流程。

## Decisions

- 四面板统一取用「公司十大重要事项」的暖橙色，面板定义去掉各自的 accent 字段。
- 展开查看沿用底部入口，浮层条目使用序号圆形徽标加虚线分隔，移除「演示数据」；头部以暖橙色圆点对齐其它面板抬头。
- 单条详情内层 Column 使用 `fillMaxWidth`，使序号相对组件框居中而非相对文字；移除返回按钮、抬头与副标题。
- 个人事项编辑使用内存态 `SnapshotStateList` 支撑增删改；排序使用 `detectDragGesturesAfterLongPress` 长按整行拖动，被拖行以 `graphicsLayer.translationY` 跟随手指，其余行按位移让位，松手后按 hover 目标调整列表顺序。
- 字号缩放通过 `CompositionLocalProvider(LocalDensity provides Density(density, fontScale * userFontScale))` 覆盖全局 `fontScale`；设置页复用统一的 SectionLabel / SettingsCard / SettingsRow 组件，并被「我的」页复用。

## Risks / Trade-offs

- 演示数据不持久化 → 进程结束即重置，页面保留模拟标记。
- 大字档位下底部导航文字可能溢出 → 作为设置生效的真实反馈予以保留。
- 设置行组件同时被设置页与「我的」页复用 → 后续调整样式需同时评估两处。
- 拖动排序的手感无法通过静态代码验证 → 需在模拟器人工确认落点与惯性。
