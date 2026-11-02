## 1. Implementation

- [x] 1.1 保存首页消息列表状态，进入消息目标后返回恢复列表，并支持系统返回关闭列表。

## 2. Verification

- [x] 2.1 检查任务及审批返回、普通入口与关闭路径，执行 Android 编译、OpenSpec 校验。

验证：代码检查确认任务与审批返回复用首页保存状态，消息打开时不清除抽屉状态，列表滚动状态在加载期间保持注册；关闭按钮、遮罩及系统返回均关闭列表，日历入口保持原逻辑。Android :app:compileDebugKotlin --offline、openspec validate notification-detail-return-list --strict、git diff --check 均通过。尚未进行手机实机验证。
