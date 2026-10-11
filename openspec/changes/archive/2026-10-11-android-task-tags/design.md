# 设计说明

## 标签模型

- 标签目录放在 `MockData.taskTags`（`TaskTag(name, color: Long)`，8 个预置标签、Material 300 级浅色，保证 Ink 深色文字可读），颜色以 ARGB Long 存储，UI 侧 `Color(long)` 转换，避免 mock 层依赖 Compose。
- `WorkTask` 追加带默认值的 `tags: List<String>`，不破坏既有构造调用；首个元素即主标签。顶层 `taskTagColor(name)` 做名称 → 颜色查找。
- 演示任务预置标签（研发/产品/会议等），使视图着色立即可见。

## 选择交互

- 任务填写弹层在「重要程度」与「接收人」之间新增「标签」区：已选标签横向滚动排列为主胶囊（填充标签色 +「主」标记）与次胶囊（标签色描边），点击胶囊即取消；尾部「选择标签」按钮弹出目录。
- 目录弹窗逐行列出标签（色点 + 名称），点击切换选中；已选行右侧显示「主标签 / 次标签」与对勾。主次由选中顺序决定（先选为主），取消主标签后剩余首项递补——无需额外的"设为主"操作，交互最简。
- `tags` 计入 `dirty` 判断（未保存关闭需确认），随 `onSave` 一并写入 `WorkTask`。

## 着色链路

`CalendarScreen.taskColor(task)` 改为：主标签颜色优先、无标签回退 `colorIndex` 色板。该方法被时间网格任务块、`DateLane`（全天条 / 月视图条）复用，年视图日程圆点改为取当天首个任务的主标签颜色（原为固定 WarmOrange，尺寸 3dp→4dp 以显色）。

## 详情页

- 视图点击任务 → `onOpen` → `detail` 路由到 `DetailScreen`，该链路为既有行为，本次确认无需改动。
- 移除 `DetailScreen` 尾部的 `DetailCard("当前状态", "模拟数据 · 仅用于线下讨论")` 及其容器分支，其余卡片不动。
