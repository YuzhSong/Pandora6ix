## Context

TaskSummaryFlow 使用固定 TopAppBar、单独 TabRow 和表格提示，外层 MainActivity 的 Scaffold 始终显示底部主导航。Surface 应用了 Scaffold padding，但未消费对应 insets，子级 TopAppBar 可重复应用系统栏边距。横屏高度有限，固定布局明显挤压表格。

## Goals / Non-Goals

**Goals:** 横屏把可用高度优先分给表格；返回、范围切换、刷新和旋转操作可达；竖屏原有布局及表格状态保留。

**Non-Goals:** 不修改任务接口、列定义、权限、数据分页或其他页面的导航布局。不强制隐藏系统手势/导航区域。

## Decisions

- 依据实际 LocalConfiguration 方向，而非横屏按钮的锁定标志，决定紧凑布局及底部主导航是否显示，支持手动旋转和系统旋转。
- 汇总页显式使用 safeDrawing 作为外层 Scaffold 内容边距，并消费已经应用的 padding；自定义横屏工具栏不再添加系统栏边距。
- 横屏工具栏采用单行 48dp：返回、任务汇总标题、当前范围下拉、恢复竖屏/横屏按钮和刷新。保持 48dp 操作触区，表格占其余高度，不额外显示三标签行和滑动提示。
- 竖屏仍使用原标题栏及三个范围标签；布局抽为 TaskSummaryLayout，以便在短高度容器下独立验证真实控件和表格区域。
- 复用现有 SaveableStateHolder、分页器及 LazyListState，保证切换方向或从详情返回不丢失范围和滚动状态。

## Risks / Trade-offs

- 横屏主导航暂时不可见 → 保留工具栏返回和系统返回，关闭汇总后自动恢复主导航。
- 系统栏与刘海遮挡 → 使用 safeDrawing 一次性避让，保留系统导航区域。
- 设备安装测试应用可能被手机拒绝 → 如实记录设备验证范围，不覆盖用户正式应用。
