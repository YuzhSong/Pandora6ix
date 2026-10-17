## 日期判定

企业配置时区为权威。PATCH/DELETE 要求作者本人且 workDate 等于 identity.today(actor)。POST 要求 workDate 在 today-2 到 today；昨天、前天各仅能初次补交，提交后不能再修改或删除。旧的 published_at + 24h 判定已被用户明确替换。

## 条目和事务

PATCH 使用 upsertEntries 与 deleteEntryIds，拒绝同一条目同时删改，保持未列出的条目。删除条目前清理个人重要事项引用并递增选择版本，保留手动空集。patch/delete 先锁 app_user，与 saveSelection 同序串行化，避免选择与删除竞态。

## 客户端与验证

公司时区日期、businessToday/allowedActions 控制编辑器和卡片；服务端跨日重新校验。补交重复冲突不自动 PATCH，过期草稿不恢复为其他日期。Java HTTP 回归覆盖两个补交日期、重复发表、跨日改删、三日外拒绝与删除选择引用；Android JVM 测试覆盖公司午夜和设备时区差异。真机验收尚未完成。
