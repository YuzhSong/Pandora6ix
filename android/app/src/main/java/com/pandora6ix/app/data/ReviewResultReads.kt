package com.pandora6ix.app.data

import android.content.Context

/** 本机已读结果，审批记录由服务端鉴权；持久化记录按登录账号隔离。 */
class ReviewResultReads(context: Context, ownerId: String) {
    private val prefs = context.getSharedPreferences("review_results_$ownerId", Context.MODE_PRIVATE)
    fun keys(): Set<String> = prefs.getStringSet("read", emptySet())!!.toSet()
    fun markRead(review: TaskReviewDto) {
        val key = reviewResultKey(review) ?: return
        prefs.edit().putStringSet("read", keys() + key).apply()
    }
}

fun reviewResultKey(review: TaskReviewDto): String? =
    if (review.decision in setOf("APPROVED", "REJECTED"))
        "${review.id}|${review.decision}|${review.decidedAt.orEmpty()}"
    else null

/** 扫描 created 全部分页；不能按提交时间截断，旧申请也可能刚刚得到审批。 */
suspend fun unreadReviewResults(
    readKeys: Set<String>,
    loadPage: suspend (String?) -> TaskReviewPageDto
): Set<String> {
    val unread = linkedSetOf<String>()
    var cursor: String? = null
    do {
        val page = loadPage(cursor)
        page.items.mapNotNull(::reviewResultKey).filterNot { it in readKeys }.forEach(unread::add)
        if (unread.size >= 99) return unread.take(99).toSet()
        cursor = page.nextCursor
    } while (cursor != null)
    return unread
}
