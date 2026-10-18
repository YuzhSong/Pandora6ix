package com.pandora6ix.app.data

/**
 * 一次提醒事件：页内弹窗与系统通知共用。
 * key 同时作为去重键（同一事件只提醒一次）。
 */
data class ReminderEvent(
    val kind: String,
    val key: String,
    val title: String,
    val message: String
)

private const val MINUTE_MILLIS = 60L * 1000L

/**
 * 从服务端任务快照派生提醒（后端无消息服务，判定放在客户端）：
 * - NEW_TASK：新增且已批准的执行关系（你被派发了新任务 / 个人任务批准）
 * - DEADLINE：截止前 leadHours 小时内且未完成（已完成不再提醒）
 * - STATUS：已见过任务的执行状态或审批状态发生变化
 *
 * 首次运行（无快照）只建立基线，避免把存量任务当成新任务；
 * 已提醒过的事件不再重复返回。
 */
suspend fun collectReminders(
    repo: CalendarRepository,
    prefs: NotificationPrefs,
    timezone: String?,
    nowMillis: Long = System.currentTimeMillis()
): List<ReminderEvent> {
    if (!prefs.master) return emptyList()
    val tasks = runCatching { repo.tasks(scope = "mine", limit = 100) }.getOrNull()?.items ?: return emptyList()

    val previous = prefs.snapshot()
    val firstRun = previous.isEmpty()
    val current = LinkedHashMap<String, String>()
    val events = ArrayList<ReminderEvent>()

    for (task in tasks) {
        val assignments = task.authorizedAssignments
        for (assignment in assignments) {
            val key = task.taskId.ifBlank { task.id } + "|" + assignment.id
            val state = assignment.executionStatus + "|" + task.reviewStatus
            current[key] = state
            val before = previous[key]
            when {
                before == null -> {
                    // 新出现的执行关系：已批准（派发生效 / 个人任务批准）才算新任务
                    if (!firstRun && prefs.enabled(ReminderKind.NEW_TASK) &&
                        task.reviewStatus == "APPROVED" && assignment.executionStatus != "CANCELLED"
                    ) {
                        events += ReminderEvent(
                            kind = ReminderKind.NEW_TASK,
                            key = ReminderKind.NEW_TASK + ":" + key,
                            title = "新任务提醒",
                            message = "${kindLabelOf(task.kind)}《${task.title}》已派发给你，请及时处理。"
                        )
                    }
                }
                before != state -> {
                    if (prefs.enabled(ReminderKind.STATUS)) {
                        events += ReminderEvent(
                            kind = ReminderKind.STATUS,
                            key = ReminderKind.STATUS + ":" + key + ":" + state,
                            title = "任务状态变化",
                            message = "《${task.title}》更新为${executionStatusLabel(assignment.executionStatus)}" +
                                if (task.reviewStatus == "REJECTED") "（任务已驳回）" else ""
                        )
                    }
                }
            }
        }
        // 截止时间临近：任一未完成的执行关系都算本人待办
        val pending = assignments.any { it.executionStatus !in listOf("COMPLETED", "CANCELLED") }
        if (pending && task.reviewStatus != "REJECTED" && prefs.enabled(ReminderKind.DEADLINE)) {
            val due = businessDayMillis(task.endDate, task.endTime, timezone) ?: continue
            val remaining = due - nowMillis
            val window = prefs.leadMinutes.coerceIn(1, NotificationPrefs.MAX_LEAD_MINUTES) * MINUTE_MILLIS
            if (remaining in 0..window) {
                events += ReminderEvent(
                    kind = ReminderKind.DEADLINE,
                    key = ReminderKind.DEADLINE + ":" + (task.taskId.ifBlank { task.id }),
                    title = "截止时间临近",
                    message = "《${task.title}》将于 ${task.endDate}${task.endTime?.let { " $it" } ?: ""} 截止，请尽快处理。"
                )
            }
        }
    }

    prefs.saveSnapshot(current)
    val fresh = events.filter { !prefs.isNotified(it.key) }
    if (fresh.isNotEmpty()) prefs.markNotified(fresh.map { it.key })
    return fresh
}
