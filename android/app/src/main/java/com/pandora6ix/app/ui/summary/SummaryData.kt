package com.pandora6ix.app.ui.summary

import androidx.compose.runtime.*
import com.pandora6ix.app.data.*
import kotlinx.coroutines.CancellationException

internal data class SummaryPage<T>(val items: List<T>, val nextCursor: String?)

/** One bounded request at a time. Failed/cancelled pages never consume their cursor. */
internal class SummaryPager<T>(private val id: (T) -> String, private val fetch: suspend (String?) -> SummaryPage<T>) {
    var items by mutableStateOf<List<T>>(emptyList())
        private set
    var nextCursor by mutableStateOf<String?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var initialized by mutableStateOf(false)
        private set
    private val consumedCursors = mutableSetOf<String>()
    private var retryRefresh = false

    suspend fun ensureLoaded() {
        if (!initialized) load()
    }

    suspend fun load(refresh: Boolean = false) {
        if (loading || (!refresh && initialized && nextCursor == null && error == null)) return
        val replace = refresh || retryRefresh
        val cursor = if (replace) null else nextCursor
        loading = true
        error = null
        try {
            val page = fetch(cursor)
            check(page.nextCursor == null || (page.nextCursor != cursor && (replace || page.nextCursor !in consumedCursors))) {
                "Repeated summary cursor"
            }
            items = ((if (replace) emptyList() else items) + page.items).distinctBy(id)
            if (replace) consumedCursors.clear()
            cursor?.let { consumedCursors.add(it) }
            nextCursor = page.nextCursor
            initialized = true
            retryRefresh = false
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            initialized = true
            retryRefresh = replace
            error = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
        } finally {
            loading = false
        }
    }
}

internal fun directReports(people: List<RecipientDto>) = people.filter { it.relationship == "DIRECT" }.distinctBy { it.id }

internal fun responsibleTasks(tasks: List<TaskDto>, userId: String) = tasks.filter { task ->
    task.authorizedAssignments.any { it.recipientUserId == userId }
}

internal fun executorIds(task: TaskDto) = task.authorizedAssignments.map { it.recipientUserId }.filter { it.isNotBlank() }.distinct()

/**
 * 执行人姓名：优先用任务执行记录自带的 recipientName（同级、上级等不在下属名单里的人也有真实姓名），
 * 其次再按本人/下属名单查找；避免头像退化成占位文字“成员 xxxx”的首字“成”。
 */
internal fun executorName(task: TaskDto, userId: String, fallback: (String) -> String): String =
    task.authorizedAssignments.firstOrNull { it.recipientUserId == userId }?.recipientName?.takeIf { it.isNotBlank() } ?: fallback(userId)

internal fun summaryTaskStatus(task: TaskDto): String {
    return taskCompletionLabel(task.taskCompletionState())
}

internal fun memberJournals(journals: List<JournalDto>, userId: String) = journals.filter { it.authorUserId == userId }

internal fun summaryPersonName(userId: String, user: UserDto, people: List<RecipientDto>): String =
    (if (userId == user.id) user.displayName.ifBlank { user.name } else people.firstOrNull { it.id == userId }?.displayName)
        ?.takeIf { it.isNotBlank() } ?: "成员 ${userId.takeLast(4)}"

internal fun journalSummary(journal: JournalDto): String = journal.entries.sortedBy { it.displayOrder }
    .joinToString(" ") { it.content }.replace(Regex("\\s+"), " ").trim().ifBlank { "暂无日志内容" }
