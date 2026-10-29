## Why

审批中心角标仅统计待我审批，提交人无法从入口发现申请已通过或被拒绝。

## What Changes

- 将本人提交且尚未查看的通过、拒绝结果计入审批中心及“我的”角标。
- 在“我提交的”列表标注未读结果，进入对应详情后清除该结果提示。
- 按账号持久化本机已读记录，前台定期刷新并完整遍历分页。

## Capabilities

### New Capabilities
- `review-result-badges`: 审批结果未读提示及已读管理。

### Modified Capabilities
无。

## Impact

仅修改 Android 审批中心、入口角标与本地已读存储；复用现有审批查询接口，不增加后端接口或数据表。
