# Pandora 文档索引

2026-10-09：每个主题目录仅保留下表指定的最新主文档，文件名沿用组长确认命名。本次在 backend-syz 同步 dev 后整理，提交 PR 到 dev。

| 文件夹 | 最新主文档 |
|---|---|
| docs/api | [Pandora_接口设计_20261009_V0.1.md](api/Pandora_接口设计_20261009_V0.1.md) |
| docs/architecture | [Pandora_技术架构设计_20261009_V0.2.md](architecture/Pandora_技术架构设计_20261009_V0.2.md) |
| docs/database | [Pandora_数据库设计_20261009_V0.2.md](database/Pandora_数据库设计_20261009_V0.2.md) |
| docs/requirements | [Pandora_产品需求与设计文档_20261009确认修订版_V1.3.md](requirements/Pandora_产品需求与设计文档_20261009确认修订版_V1.3.md) |
| docs/testing | [Pandora_V1.3文档验证_20261009.md](testing/Pandora_V1.3文档验证_20261009.md) |
| docs/meetings | 当前没有会议文档 |

## 阅读顺序与边界

先读完整需求第二章，再评审架构、数据库及接口。需求主文档已包含[附录A：确认修订说明](requirements/Pandora_产品需求与设计文档_20261009确认修订版_V1.3.md#revision-notes)和[附录B：交互与验收设计](requirements/Pandora_产品需求与设计文档_20261009确认修订版_V1.3.md#interaction-acceptance)，不再维护重复的独立文件。

这是一版需求与设计基线，不是上线结果：Android有低保真原型、Web有基础工程、Backend有健康检查；业务API、数据库迁移及V1.3全部功能尚未实施。MySQL仍为候选。验收Planned与消息审核Reserved保持原状态，Q-01～09不得自行补齐。

## 历史与协作

- 本次移除10份旧版/重复Markdown；原件与旧草案可从Git基线 `ab32ced` 或更早提交恢复，不在当前目录另建备份副本。
- 最新5份主文档与本索引共6份Markdown；目录及.gitkeep不删除，既有OpenSpec历史记录不改写。
- 整理记录：[consolidate-latest-design-documents](../openspec/changes/consolidate-latest-design-documents/proposal.md)；业务确认依据：[reconcile-confirmed-requirements-1009](../openspec/changes/reconcile-confirmed-requirements-1009/proposal.md)。
- 后续业务实现另建OpenSpec并评审。PR由非作者审核；合并并验证后才归档，本次不自动合并dev/main。
