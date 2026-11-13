package com.pandora6ix.app.data

/** Creation-review rejection belongs in submitted review history, not working task lists.
 * Completion rejection keeps an approved task actionable and must not hide it.
 */
internal fun TaskPageDto.withoutRejectedTasks(): TaskPageDto =
    copy(items = items.filterNot { it.reviewStatus == "REJECTED" })

internal fun CalendarDto.withoutRejectedTasks(): CalendarDto =
    copy(tasks = tasks.filterNot { it.reviewStatus == "REJECTED" })
