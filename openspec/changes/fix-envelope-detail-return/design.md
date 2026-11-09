## Context

`HomeScreen` 使用局部 `remember` 保存 notifications。进入任务详情或审批中心时首页退出组合，列表打开状态随之丢失。现有按账号隔离的 SaveableStateHolder 能保留首页滚动状态，但无法保留普通 remember 值。

## Goals / Non-Goals

**Goals:** 从消息目标返回恢复列表；明确关闭或切换底栏后保持关闭；账号间隔离。

**Non-Goals:** 不更改推送打开策略、已读标记接口、目标解析和其他任务入口。

## Decisions

- 将消息开关提升至 `PandoraApp`，使用 `rememberSaveable(currentUser?.id)`；通过受控参数和回调传入 `HomeScreen`。该状态在首页退出组合时仍存在。
- 不恢复局部 rememberSaveable 的旧方案，因为它会在切换底栏后回首页时重新打开抽屉；底栏点击明确清除外层开关。
- 信封设置开关为 true；关闭按钮、遮罩、列表系统返回均设置 false。打开任务或审批仅清除消息定位，不关闭列表；详情页自己的返回保持现有逻辑。
- 推送事件不把开关设为 true；账号变化重置开关。复用既有 SaveableStateHolder 与 rememberLazyListState 保留列表滚动状态。

## Risks / Trade-offs

- 状态提升后底栏切换可能残留 → 所有底栏点击主动关闭。
- 切换账号恢复旧抽屉 → 开关以 currentUser.id 为输入，账号变化即重置。
- UI 返回行为需设备验证 → 执行构建和源代码路径核对，明确记录尚未执行的实机验收。
