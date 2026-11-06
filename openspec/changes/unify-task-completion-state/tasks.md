## 1. Backend

- [x] 1.1 增加任务完成状态和历史数据迁移，保留真实验收归属与历史。
- [x] 1.2 实现任务级提交/验收/驳回、统一锁序、全员通知及接收人编辑保护，更新接口契约。
- [x] 1.3 添加多人同步、幂等、并发、权限、回滚与迁移回归测试。

## 2. Android

- [x] 2.1 接入任务级状态，统一任务列表和详情文案与状态色，保留待验收首页条目。
- [x] 2.2 补充状态文案和首页筛选回归测试。
- [x] 2.3 按追加要求收窄首页及展开列表备注：只显示“待验收”，不显示“未完成”，空备注不渲染。
- [x] 2.4 对照审批中心既有用词，将本次任务完成相关界面、通知、操作日志、测试及说明统一为“验收”。文案调整后两端编译、10 项相关单元测试及 OpenSpec 校验通过。

## 3. Validation

- [x] 3.1 执行 Backend 检查、Android 检查、OpenSpec 严格校验及差异检查，记录结果与发布边界。

验证记录：
- Backend：最终 `./mvnw.cmd -q -l target/shared-task-final-verify.log verify` 的全部 175 项测试通过（0 failures / 0 errors），包含 V16→V17 文件数据库迁移、多人并发提交、全员通知、验收权限、驳回重提、幂等及原子回滚。首轮完整测试发现两项旧逐人完成断言，已按本次确认规则更新后重跑通过。
- Maven 打包阶段的 zstd-jni 下载两次在尾部中断；从同一官方 Maven Central 下载并校验官方 SHA1 后，`./mvnw.cmd -q -DskipTests -l target/shared-task-package-final.log verify` 通过，生成可执行后端 JAR。该补充检查复用已通过的 175 项测试结果。
- Android：`:app:assembleDebug :app:compileDebugUnitTestKotlin` 通过（既有图标弃用及恒真条件警告）。
- Android：以 JUnit 4 直接执行 TaskCompletionTest、HomeTaskBoardTest 和 SummaryDataTest 中的状态展示方法，10 项通过。使用编译产物及缓存依赖，本地方法选择 runner 位于忽略的 app/build/ 下。直接运行整个 SummaryDataTest 时，另 5 项既有 Compose 分页测试因 JVM 缺少 android.os.Looper 无法执行；未声称 Android 全量单测通过。
- `git diff --check`、`openspec validate unify-task-completion-state --strict`、OpenAPI JSON 解析通过。
- 未执行双账号实机/模拟器联调，未发布后端或安装 Android，未执行生产数据迁移，未归档。上线前备份现有数据库，先发布后端执行 V17，再更新 Android；实机确认 A 提交后 B 收到通知及状态刷新、验收通过/驳回后的全员同步和首页显示规则。
