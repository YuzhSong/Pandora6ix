package com.pandora6ix.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ApprovalPushEventTest {
    @Test
    fun acceptsOnlyApprovalEventsAndTrimsEmptyNotificationIds() {
        assertEquals(
            ApprovalPrompt("notice-1", ApprovalPrompt.REVIEW_REQUEST),
            ApprovalPrompt.from("notice-1", ApprovalPrompt.REVIEW_REQUEST)
        )
        assertEquals(
            ApprovalPrompt(null, ApprovalPrompt.REVIEW_RESULT),
            ApprovalPrompt.from("", ApprovalPrompt.REVIEW_RESULT)
        )
        assertNull(ApprovalPrompt.from("notice-1", "TASK_UPDATED"))
        assertNull(ApprovalPrompt.from(null, null))
    }

    @Test
    fun usesGenericCopyForPromptMessages() {
        assertEquals("你有新的审批申请", ApprovalPrompt.from(null, ApprovalPrompt.REVIEW_REQUEST)?.message)
        assertEquals("你的审批状态有更新", ApprovalPrompt.from(null, ApprovalPrompt.REVIEW_RESULT)?.message)
    }
}
