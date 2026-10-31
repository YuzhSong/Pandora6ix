package com.pandora6ix.app.ui.summary

import com.pandora6ix.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class SummaryDataTest {
    private fun task(id: String, creator: String, vararg executors: String) = TaskDto(
        id = id, creatorUserId = creator, authorizedAssignments = executors.mapIndexed { i, user ->
            AssignmentDto(id = "$id-$i", recipientUserId = user)
        }
    )

    @Test fun directReportsAreNeverInferredFromOrganization() {
        val people = listOf(
            RecipientDto("direct", orgUnitId = "team", relationship = "DIRECT"),
            RecipientDto("other", orgUnitId = "team", relationship = "DESCENDANT"),
            RecipientDto("unknown", orgUnitId = "team"))
        assertEquals(listOf("direct"), directReports(people).map { it.id })
    }

    @Test fun creatorIsNotAnExecutorAndSharedTaskKeepsAllAuthorizedExecutors() {
        val createdOnly = task("created", "member", "someone-else")
        val assigned = task("assigned", "boss", "member", "second", "member")
        assertEquals(listOf("assigned"), responsibleTasks(listOf(createdOnly, assigned), "member").map { it.id })
        assertEquals(listOf("member", "second"), executorIds(assigned))
        assertTrue(executorIds(task("empty", "creator")).isEmpty())
    }

    @Test fun journalsAreFilteredByAuthorNotEntryId() {
        val own = JournalDto("j1", authorUserId = "member")
        val other = JournalDto("member", authorUserId = "other")
        assertEquals(listOf(own), memberJournals(listOf(own, other), "member"))
    }

    @Test fun namesComeFromCurrentUserOrAuthorizedCandidates() {
        val me = UserDto("me", displayName = "本人")
        val people = listOf(RecipientDto("member", displayName = "同事"))
        assertEquals("本人", summaryPersonName("me", me, people))
        assertEquals("同事", summaryPersonName("member", me, people))
        assertEquals("成员 1234", summaryPersonName("missing1234", me, people))
    }

    @Test fun realWorkStateIsUsedWithoutConflatingApprovalAndExecution() {
        val pending = TaskDto("pending", reviewStatus = "PENDING")
        val active = TaskDto("active", reviewStatus = "APPROVED", authorizedAssignments = listOf(
            AssignmentDto("assignment", executionStatus = "ACTIVE", workStatus = "NOT_STARTED")))
        assertEquals("待审批", summaryTaskStatus(pending))
        assertEquals("待开始", summaryTaskStatus(active))
        assertEquals("已批准", summaryTaskStatus(TaskDto("unassigned", reviewStatus = "APPROVED")))
    }

    @Test fun nextPageFailureRetainsDataAndRetriesSameCursor() = runBlocking {
        val cursors = mutableListOf<String?>()
        var fail = true
        val pager = SummaryPager<String>({ it }) { cursor ->
            cursors += cursor
            if (cursor == null) SummaryPage(listOf("a"), "next")
            else if (fail) { fail = false; throw ApiException(403, null, "forbidden") }
            else SummaryPage(listOf("a", "b"), null)
        }
        pager.ensureLoaded()
        pager.ensureLoaded()
        pager.load()
        assertEquals(listOf("a"), pager.items)
        assertEquals("next", pager.nextCursor)
        assertEquals("没有权限执行此操作", pager.error)
        pager.load()
        assertEquals(listOf("a", "b"), pager.items)
        assertEquals(listOf(null, "next", "next"), cursors)
        assertNull(pager.nextCursor)
    }

    @Test fun emptyFilteredPageStillAllowsContinuingToMemberMatch() = runBlocking {
        val pager = SummaryPager<TaskDto>({ it.id }) { cursor ->
            if (cursor == null) SummaryPage(listOf(task("other", "boss", "other")), "next")
            else SummaryPage(listOf(task("match", "boss", "member")), null)
        }
        pager.ensureLoaded()
        assertTrue(responsibleTasks(pager.items, "member").isEmpty())
        assertNotNull(pager.nextCursor)
        pager.load()
        assertEquals(listOf("match"), responsibleTasks(pager.items, "member").map { it.id })
    }

    @Test fun failedRefreshRetriesFirstPageInsteadOfOldNextPage() = runBlocking {
        var calls = 0
        val cursors = mutableListOf<String?>()
        val pager = SummaryPager<String>({ it }) { cursor ->
            cursors += cursor
            when (++calls) {
                1 -> SummaryPage(listOf("old"), "next")
                2 -> throw IllegalStateException("offline")
                else -> SummaryPage(listOf("new"), null)
            }
        }
        pager.ensureLoaded()
        pager.load(refresh = true)
        assertEquals(listOf("old"), pager.items)
        pager.load()
        assertEquals(listOf("new"), pager.items)
        assertEquals(listOf(null, null, null), cursors)
    }

    @Test fun repeatedCursorIsAnErrorAndNeverAppendsThatPage() = runBlocking {
        val pager = SummaryPager<String>({ it }) { cursor ->
            if (cursor == null) SummaryPage(listOf("first"), "same")
            else SummaryPage(listOf("duplicate"), "same")
        }
        pager.ensureLoaded()
        pager.load()
        assertEquals(listOf("first"), pager.items)
        assertNotNull(pager.error)
    }

    @Test fun cancellationDoesNotBecomeEmptyDataOrConsumeInitialization() = runBlocking {
        val pager = SummaryPager<String>({ it }) { throw CancellationException() }
        try { pager.ensureLoaded(); fail("Cancellation must propagate") } catch (_: CancellationException) { }
        assertFalse(pager.initialized)
        assertFalse(pager.loading)
        assertNull(pager.error)
    }
}
