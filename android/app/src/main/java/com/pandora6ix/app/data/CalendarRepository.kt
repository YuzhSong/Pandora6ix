package com.pandora6ix.app.data

import java.net.URLEncoder

/**
 * 日历 / 标签 / 统计 / 任务列表读取。
 * - 日/周/月走 GET /calendar 获取服务端日期范围，非 mine 范围通过 /tasks 查询
 * - 年视图后端无对应 mode：按 /tasks?scope=mine&from=年初&to=年末 的既有日期区间查询
 * - 标签目录 GET /tags（id/colorHex(#RRGGBB)/version，不以名称为键）
 */
class CalendarRepository(private val api: ApiClient) {

    suspend fun calendar(
        mode: String,
        anchorDate: String,
        tagIds: List<String> = emptyList(),
        cursor: String? = null,
        limit: Int = 100,
        scope: String = "mine"
    ): CalendarDto {
        val query = buildString {
            append("/calendar?mode=").append(url(mode))
            append("&anchorDate=").append(url(anchorDate))
            tagIds.forEach { append("&tagIds=").append(url(it)) }
            cursor?.let { append("&cursor=").append(url(it)) }
            append("&limit=").append(limit.coerceIn(1, 100))
        }
        val calendar = api.get(query, CalendarDto.serializer())
        if (scope == "mine") return calendar

        // /calendar 仅支持 mine；复用其服务端周期边界查询其他范围。
        val scopedTasks = mutableListOf<TaskDto>()
        var taskCursor: String? = null
        do {
            val page = tasks(scope = scope, from = calendar.rangeStart, to = calendar.rangeEnd, cursor = taskCursor)
            scopedTasks.addAll(page.items)
            taskCursor = page.nextCursor
        } while (taskCursor != null)
        val filteredTasks = if (tagIds.isEmpty()) scopedTasks else scopedTasks.filter { task ->
            tagIds.all { id -> task.tags.any { it.id == id } }
        }
        return calendar.copy(tasks = filteredTasks, nextCursor = null, totalCount = filteredTasks.size.toLong())
    }

    suspend fun tags(limit: Int = 100): List<TagDto> =
        api.get("/tags?limit=${limit.coerceIn(1, 100)}", TagPageDto.serializer()).items

    suspend fun statistics(): StatisticsDto = api.get("/statistics", StatisticsDto.serializer())

    /** 任务列表：scope ∈ mine|managed|created；年视图按全年区间查询 */
    suspend fun tasks(
        scope: String = "mine",
        from: String? = null,
        to: String? = null,
        limit: Int = 100,
        cursor: String? = null
    ): TaskPageDto {
        val query = buildString {
            append("/tasks?scope=").append(url(scope))
            from?.let { append("&from=").append(url(it)) }
            to?.let { append("&to=").append(url(it)) }
            cursor?.let { append("&cursor=").append(url(it)) }
            append("&limit=").append(limit.coerceIn(1, 100))
        }
        return api.get(query, TaskPageDto.serializer())
    }

    private fun url(value: String) = URLEncoder.encode(value, "UTF-8")
}
