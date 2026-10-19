## Why

任务接收人目前只有单层部门分组，PR #16 又改用显示名作为分组键，无法表达部门下的团队，还会合并同名的不同组织。用户要求按真实组织关系逐级展开。

## What Changes

- Android 派发任务接收人按部门、团队、人员逐级展开；部门直属人员直接显示在部门下。
- 使用服务端 departmentId / orgUnitId 标识组织，显示名仅用于展示；同名组织保持独立。
- 保留默认折叠、搜索命中路径自动展开、多选与选中后清空检索；候选范围和分页沿用现有后端权限。
- 旧响应缺少部门标识时按已知组织 ID 展示单层，不凭名称拼接未知父子关系。
- 保留现有日报按业务日期判断的规则与标签颜色修复，纠正 PR #16 的过时描述。

## Capabilities

### New Capabilities

- `android-recipient-hierarchy`: Android 接收人选择的真实组织树、身份稳定性与搜索/多选交互。

### Modified Capabilities

无。

## Impact

仅 Android 接收人展示与纯 Kotlin 分组测试，以及本次 OpenSpec 文档。复用当前候选 DTO/API，不新增接口、数据库表、依赖或权限；更新现有 PR #16，保留待审阅状态，不合并 #15 或 #16。
