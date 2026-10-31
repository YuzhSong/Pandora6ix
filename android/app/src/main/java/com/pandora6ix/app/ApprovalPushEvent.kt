package com.pandora6ix.app

import com.pandora6ix.app.data.ReminderEvent

/** Coordinate the dedicated approval prompt and the unified notification queue after PR integration. */
internal fun claimApprovalPrompt(id: String?, announced: MutableSet<String>, pending: MutableList<ReminderEvent>): Boolean {
    if (id == null) return true
    val replacesPending = pending.removeAll { it.key == id }
    return announced.add(id) || replacesPending
}

data class ApprovalPrompt(
    val notificationId: String?,
    val eventType: String
) {
    val message: String
        get() = when (eventType) {
            REVIEW_REQUEST -> "你有新的审批申请"
            else -> "你的审批状态有更新"
        }

    companion object {
        const val REVIEW_REQUEST = "REVIEW_REQUEST"
        const val REVIEW_RESULT = "REVIEW_RESULT"

        fun from(notificationId: String?, eventType: String?): ApprovalPrompt? {
            val normalizedType = eventType?.takeIf { it == REVIEW_REQUEST || it == REVIEW_RESULT } ?: return null
            return ApprovalPrompt(notificationId?.takeIf(String::isNotBlank), normalizedType)
        }
    }
}
