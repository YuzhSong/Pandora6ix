package com.pandora6ix.app

import com.pandora6ix.app.data.ReminderEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class ApprovalPushEventTest {
    private fun notice(id: String) = ReminderEvent("STATUS", id, "审批", "通知")

    @Test
    fun approvalPushBeforePollingClaimsTheIdOnlyOnce() {
        val announced = mutableSetOf<String>()
        val pending = mutableListOf<ReminderEvent>()
        assertTrue(claimApprovalPrompt("approval", announced, pending))
        assertTrue("approval" in announced)
        assertFalse(claimApprovalPrompt("approval", announced, pending))
    }

    @Test
    fun approvalPushReplacesOnlyItsQueuedGenericReminder() {
        val announced = mutableSetOf("approval", "other")
        val pending = mutableListOf(notice("approval"), notice("other"))
        assertTrue(claimApprovalPrompt("approval", announced, pending))
        assertEquals(listOf("other"), pending.map { it.key })
        assertFalse(claimApprovalPrompt("approval", announced, pending))
    }

    @Test
    fun confirmedOrHistoricalNotificationsAreNotAnnouncedAgain() {
        assertFalse(claimApprovalPrompt("old", mutableSetOf("old"), mutableListOf()))
    }

    @Test
    fun legacyApprovalWithoutNotificationIdKeepsExistingPromptBehavior() {
        val pending = mutableListOf(notice("other"))
        assertTrue(claimApprovalPrompt(null, mutableSetOf(), pending))
        assertEquals(listOf("other"), pending.map { it.key })
    }

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
