# Tasks: journal-edit-window

## 1. 后端

- [x] 1.1 Contracts.JournalPatch 增加 deleteEntryIds；JournalService.patch 删除条目并校验与 upsert 不重叠
- [x] 1.2 JournalService 24h 窗口校验（patch/delete 共用）：超窗 409 JOURNAL_EDIT_WINDOW_EXPIRED
- [x] 1.3 JournalService.delete + JournalController DELETE /work-logs/{id}（作者限定、级联删除）
- [x] 1.4 openapi.json 同步：DELETE 端点、JournalPatch.deleteEntryIds、错误码
- [x] 1.5 集成测试：窗口内改/删成功、超窗 409、非作者 403、deleteEntryIds 生效且不与 upsert 重叠

## 2. Android

- [x] 2.1 JournalModels/JournalRepository：JournalPatchDto.deleteEntryIds、delete(id)
- [x] 2.2 ApiClient.friendlyMessage：JOURNAL_EDIT_WINDOW_EXPIRED 中文提示
- [x] 2.3 编辑器日期改为最近三日 chip 选择（今天/昨天/前天），按所选日期加载或新建
- [x] 2.4 编辑器既有条目支持逐条删除（deleteEntryIds 随 PATCH 提交）
- [x] 2.5 日志卡片 24h 窗口内提供编辑/删除入口（删除带确认）；超窗隐藏
- [x] 2.6 条目展示真实时间（recordedAt → HH:mm）

## 3. 验证

- [x] 3.3 审查回归：删除已选条目/日报时清理选择引用并递增版本，保留手动空集

- [x] 3.1 前后端编译通过；openspec validate --strict
- [ ] 3.2 场景：三日创建、24h 内改/删、超窗拒绝、补充时不追加新条目
