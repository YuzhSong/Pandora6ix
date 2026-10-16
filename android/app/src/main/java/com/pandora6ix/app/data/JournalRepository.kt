package com.pandora6ix.app.data

import java.net.URLEncoder

/**
 * 日报（一篇多条工作条目）：
 * - 列表 GET /work-logs?date/from/to/scope（缺省 scope=mine）
 * - 详情 GET /work-logs/{journalId}
 * - 发布 POST /work-logs（201 返回 Journal；同日重复返回 409 DAILY_JOURNAL_EXISTS + existingJournalId）
 * - 编辑 PATCH /work-logs/{journalId}（expectedVersion + upsertEntries，不删除遗漏条目）
 */
class JournalRepository(private val api: ApiClient) {

    suspend fun journals(
        date: String? = null,
        from: String? = null,
        to: String? = null,
        scope: String = "mine",
        cursor: String? = null,
        limit: Int = 50
    ): JournalPageDto {
        val query = buildString {
            append("/work-logs?scope=").append(url(scope))
            date?.let { append("&date=").append(url(it)) }
            from?.let { append("&from=").append(url(it)) }
            to?.let { append("&to=").append(url(it)) }
            cursor?.let { append("&cursor=").append(url(it)) }
            append("&limit=").append(limit.coerceIn(1, 100))
        }
        return api.get(query, JournalPageDto.serializer())
    }

    suspend fun journal(journalId: String): JournalDto =
        api.get("/work-logs/$journalId", JournalDto.serializer())

    suspend fun create(workDate: String, entries: List<EntryCreateDto>): JournalDto {
        val body = api.json.encodeToString(
            JournalCreateDto.serializer(),
            JournalCreateDto(workDate = workDate, entries = entries)
        )
        return api.post("/work-logs", body, JournalDto.serializer())
    }

    suspend fun patch(journalId: String, patch: JournalPatchDto): JournalDto {
        val body = api.json.encodeToString(JournalPatchDto.serializer(), patch)
        return api.patch("/work-logs/$journalId", body, JournalDto.serializer())
    }

    private fun url(value: String) = URLEncoder.encode(value, "UTF-8")
}
