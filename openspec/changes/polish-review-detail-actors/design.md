## 为什么姓名要由后端给

`submittedSnapshot` 是提交时的不可变内容，里面只有 `recipientIds`（UUID）；`TaskReviewDto` 也只有 `submittedByUserId` / `reviewerUserId`。客户端唯一的"ID → 姓名"途径是 `GET /tasks/recipient-candidates`，但那是**我的管理范围内的活跃用户**，覆盖不全（已停用/已离职的人不在列表里，上级也不在里面），而且要额外拉全量候选页。

所以姓名由后端在 `reviewView()` 里补：

```java
private Map<String, String> nameIndex(IdentityService.Actor a) {
    // SELECT id,display_name FROM app_user WHERE enterprise_id=?  → Map<userId, displayName>
}
```

- `list()` 构建一次索引、`detail()` 构建一次，避免每页 20 条审批各查一次用户表（原来是 N+1）。
- 执行人取**任务当前执行关系**（`task_assignment`），而不是快照里的 `recipientIds`——批准后真正执行的就是这些关系；顺序按查询结果，客户端不去重排序以外的加工。
- `recipients[]` 带 `id` 而不只是姓名：客户端在姓名缺失时回退 `成员 <ID 后四位>`。

契约同步走 `scripts/generate-openapi.py`：`TaskReview` schema 增加 `submittedByName` / `reviewerName` / `recipients`（复用新增的 `reviewActor` 小对象），重跑脚本生成 `openapi.json`（+46 行）。新增字段都是可选的，旧客户端 `ignoreUnknownKeys = true` 不受影响。

## 详情分区结构（`ui/review/ReviewScreen.kt`）

```
任务信息   标题 + 决策徽章 / [派发任务] ★★☆☆☆ 低 / 说明
时间安排   日期 2026/10/21 – 2026/10/23   时间 22:30 – 02:15
人员       ⬤ 请求人 张三（第 2 次提交 · 10-21 14:30）
           ⬤ 审批人 李四
           执行人  [⬤ 张三] [⬤ 王五]
标签       [● 客户端] [● 迭代 · 主]
审批结果   已批准 / 审批人 李四 / 批注 … / 决定时间 …
```

新增组件：

- `SectionCard(title, content)`：12sp 小标题 + 主题卡片（`WarmCard`，圆角 16），`content` 为 `ColumnScope`，内部统一 8dp 间距。
- `AvatarDot(name, size, fontSize)`：主题强调色浅底圆 + 姓名首字（与"我的"页头像语义一致，但不需要真实照片）。
- `PersonRow(label, name, sub)`：34dp 头像 + 角色 + 姓名 + 补充信息（提交次数/时间）。
- `PersonChip(name)`：20dp 头像 + 姓名胶囊，用 `FlowRow` 排列（`@OptIn(ExperimentalLayoutApi::class)`），多人自动换行。
- `MetaChip(text)`：类型等次要信息的小标签。

重要程度复用了 `ui/theme/Theme.kt` 已有的 `priorityMeta(level)`（颜色 + 文案：低/普通/重要/紧急），与任务详情、日历任务块保持一致，不新增第二套语义（Q-05 仍未定方向，这里只做视觉呈现，不推断优先级高低方向）。

## 回退策略

| 情况 | 展示 |
|---|---|
| 后端返回 `displayName` | 姓名 |
| 无 `displayName`（旧数据/已停用/跨企业） | `成员 <ID 后四位>` |
| 后端未返回 `recipients`（旧后端） | 快照口径：`本人执行` / `接收人 N 人` |

第一行是"数据没到"的典型症状：如果执行人显示 `本人执行` / `接收人 N 人`，基本可以断定跑着的后端还是旧的（`recipients` 字段不存在），或 APK 没重装。

## 执行人取自快照，而不是当前执行关系

一开始 `recipients()` 查 `task_assignment`（任务**当前**执行关系）。这有两个问题：

- 审批详情展示的是**提交时不可变快照**，任务改派后再看旧审批，执行人会变成新执行人，与快照里的内容对不上
- 执行关系缺失/变化时 `recipients` 会空 → 退化成 `接收人 N 人`，也就是"执行人没有名字"

改成从 `submitted_snapshot.recipientIds` 取（快照里一定有请求当时的执行人 ID），只有当快照确实没有这个字段的历史数据才回退 `task_assignment`。快照优先后，只要这条申请当时指定了执行人，姓名就一定拿得到。

## 验证

- 后端：`reviewDetailExposesRequestorAndRecipientNames` 断言 `submittedByName=employee`、`reviewerName=lead`、`recipients[].displayName`；`reviewDetailExecutorsCarryNamesAndFollowSubmittedSnapshot` 断言派发两人时逐个返回姓名+职位，并在删除 `task_assignment` 后旧审批仍显示快照里的两人；全量 `mvn test` 95 项通过。
- Android：`:app:compileDebugKotlin` 通过；真机验收看分区层次与姓名是否正确。
