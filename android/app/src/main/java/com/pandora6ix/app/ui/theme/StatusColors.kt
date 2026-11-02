package com.pandora6ix.app.ui.theme

import androidx.compose.ui.graphics.Color

/** 状态语义色调（规范 2.2）：颜色只辅助文字，不单独表达状态 */
enum class StatusTone { NEUTRAL, INFO, WARNING, SUCCESS, ERROR }

data class StatusColors(val container: Color, val content: Color)

fun PandoraColorTokens.statusColors(tone: StatusTone): StatusColors = when (tone) {
    StatusTone.NEUTRAL -> StatusColors(neutralContainer, textSecondary)
    StatusTone.INFO -> StatusColors(infoContainer, info)
    StatusTone.WARNING -> StatusColors(warningContainer, warning)
    StatusTone.SUCCESS -> StatusColors(successContainer, success)
    StatusTone.ERROR -> StatusColors(errorContainer, error)
}

/** 审批状态：仅映射服务端已有值，未知值归中性而不是成功 */
fun reviewStatusTone(status: String?): StatusTone = when (status) {
    "PENDING" -> StatusTone.WARNING
    "APPROVED" -> StatusTone.SUCCESS
    "REJECTED" -> StatusTone.ERROR
    else -> StatusTone.NEUTRAL
}

/** 执行状态；ACTIVE 再按工作状态区分未开始/进行中 */
fun executionStatusTone(executionStatus: String?, workStatus: String? = null): StatusTone = when (executionStatus) {
    "PENDING_APPROVAL" -> StatusTone.WARNING
    "ACTIVE" -> if (workStatus == "NOT_STARTED") StatusTone.NEUTRAL else StatusTone.INFO
    "AWAITING_ACCEPTANCE" -> StatusTone.WARNING
    "COMPLETED" -> StatusTone.SUCCESS
    else -> StatusTone.NEUTRAL
}

/** 重要程度 1–5：1/2 很低/一般，3 普通，4 重要，5 紧急 */
fun priorityTone(level: Int): StatusTone = when (level.coerceIn(1, 5)) {
    4 -> StatusTone.WARNING
    5 -> StatusTone.ERROR
    else -> StatusTone.NEUTRAL
}

fun priorityLabel(level: Int): String = when (level.coerceIn(1, 5)) {
    1 -> "很低"
    2 -> "一般"
    3 -> "普通"
    4 -> "重要"
    else -> "紧急"
}
