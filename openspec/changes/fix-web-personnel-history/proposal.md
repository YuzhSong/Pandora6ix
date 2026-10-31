# Proposal

## Why
岗位调整只更换上级时组织不跟随，人员历史缺少上下文，负责人选项无法区分部门，生命周期操作重复写账号日志。

## What Changes
- 提供普通员工随上级转组的明确选项，保留手动组织选择。
- 负责人候选显示组织路径；历史显示完整快照，旧名称缺失时独立标记当前目录参考。
- 生命周期复用人员校验但只写一条语义正确的操作日志。

## Capabilities
### New Capabilities
- `personnel-adjustment-presentation`: 转组、候选与历史的可读展示及单次生命周期审计。
### Modified Capabilities
无。

## Impact
Web 人员岗位弹窗、历史展示、后端 DirectoryService 与人员生命周期调用及回归测试。无数据库和 API 变更。
