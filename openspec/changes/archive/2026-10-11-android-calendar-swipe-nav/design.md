# 设计说明

## 手势实现

新增 `Modifier.swipeNavigate(mode, yearGridState, scope, onAdvance)`，与既有 `pinchToZoom` 一起挂在视图页内容区 `Box` 上：

- 在 `PointerEventPass.Initial` 通道用 `awaitEachGesture` 观察，不参与布局。
- 累计单指位移，超过 48dp 阈值且主轴位移大于副轴时判定方向并触发一次 `onAdvance(forward)`；触发后消费剩余事件，避免手指抬起时误触子项 `clickable`。
- 出现第二根手指或事件已被消费（双指缩放接管）时立即放弃本次手势，两种手势互不干扰。
- 通过 `rememberUpdatedState` 持有 `onAdvance` 回调，`pointerInput` 仅以 `mode` 为 key，避免切换日期后回调闭包读到旧游标。

## 与既有滚动的冲突处理

- **周 / 日视图**：手势只认横向主轴，时间网格与全天任务区的纵向滚动不受影响。
- **年视图（连续滚动）**：月份网格本身是 `LazyVerticalGrid`，年份切换完全由网格滚动驱动，`swipeNavigate` 对年视图直接不参与（无分片手势）。网格内容覆盖锚点年份前后各 12 年（24 年 × 12 月 = 288 个月卡片），通过 `snapshotFlow` 监听首个可见 item：
  - 实时上报其所属年份（驱动日期行标题与「回到今天」按钮状态），经 `onCursorChange` 写回的是等值字符串日期，不会引发多余重组；
  - 滚动接近列表边缘（顶部年份距起点 ≤1 年或距终点 ≤2 年）时静默重锚（`anchorYear = 当前年份`）并 `scrollToItem` 保持视觉位置，实现近似无限滚动；
  - 外部大幅跳转（「回到今天」等，与最近上报年份相差 ≥2 年）时重锚并定位到目标年一月。
  - `LazyGridState` 初始 `firstVisibleItemIndex = 144`（锚年一月），锚点用 `remember` 保存、不随 `cursor.year` 直接重算，避免滚动中重组导致位置跳变。
- **月视图**：无滚动子组件，直接累计纵向位移触发。

## 周视图点按跳转

星期条 `WeekDates` 的点按回调在周视图级别下追加 `onModeChange(CalendarMode.DAY)`，与 `onCursorChange(date)` 一起完成「定位到该天 + 进入日视图」。

## 不做的事

- 不移除双指缩放切换级别（既定能力，另行评审）。
- 不改动 `CalendarScreen` 对外签名与 `CalendarLayout` 的纯函数。
