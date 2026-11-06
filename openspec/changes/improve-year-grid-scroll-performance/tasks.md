## 1. Optimize

- [x] 1.1 使用月份级缓存绘制日期，保留独立点击语义和原有布局、标记及导航。

## 2. Validate

- [x] 2.1 运行年视图导航回归测试、Android 构建和 OpenSpec 严格校验。
- [x] 2.2 保留数据安装，截图检查日期与点击语义，按基线动作复测并记录 debug 性能边界。

验证记录：
- Android `:app:assembleDebug :app:compileDebugUnitTestKotlin` 通过。YearGridWindowTest + CalendarNavigationTest 直接 JUnit 运行 8 项通过。
- 已保留数据安装到连接手机；截图确认日期、今日强调和任务圆点；UI hierarchy 检出 208 个独立的完整日期点击入口；「回到今年」及月份标题进入月视图均通过。
- 设备帧数据保存在忽略的 `android/app/build/year-scroll-*.txt`：同手机、debug、八次 `(540,1820)→(540,740),350ms` 滑动。原采样 314 帧，P50/P95/P99=17/65/97ms；仅缓存绘制的首轮 320 帧=16/69/117ms，未显示长帧改善；加入滚动停稳提交并新装后的冷测 292 帧=17/93/350ms；预热两轮分别 396 帧=15/31/57ms、420 帧=14/25/36ms。
- 不能将原采样与新装冷测/预热结果视为严格因果对比：年份范围、JIT、后台任务和设备状态未完全锁定；未测 release，也不承诺稳定 60fps。结果支持继续做 release profiling，不能归因全部改善于代码修改。
- 日期绘制遵循字体缩放，并镜像 RTL 列位置以匹配点击布局；未额外修改系统字体或启用 TalkBack 做完整验证。
- 测量临时将 USB 连接常亮设置从 0 改为 USB，结束已恢复为 0。未修改业务数据，未提交或推送。
