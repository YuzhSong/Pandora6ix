package com.pandora6ix.app.ui.review

import com.pandora6ix.app.data.TaskReviewDto
import com.pandora6ix.app.data.TaskReviewPageDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ReviewListWindowTest {
    private fun review(id: String) = TaskReviewDto(id = id)

    @Test fun refreshKeepsDeepRowsAndUsesFreshCursorForEachPage() = runBlocking {
        val cursors = mutableListOf<String?>()
        val refreshed = refreshReviewWindow(3) { cursor ->
            cursors.add(cursor)
            when (cursor) {
                null -> TaskReviewPageDto(listOf(review("top")), "fresh-page-2")
                "fresh-page-2" -> TaskReviewPageDto(listOf(review("middle")), "fresh-page-3")
                "fresh-page-3" -> TaskReviewPageDto(listOf(review("deep")), "fresh-page-4")
                else -> error("Must not load beyond the existing window")
            }
        }
        assertEquals(listOf(null, "fresh-page-2", "fresh-page-3"), cursors)
        assertEquals(listOf("top", "middle", "deep"), refreshed.items.map { it.id })
        assertEquals("fresh-page-4", refreshed.nextCursor)
        assertEquals(3, refreshed.pageCount)
    }

    @Test fun stopsWhenQueueShrinksAfterReviewDecision() = runBlocking {
        var calls = 0
        val refreshed = refreshReviewWindow(4) {
            calls++
            TaskReviewPageDto(listOf(review("remaining")))
        }
        assertEquals(1, calls)
        assertEquals(1, refreshed.pageCount)
        assertNull(refreshed.nextCursor)
    }

    @Test fun initialLoadOnlyRequestsFirstPage() = runBlocking {
        var calls = 0
        val refreshed = refreshReviewWindow(0) {
            calls++
            TaskReviewPageDto(listOf(review("first")), "second")
        }
        assertEquals(1, calls)
        assertEquals(1, refreshed.pageCount)
        assertEquals("second", refreshed.nextCursor)
    }

    @Test fun pageFailureNeverReplacesExistingWindowWithPartialData() = runBlocking {
        val previous = ReviewListWindow(listOf(review("old-top"), review("old-deep")), "old-next", 2)
        var visible = previous
        try {
            visible = refreshReviewWindow(2) { cursor ->
                if (cursor == null) TaskReviewPageDto(listOf(review("new-top")), "next")
                else error("offline")
            }
            fail("Expected refresh failure")
        } catch (expected: IllegalStateException) {
            assertEquals("offline", expected.message)
        }
        assertSame(previous, visible)
    }

    @Test fun duplicateIdsAcrossChangingPagesRemainUniqueLazyListKeys() = runBlocking {
        val refreshed = refreshReviewWindow(2) { cursor ->
            if (cursor == null) TaskReviewPageDto(listOf(review("first"), review("shared")), "next")
            else TaskReviewPageDto(listOf(review("shared"), review("deep")))
        }
        assertEquals(listOf("first", "shared", "deep"), refreshed.items.map { it.id })
    }
}
