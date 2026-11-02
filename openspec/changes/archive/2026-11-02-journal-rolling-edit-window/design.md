## Context

服务端 journalView 与写请求共享创建当天判定；Android 提交时另校验 workDate 三天范围。published_at 已持久化且 PATCH 不修改它。

## Goals / Non-Goals

**Goals:** 作者创建后连续 24 小时可编辑、删除，列表与详情能力一致；支持跨午夜补交编辑。

**Non-Goals:** 扩大新建日期范围、修改授权、增加 API 或数据库字段。

## Decisions

- 使用服务端 Instant 与 published_at 比较，截止时刻为 published_at + 24h，恰满 24h 关闭；不额外要求当前时间大于数据库生成的创建时间，避免时间戳精度或瞬时时钟回拨让新建日志暂时只读；不采用自然日或最后更新时间。
- 提取接受 now 的包级时间判定，生产传 Instant.now，单元测试固定时间覆盖精确边界；支持 JDBC OffsetDateTime 与 Instant。
- 列表、详情、PATCH、DELETE 共用窗口函数，保留事务锁、作者检查与版本保护。
- Android 仅在创建时检查三天日期范围；编辑依赖 allowedActions，实际请求由服务端重验过期，更新文案。

## Risks / Trade-offs

- 页面打开后能力可能过期 → 写请求仍由后端实时拒绝，客户端呈现统一 24 小时提示。
- 旧客户端文案与新规则不一致 → 后端与 Android 同步交付；无需迁移，回滚为旧版本。
