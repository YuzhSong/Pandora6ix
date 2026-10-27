# 验证记录（2026-10-09）

## 构建与自动检查

- JBR 21.0.11：`gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain` 成功。
- CalendarLayoutTest：6 项测试，0 失败、0 错误。覆盖四/五/六周及连续 48 个月的日期完整性、月底切月钳制、跨周连续条、重叠分行和优先级、自定义跨午夜时段、非法区间及角色权限值。
- `openspec validate android-calendar-task-entry --strict` 通过；`git diff --check` 无空白错误（只有 Git CRLF 提示）。

## Pixel 9a API 35 实际运行

- 已安装、启动 APK；菜单可选择日、周、月。
- 月视图点击日期进入周；周视图点击任务进入当天日视图；日视图进入详情，系统返回键返回日历。
- 2026 年 9 月仅显示至 10 月 4 日；向前切六个月到 2026 年 3 月，六周全部显示，无整页滚动；回到今天返回演示基准月份。
- 创建 Demo_Task，选择接收人，自定义开始 11:00、结束 10:00，保存被拒绝且提示可见；改为结束 12:00 后保存，周、日、详情均一致，详情状态为草稿（演示）。
- 创建 All_Day_Check，日期 9/19—9/20、5 星、多选接收人；人员列表可滚到跨级员工；保存后月历出现连续条并优先排布。
- 关闭有内容的表单出现放弃确认；继续填写保留内容，系统返回也触发确认；放弃后再次打开表单为空。
- 日期、时间选择器可实际操作；键盘收起后可继续填写。
- 日视图只有任务，不含工作日志；全天与定时任务分区、重叠时段并排。
- 已截取月、周、日及任务填写面板。测试草稿通过重启应用清空，未加入正式模拟数据。

## 环境观察与限制

- 原模拟器图形渲染偶发保留旧帧/弹层空白；用 `-no-snapshot-load -no-snapshot-save -gpu swiftshader_indirect` 冷启动完成走查。未 Wipe Data、未更改 AVD 持久配置。
- 普通员工权限通过枚举测试与 UI、表单保存、App 写入三处检查验证；当前运行角色仍是模拟部门负责人，未声称测试真实登录/服务端鉴权。
- 本地任务只存内存，重启清空；不连接后端，不实现正式派发或审核。演示“今天”仍为 2026/9/19。
- 未进行真机、多机型、不同字体缩放和屏幕旋转完整测试。

## 后续环境修复（2026-10-09，经用户同意）

- Studio 再次启动原配置后，系统桌面 Pixel Launcher 出现无响应；模拟器内安装包与本地最新版 SHA-256 不同。
- 停止 Pixel_9a 后，将原 config.ini 备份至 `C:\Users\34755\.android\avd\Pixel_9a.avd\config.ini.codex-backup-20261009`。
- 持久设置 `hw.gpu.mode=software`、`fastboot.forceColdBoot=yes`、`fastboot.forceFastBoot=no`；未删除快照、未 Wipe Data。
- 使用隐藏独立窗口的 gRPC 启动方式，Studio 日志确认重新连接；未用命令行覆盖 GPU 和 cold boot 设置，启动日志确认 SwiftShader，hardware-qemu.ini 确认软件后端及 cold boot。
- 重新构建、安装最新 APK，安装包 SHA-256 与本地完全一致；实际检查导图、日周月菜单和添加任务弹层，未出现 AndroidRuntime 崩溃记录。
- 此次仅修复本机模拟器配置并补充验证记录，没有修改业务代码。
