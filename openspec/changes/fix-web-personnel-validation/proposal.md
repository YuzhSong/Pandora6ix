# Proposal

## Why

Web 人员管理允许重复使用身份证号和录入带符号的姓名。晋升后仍展示普通员工标签，容易误读当前岗位。

## What Changes

- 新建或修改身份证号、电话号码时检查公司内重复，批量预检同时检查同批数据。
- Web 新建人员和修改姓名时，仅接受纯汉字或纯英文字母。
- 业务负责人在 Web 人员展示中隐藏普通员工标签，并与搜索和筛选保持一致。

## Capabilities

### New Capabilities

### Modified Capabilities

- `backend-identity-directory` 增加身份证号唯一性保护。
- `organization-directory` 补充姓名、身份证号校验及角色展示规则。

## Impact

涉及 Web 人员表单、批量预检、岗位展示和后端人员写入接口。
