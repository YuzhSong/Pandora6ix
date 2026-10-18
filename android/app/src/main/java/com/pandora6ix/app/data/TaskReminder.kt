package com.pandora6ix.app.data

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

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
 * 从服务端任务快照派生提醒（当前客户端尚未消费通知接口）：
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
    val ownerId = prefs.ownerId ?: return emptyList()
    val tasks = loadReminderTasks { cursor -> repo.tasks(scope = "mine", limit = 100, cursor = cursor) }
    currentCoroutineContext().ensureActive()
    val batch = deriveReminders(tasks, ownerId, prefs.snapshot(), prefs.hasSnapshot(),
        prefs.newTask, prefs.deadline, prefs.statusChange, prefs.leadMinutes, timezone, nowMillis)
    val fresh = batch.events.filter { !prefs.isNotified(it.key) }
    prefs.saveSnapshot(batch.snapshot)
    if (fresh.isNotEmpty()) prefs.markNotified(fresh.map { it.key })
    return fresh
}

internal suspend fun loadReminderTasks(fetch: suspend (String?) -> TaskPageDto): List<TaskDto> {
    val tasks = LinkedHashMap<String, TaskDto>()
    val seen = HashSet<String>()
    var cursor: String? = null
    do {
        val page = fetch(cursor)
        page.items.forEach { tasks[it.taskId.ifBlank { it.id }] = it }
        cursor = page.nextCursor
        check(cursor == null || seen.add(cursor)) { "Repeated task cursor" }
    } while (cursor != null)
    return tasks.values.toList()
}

internal data class ReminderBatch(val snapshot: Map<String, String>, val events: List<ReminderEvent>)

internal fun deriveReminders(
    tasks: List<TaskDto>, ownerId: String, previous: Map<String, String>, initialized: Boolean,
    newTask: Boolean, deadline: Boolean, statusChange: Boolean, leadMinutes: Int,
    timezone: String?, nowMillis: Long
): ReminderBatch {
    val firstRun = !initialized
    val current = LinkedHashMap<String, String>()
    val events = ArrayList<ReminderEvent>()

    for (task in tasks) {
        // Authorized task data may include other recipients; reminders belong to this account only.
        if (task.reviewStatus != "APPROVED") continue
        val assignments = task.authorizedAssignments.filter { it.recipientUserId == ownerId }
        for (assignment in assignments) {
            val key = task.taskId.ifBlank { task.id } + "|" + assignment.id
            val state = assignment.executionStatus + "|" + task.reviewStatus
            current[key] = state
            val before = previous[key]
            when {
                before == null -> {
                    // 新出现的执行关系：已批准（派发生效 / 个人任务批准）才算新任务
                    if (!firstRun && newTask &&
                        assignment.executionStatus in listOf("ACTIVE", "AWAITING_ACCEPTANCE")
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
                    if (statusChange) {
                        events += ReminderEvent(
                            kind = ReminderKind.STATUS,
                            key = ReminderKind.STATUS + ":" + key + ":" + state + ":" + assignment.version,
                            title = "任务状态变化",
                            message = "《${task.title}》更新为${executionStatusLabel(assignment.executionStatus)}" +
                                if (task.reviewStatus == "REJECTED") "（任务已驳回）" else ""
                        )
                    }
                }
            }
        }
        // 截止时间临近：只计算本人的未完成执行关系
        val pending = assignments.any { it.executionStatus in listOf("ACTIVE", "AWAITING_ACCEPTANCE") }
        if (pending && deadline) {
            val due = businessDayMillis(task.endDate, task.endTime, timezone) ?: continue
            val remaining = due - nowMillis
            val window = leadMinutes.coerceIn(1, NotificationPrefs.MAX_LEAD_MINUTES) * MINUTE_MILLIS
            if (remaining > 0 && remaining <= window) {
                events += ReminderEvent(
                    kind = ReminderKind.DEADLINE,
                    key = ReminderKind.DEADLINE + ":" + (task.taskId.ifBlank { task.id }),
                    title = "截止时间临近",
                    message = "《${task.title}》将于 ${task.endDate}${task.endTime?.let { " $it" } ?: ""} 截止，请尽快处理。"
                )
            }
        }
    }

    return ReminderBatch(current, events)
}
