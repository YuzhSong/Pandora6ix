package com.pandora6ix.app.data

/** 首页专用过滤：只判断本人的执行状态，不改变其他任务列表。 */
internal suspend fun loadHomeTaskBoardPage(
    currentUserId: String,
    cursor: String? = null,
    fetch: suspend (String?) -> TaskPageDto
): TaskPageDto {
    var next = cursor
    val visited = mutableSetOf<String?>()
    while (true) {
        check(visited.add(next)) { "任务分页游标重复，请重试" }
        val page = fetch(next)
        check(page.nextCursor == null || page.nextCursor !in visited) { "任务分页游标重复，请重试" }
        val visible = page.items.filterNot { task ->
            task.authorizedAssignments.any {
                it.recipientUserId == currentUserId && it.executionStatus == "COMPLETED"
            }
        }
        if (visible.isNotEmpty() || page.nextCursor == null) return page.copy(items = visible)
        next = page.nextCursor
    }
}
