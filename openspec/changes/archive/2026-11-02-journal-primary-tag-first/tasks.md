## 1. Implementation

- [x] 1.1 日志编辑回填、选择器及展示组件统一主标签优先排序。

## 2. Verification

- [x] 2.1 检查切换、回填及无主标签排序路径，执行 Android 编译和 OpenSpec 严格校验。

验证：代码检查确认已选标签首项作为主标签、设为主立即移至首项、草稿沿用已选顺序；两个编辑入口及卡片/详情按 primaryTagId 稳定排序，无匹配时保留原顺序。Android :app:compileDebugKotlin --offline、openspec validate journal-primary-tag-first --strict、git diff --check 均通过。尚未进行手机实机验证。
