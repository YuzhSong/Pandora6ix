## Why

组长要求 docs 每个目录只保留指定最新主文档，避免旧草案与确认版混用。当前重复第二章、修订说明和交互设计分散，需要在移除重复文件前保留有效依据和验收设计。

## What Changes

- 保留指定的 API0.1、架构A0.2、数据库DB0.2、完整需求V1.3、V1.3验证记录及 docs/README.md，文件名不变。
- 将当前确认修订说明和交互验收设计并入完整需求的附录；第二章业务正文与编号不改变。
- 删除10份旧版或重复Markdown文件，修正当前文档链接与索引。旧文件可从Git历史恢复。
- 从 origin/backend-syz 创建本地跟踪分支，同步 origin/dev 后完成文档整理，提交PR到dev；不自动合并或提前归档。

## Capabilities

### New Capabilities

- `latest-documentation-layout`: 最新文档唯一入口、信息保留、有效引用与历史恢复约定（文档管理，不是新增产品功能）。

### Modified Capabilities

无产品规格变更；不重写已有OpenSpec历史。

## Impact

仅 docs 和本次 OpenSpec 记录。Android/Web/Backend业务代码、API契约、数据库业务规则和依赖不变。历史文档链接被替代，需要以最新索引为入口。
