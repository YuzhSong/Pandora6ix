package com.pandora6ix.app.ui.review

import com.pandora6ix.app.data.TaskReviewDto
import com.pandora6ix.app.data.TaskReviewPageDto

internal data class ReviewListWindow(
    val items: List<TaskReviewDto>,
    val nextCursor: String?,
    val pageCount: Int
)

/** 刷新用户已经加载的窗口，全部完成后交给 UI 一次更新，避免中途缩回第一页。 */
internal suspend fun refreshReviewWindow(
    pageCount: Int,
    loadPage: suspend (String?) -> TaskReviewPageDto
): ReviewListWindow {
    val items = mutableListOf<TaskReviewDto>()
    var cursor: String? = null
    var loaded = 0
    do {
        val page = loadPage(cursor)
        items.addAll(page.items)
        cursor = page.nextCursor
        loaded++
    } while (cursor != null && loaded < pageCount.coerceAtLeast(1))
    return ReviewListWindow(items.distinctBy { it.id }, cursor, loaded)
}
