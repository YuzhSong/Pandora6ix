package com.pandora6ix.app.data

/** 首页只隐藏任务整体已通过验收的完成结果，待验收任务继续显示。 */
internal suspend fun loadHomeTaskBoardPage(
    cursor: String? = null,
    fetch: suspend (String?) -> TaskPageDto
): TaskPageDto {
    var next = cursor
    val visited = mutableSetOf<String?>()
    while (true) {
        check(visited.add(next)) { "任务分页游标重复，请重试" }
        val page = fetch(next)
        check(page.nextCursor == null || page.nextCursor !in visited) { "任务分页游标重复，请重试" }
        val visible = page.items.filterNot { it.taskCompletionState() == "COMPLETED" }
        if (visible.isNotEmpty() || page.nextCursor == null) return page.copy(items = visible)
        next = page.nextCursor
    }
}
