## 1. 后端

- [x] 1.1 Flyway V16 新增 `work_log_comment` 表与索引；删除日志时删除留言
- [x] 1.2 日志详情返回 `author`（姓名、职位、组织、部门）
- [x] 1.3 留言列表/新增/删除接口与权限；`Contracts.JournalComment`
- [x] 1.4 更新 `openapi.json`；集成测试覆盖上级留言、作者禁止、无关同事 404、删除他人留言、删除日志级联
  - `JournalCommentIntegrationTests` 4 项；`TaskWorkflowMigrationTests` 的迁移数量随 V16 由 5 改为 6；留言时间按 `view` 规则输出 UTC ISO（联调时发现直接输出会被客户端误读 8 小时，已修正并加断言）。

## 2. Android

- [x] 2.1 模型与 `JournalRepository` 接口
- [x] 2.2 日志详情顶部作者姓名、部门、职位
- [x] 2.3 “上级留言”区：列表、空状态、大输入框、发送状态、删除自己的留言

## 3. 验证

- [x] 3.1 后端测试、Android 构建与单测通过；`openspec validate --strict`
  - 后端 169 项全部通过；Android 138 项全部通过。
- [x] 3.2 模拟器验证：上级留言、作者查看
  - 本地隔离后端：陈组长打开李思思日志，顶部显示“产品研发部 · 移动端小组 | Android 工程师”，发送留言后列表即时出现并可删除；李思思查看为只读，无输入框和删除按钮。
