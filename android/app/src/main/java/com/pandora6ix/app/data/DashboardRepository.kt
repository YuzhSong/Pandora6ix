package com.pandora6ix.app.data

import kotlinx.serialization.DeserializationStrategy
import java.net.URLEncoder

/**
 * 导图四面板与单条详情读取：
 * - GET /dashboard 一次取四面板 + 统计
 * - 单条全文用稳定 ID：company-important-items / tasks / work-log-entries / work-logs
 * - 分页按面板 moreUrl + nextCursor；moreUrl 形如 "/api/v1/xxx"（带版本前缀，需剥掉再拼基址）
 */
class DashboardRepository(private val api: ApiClient) {

    suspend fun dashboard(): DashboardDto = api.get("/dashboard", DashboardDto.serializer())

    suspend fun companyItem(id: String): CompanyItemDto =
        api.get("/company-important-items/$id", CompanyItemDto.serializer())

    suspend fun task(taskId: String): TaskDto =
        api.get("/tasks/$taskId", TaskDto.serializer())

    /** 单条工作条目全文；注意 entryId 不是 journalId */
    suspend fun workEntry(entryId: String): WorkEntryDto =
        api.get("/work-log-entries/$entryId", WorkEntryDto.serializer())

    /** 整篇日报 */
    suspend fun journal(journalId: String): JournalDto =
        api.get("/work-logs/$journalId", JournalDto.serializer())

    /** 按 moreUrl + cursor 读取下一页（公司事项/派发任务/工作条目面板通用） */
    suspend fun <T> more(
        moreUrl: String,
        cursor: String,
        serializer: DeserializationStrategy<T>
    ): T = api.get(
        moreUrl.removePrefix("/api/v1") + "?cursor=" + URLEncoder.encode(cursor, "UTF-8"),
        serializer
    )

    fun companyItemPageSerializer() = CompanyItemPageDto.serializer()
    fun taskPageSerializer() = TaskPageDto.serializer()
    fun workEntryPageSerializer() = WorkEntryPageDto.serializer()
}
