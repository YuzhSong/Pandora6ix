# Proposal
## Why
人员创建需要手填密码，自由文本职位与角色标签重复且可能不一致。用户要求自动初始密码和固定业务职位。
## What Changes
- 单人及批量创建自动生成随机初始密码，保留账号交付。
- 批量职位选择映射实际业务角色，并校验组织和上级。
- 统一业务角色文案为创始人、部门老总、团队长、员工，移除重复职位输入和展示。
## Capabilities
### New Capabilities
无。
### Modified Capabilities
- `web-admin-shell`: 简化人员创建和角色展示。
## Impact
Web 创建、批量导入、人员资料及岗位调整；沿用现有 API，保留历史职位数据。
