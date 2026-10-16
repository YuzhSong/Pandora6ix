package com.pandora6ix.app.data

import kotlinx.serialization.Serializable
import java.net.URLEncoder

/** PUT /me/important-entries 请求体：entryIds 最多十条；空数组是合法的主动清空 */
@Serializable
data class SelectionSaveDto(
    val expectedVersion: Long,
    val entryIds: List<String>
)

/**
 * 个人重要事项（entryId 选择集）：
 * - 候选：GET /me/important-entry-candidates（D～D-9 已发布日报的工作条目）
 * - 当前选择：GET /me/important-entries（ImportantSelection，含 version）
 * - 保存：PUT /me/important-entries（expectedVersion + entryIds）
 * 引用稳定 entryId；不是整篇日报，也不是内容副本。
 */
class SelectionRepository(private val api: ApiClient) {

    suspend fun candidates(cursor: String? = null, limit: Int = 100): WorkEntryPageDto {
        val query = buildString {
            append("/me/important-entry-candidates?limit=").append(limit.coerceIn(1, 100))
            cursor?.let { append("&cursor=").append(URLEncoder.encode(it, "UTF-8")) }
        }
        return api.get(query, WorkEntryPageDto.serializer())
    }

    suspend fun selection(): ImportantSelectionDto =
        api.get("/me/important-entries", ImportantSelectionDto.serializer())

    suspend fun save(entryIds: List<String>, expectedVersion: Long): ImportantSelectionDto {
        val body = api.json.encodeToString(
            SelectionSaveDto.serializer(),
            SelectionSaveDto(expectedVersion = expectedVersion, entryIds = entryIds.take(10))
        )
        return api.put("/me/important-entries", body, ImportantSelectionDto.serializer())
    }
}
