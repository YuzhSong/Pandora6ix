package com.pandora6ix.app.data

/** completionStatus 是任务整体状态；旧服务端缺少字段时兼容已有执行关系。 */
fun TaskDto.taskCompletionState(): String = completionStatus ?: when {
    authorizedAssignments.any { it.executionStatus == "COMPLETED" } -> "COMPLETED"
    authorizedAssignments.any { it.executionStatus == "AWAITING_ACCEPTANCE" } -> "AWAITING_ACCEPTANCE"
    else -> "NOT_COMPLETED"
}

fun taskCompletionLabel(state: String): String = when (state) {
    "COMPLETED" -> "已完成"
    "AWAITING_ACCEPTANCE" -> "已完成，待验收"
    else -> "未完成"
}
