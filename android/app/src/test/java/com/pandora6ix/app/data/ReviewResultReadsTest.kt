package com.pandora6ix.app.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReviewResultReadsTest {
    private fun result(id: String, decision: String = "APPROVED", time: String = "2026-10-29T00:00:00Z") =
        TaskReviewDto(id = id, decision = decision, decidedAt = time)

    @Test fun countsApprovedAndRejectedButNotPendingOrAlreadyRead() = runBlocking {
        val read = result("read")
        val unread = unreadReviewResults(setOf(reviewResultKey(read)!!)) {
            TaskReviewPageDto(listOf(read, result("approved"), result("rejected", "REJECTED"), result("pending", "PENDING")))
        }
        assertEquals(setOf(reviewResultKey(result("approved")), reviewResultKey(result("rejected", "REJECTED"))), unread)
    }

    @Test fun readsLaterPagesEvenWhenFirstPageContainsNoUnreadResults() = runBlocking {
        val visited = mutableListOf<String?>()
        val read = result("old")
        val rejected = result("later", "REJECTED")
        val unread = unreadReviewResults(setOf(reviewResultKey(read)!!)) { cursor ->
            visited.add(cursor)
            if (cursor == null) TaskReviewPageDto(listOf(read), nextCursor = "next")
            else TaskReviewPageDto(listOf(rejected))
        }
        assertEquals(listOf(null, "next"), visited)
        assertEquals(setOf(reviewResultKey(rejected)), unread)
    }

    @Test fun viewingOneResultLeavesOthersUnreadAndNewDecisionIsUnread() = runBlocking {
        val first = result("first")
        val second = result("second", "REJECTED")
        val unread = unreadReviewResults(setOf(reviewResultKey(first)!!)) {
            TaskReviewPageDto(listOf(first, second, first.copy(decidedAt = "2026-10-30T00:00:00Z")))
        }
        assertEquals(2, unread.size)
        assertFalse(reviewResultKey(first) in unread)
        assertTrue(reviewResultKey(second) in unread)
    }

    @Test fun stopsAtBadgeCapAndDoesNotCountDuplicateRecords() = runBlocking {
        var calls = 0
        val unread = unreadReviewResults(emptySet()) {
            calls++
            val results = (1..100).map { result("$it") }
            TaskReviewPageDto(results + results, nextCursor = "unused")
        }
        assertEquals(99, unread.size)
        assertEquals(1, calls)
    }

    @Test fun failedPageDoesNotReturnPartialCount() {
        try {
            runBlocking {
                unreadReviewResults(emptySet()) { cursor ->
                    if (cursor == null) TaskReviewPageDto(listOf(result("first")), nextCursor = "next")
                    else throw IllegalStateException("offline")
                }
            }
            fail("Expected query failure")
        } catch (expected: IllegalStateException) {
            assertEquals("offline", expected.message)
        }
    }
}
