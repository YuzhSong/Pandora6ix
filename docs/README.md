# Pandora 文档索引

2026-10-09；存放分支LoFi。文档修订阶段未修改三端业务代码或建库。现按组长后续授权，将已有Android低保真代码、最新文档及对应OpenSpec以LoFi → dev的PR送审；不在交付时补做V1.3业务功能。

## 1. 当前评审基线

先读需求及修订说明，再评审设计；旧V1.2与A0.1/DB0.1只作历史，不混用被替代规则。

- [完整产品需求与设计文档 V1.3](requirements/Pandora_产品需求与设计文档_20261009确认修订版_V1.3.md)
- [独立第二章 V1.3](requirements/Pandora_第二章_20261009确认修订版_V1.3.md)：与完整版本第二章一致。
- [确认修订说明 V1.3](requirements/Pandora_20261009_确认修订说明_V1.3.md)：20条意见、9项答复、旧新对照、未决Q-01～09。
- [交互与验收设计 V0.1](requirements/Pandora_交互与验收设计_20261009_V0.1.md)：导图单条/编辑、直接任务详情、日报、标签、设置、未来消息审核。
- [技术架构 A0.2](architecture/Pandora_技术架构设计_20261009_V0.2.md)：三端边界、两种任务完成流程、事务/时序与实施计划。
- [数据库 DB0.2](database/Pandora_数据库设计_20261009_V0.2.md)：日报头/工作条目、最多十项选择、企业标签、完成来源、预留消息，共18核心＋1预留逻辑实体。
- [接口 API0.1](api/Pandora_接口设计_20261009_V0.1.md)：首版业务契约、请求响应/权限/版本/分页/错误；消息审批标记Reserved。
- [本轮文档验证](testing/Pandora_V1.3文档验证_20261009.md)：验证方法/结果与运行未验证边界。

这是需求确认与设计基线，不是运行上线：MySQL仍候选；业务API未实现；审核预留没有UI/服务。文档内业务未决不得用LoFi默认行为补齐。

## 2. 历史输入与上一版（保留原件）

- [用户提供完整V1.2修订草案](requirements/Pandora_产品需求与设计文档_20261009修订草案.md)
- [用户提供独立第二章](requirements/Pandora_第二章_20261009需求修订草案.md)
- [用户提供修改说明与待确认](requirements/Pandora_20261009_修改说明与待确认事项.md)
- [上轮审查记录](requirements/Pandora_20261009_需求审查记录.md)
- [架构A0.1历史草案](architecture/Pandora_技术架构设计_20261009.md)
- [数据库DB0.1历史草案](database/Pandora_数据库设计_20261009.md)
- [上轮文档验证](testing/Pandora_设计文档验证_20261009.md)

旧版待确认项有的已在V1.3解决；不要直接采用“同日多篇”“禁止员工个人创建”“自动月周日下钻”等旧规则。

## 3. 实施与协作约定

1. Android、Web、Backend独立工程存在，不等于三端业务完成。管理员维护端与Android业务端共用Backend权限/数据规则。
2. 本次OpenSpec：[reconcile-confirmed-requirements-1009](../openspec/changes/reconcile-confirmed-requirements-1009/proposal.md)。交付任务为文档，不伪装功能已验收。
3. 上轮文档变更：[document-requirements-architecture-database](../openspec/changes/document-requirements-architecture-database/proposal.md)。不擅自改已有Android变更。
4. 业务代码、数据库迁移、依赖和接口实现须另建实施OpenSpec并评审；标签日报粒度、最高级审核/驳回重提等先明确。
5. 组长现已授权提交并推送LoFi、创建以dev为目标的PR；不直接更新dev/main、不合并、不提前归档。历史验证记录中的“未提交/未推送”描述当时设计交付阶段，并非当前Git状态。
6. Mermaid为源码，未渲染验收；本轮未做Gradle/浏览器/模拟器/业务API/数据库测试。字体、颜色、按钮与日历容量不反推正式业务约束。
