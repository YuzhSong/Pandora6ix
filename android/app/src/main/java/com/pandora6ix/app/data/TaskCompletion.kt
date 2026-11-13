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

/**
 * 任务是否已逾期：截止时刻（公司业务时区；全天任务按当日 23:59）已过，且任务尚未提交完成/完成。
 * 仅作展示标识，不改变任务状态，也不限制完成或验收操作。
 */
fun TaskDto.isTaskOverdue(timezone: String?, nowMillis: Long = System.currentTimeMillis()): Boolean {
    when (taskCompletionState()) {
        "COMPLETED", "AWAITING_ACCEPTANCE" -> return false
    }
    if (reviewStatus != "APPROVED") return false
    // 全天任务传 null → businessDayMillis 按当日 23:59 计算，与后端 dueInstant 口径一致
    val due = businessDayMillis(endDate, if (timeKind == "ALL_DAY") null else endTime, timezone) ?: return false
    return due < nowMillis
}
