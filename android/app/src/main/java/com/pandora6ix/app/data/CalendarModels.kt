package com.pandora6ix.app.data

import com.pandora6ix.app.mock.DemoDate
import kotlinx.serialization.Serializable

/** GET /calendar 响应（day|week|month；rangeStart/End/today/businessTimezone 以服务端为准） */
@Serializable
data class CalendarDto(
    val mode: String = "",
    val anchorDate: String = "",
    val rangeStart: String = "",
    val rangeEnd: String = "",
    val today: String = "",
    val businessTimezone: String = "",
    val tasks: List<TaskDto> = emptyList(),
    val nextCursor: String? = null,
    val totalCount: Long = 0
)

/** GET /tags 响应（企业标签目录） */
@Serializable
data class TagPageDto(
    val items: List<TagDto> = emptyList(),
    val nextCursor: String? = null,
    val moreUrl: String? = null
)

/** "YYYY-MM-DD" → DemoDate；非法输入返回 null */
fun parseIsoDate(text: String): DemoDate? {
    val parts = text.split("-")
    if (parts.size != 3) return null
    val y = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    val d = parts[2].toIntOrNull() ?: return null
    if (y < 1 || m !in 1..12 || d !in 1..31) return null
    return DemoDate(y, m, d)
}

/** "HH:mm" → 当天分钟数；非法输入返回 null */
fun parseHhMm(text: String?): Int? {
    if (text == null) return null
    val parts = text.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}

/**
 * 日历/详情统一展示模型（阶段 2.6 重构）：直接承载服务端建模字段，
 * 不再经 mock WorkTask 过渡；kind/reviewStatus/authorizedAssignments/allowedActions 全量保留。
 */
data class TaskItem(
    val id: String,
    val kind: String,
    val title: String,
    val description: String?,
    val priorityLevel: Int,
    val start: DemoDate,
    val end: DemoDate,
    val timeKind: String,
    val startMinute: Int?,
    val endMinute: Int?,
    val reviewStatus: String,
    /** 主标签（primaryTagId 对应）排在首位，与「主」标记及任务着色保持一致 */
    val tags: List<TagDto>,
    val displayColorHex: String?,
    val assignments: List<AssignmentDto>,
    val allowedActions: List<String>
)

fun TaskDto.toTaskItem(): TaskItem {
    val start = parseIsoDate(startDate) ?: DemoDate(1970, 1, 1)
    val end = parseIsoDate(endDate) ?: start
    val timed = timeKind == "TIMED"
    val primary = tags.firstOrNull { it.id == primaryTagId }
    return TaskItem(
        id = id,
        kind = kind,
        title = title,
        description = description,
        priorityLevel = priorityLevel.coerceIn(1, 5),
        start = start,
        end = end,
        timeKind = timeKind,
        startMinute = if (timed) parseHhMm(startTime) else null,
        endMinute = if (timed) parseHhMm(endTime) else null,
        reviewStatus = reviewStatus,
        tags = (primary?.let { listOf(it) } ?: emptyList()) + tags.filter { it.id != primary?.id },
        displayColorHex = displayColor,
        assignments = authorizedAssignments,
        allowedActions = allowedActions
    )
}

/** 展示用文案：任务类型 / 审批状态 / 执行状态（与后端枚举一一对应，不合成单一 status） */
fun kindLabelOf(kind: String): String = if (kind == "DELEGATED") "派发任务" else "个人任务"
fun reviewStatusLabel(status: String): String = when (status) {
    "PENDING" -> "待审批"
    "APPROVED" -> "已批准"
    "REJECTED" -> "已驳回"
    else -> status.ifBlank { "未知" }
}
fun executionStatusLabel(status: String): String = when (status) {
    "PENDING_APPROVAL" -> "待审批"
    "ACTIVE" -> "进行中"
    "AWAITING_ACCEPTANCE" -> "待验收"
    "COMPLETED" -> "已完成"
    "CANCELLED" -> "已取消"
    else -> status.ifBlank { "未知" }
}
