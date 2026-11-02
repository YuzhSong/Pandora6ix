## 1. Implementation

- [x] 1.1 日历任务添加驳回标注，统一汇总和详情审批状态文案。

## 2. Verification

- [x] 2.1 检查各日历展示路径及审批状态判断，执行 Android 编译和 OpenSpec 严格校验。

验证：日历任务条、时间网格、重叠任务选择列表及周任务列表均使用 REJECTED 判断添加标记；汇总和详情通过 reviewStatusLabel 显示“被驳回”。Android :app:compileDebugKotlin --offline、openspec validate created-task-rejection-label --strict、git diff --check 均通过。尚未进行手机实机验证。
