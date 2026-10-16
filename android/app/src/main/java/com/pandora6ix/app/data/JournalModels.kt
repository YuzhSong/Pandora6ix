package com.pandora6ix.app.data

import kotlinx.serialization.Serializable

/** POST /work-logs 的新条目：clientEntryKey 为客户端生成的稳定键（如 UUID） */
@Serializable
data class EntryCreateDto(
    val clientEntryKey: String,
    val content: String
)

/** POST /work-logs 请求体：workDate(YYYY-MM-DD) + 1~100 条 entries；标签可选 */
@Serializable
data class JournalCreateDto(
    val workDate: String,
    val entries: List<EntryCreateDto>,
    val tagIds: List<String>? = null,
    val primaryTagId: String? = null
)

/**
 * PATCH /work-logs/{id} 的条目 upsert：
 * - 已有条目携带 entryId（内容更新）
 * - 新增条目携带 clientEntryKey
 * - 遗漏的旧条目不会被删除
 */
@Serializable
data class EntryUpsertDto(
    val entryId: String? = null,
    val clientEntryKey: String? = null,
    val content: String
)

/** PATCH /work-logs/{id} 请求体：expectedVersion 必填 */
@Serializable
data class JournalPatchDto(
    val expectedVersion: Long,
    val upsertEntries: List<EntryUpsertDto>? = null,
    val tagIds: List<String>? = null,
    val primaryTagId: String? = null
)

/** GET /work-logs 分页响应 */
@Serializable
data class JournalPageDto(
    val items: List<JournalDto> = emptyList(),
    val nextCursor: String? = null,
    val moreUrl: String? = null
)
