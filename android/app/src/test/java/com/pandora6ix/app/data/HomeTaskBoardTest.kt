package com.pandora6ix.app.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class HomeTaskBoardTest {
    private fun assignment(user: String, status: String) = AssignmentDto(
        id = user, recipientUserId = user, executionStatus = status,
        workStatus = "COMPLETED", progressPercent = 100
    )

    private fun task(id: String, mine: String?, other: String = "ACTIVE") = TaskDto(
        id = id, title = id,
        authorizedAssignments = listOfNotNull(
            mine?.let { assignment("me", it) }, assignment("other", other)
        )
    )

    @Test fun filtersTaskWideCompletedStateAndPreservesOrderAndIds() = runBlocking {
        val tasks = listOf(
            task("my-completed", "COMPLETED"),
            task("active", "ACTIVE"),
            task("awaiting", "AWAITING_ACCEPTANCE"),
            task("rejected", "REJECTED"),
            task("missing-own-assignment", null, "COMPLETED")
        )
        val result = loadHomeTaskBoardPage { TaskPageDto(tasks, "next") }
        assertEquals(tasks.subList(1,4), result.items)
        assertEquals("next", result.nextCursor)
    }

    @Test fun skipsConsecutiveCompletedPagesOnInitialLoad() = runBlocking {
        val calls = mutableListOf<String?>()
        val result = loadHomeTaskBoardPage { cursor ->
            calls.add(cursor)
            when (cursor) {
                null -> TaskPageDto(listOf(task("done1", "COMPLETED")), "second")
                "second" -> TaskPageDto(listOf(task("done2", "COMPLETED")), "third")
                else -> TaskPageDto(listOf(task("visible", "ACTIVE")), "fourth")
            }
        }
        assertEquals(listOf(null, "second", "third"), calls)
        assertEquals(listOf("visible"), result.items.map { it.id })
        assertEquals("fourth", result.nextCursor)
    }

    @Test fun loadMoreSkipsCompletedPageAndReturnsVisibleTasks() = runBlocking {
        val calls = mutableListOf<String?>()
        val result = loadHomeTaskBoardPage("more") { cursor ->
            calls.add(cursor)
            if (cursor == "more") TaskPageDto(listOf(task("done", "COMPLETED")), "last")
            else TaskPageDto(listOf(task("pending", "AWAITING_ACCEPTANCE")))
        }
        assertEquals(listOf("more", "last"), calls)
        assertEquals("pending", result.items.single().id)
        assertNull(result.nextCursor)
    }

    @Test fun allCompletedReachesEmptyTerminalPage() = runBlocking {
        var calls = 0
        val result = loadHomeTaskBoardPage { cursor ->
            calls++
            TaskPageDto(listOf(task("done", "COMPLETED")), if (cursor == null) "last" else null)
        }
        assertEquals(2, calls)
        assertTrue(result.items.isEmpty())
        assertNull(result.nextCursor)
    }

    @Test fun failureCanRetryFromOriginalCursorWithoutLosingTasks() = runBlocking {
        var shouldFail = true
        val calls = mutableListOf<String?>()
        val fetch: suspend (String?) -> TaskPageDto = { cursor ->
            calls.add(cursor)
            if (cursor == "start") TaskPageDto(listOf(task("done", "COMPLETED")), "later")
            else {
                if (shouldFail) throw IllegalStateException("network")
                TaskPageDto(listOf(task("active", "ACTIVE")))
            }
        }
        try {
            loadHomeTaskBoardPage("start", fetch)
            fail("Expected fetch failure")
        } catch (expected: IllegalStateException) {
            assertEquals("network", expected.message)
        }
        shouldFail = false
        val result = loadHomeTaskBoardPage("start", fetch)
        assertEquals(listOf("start", "later", "start", "later"), calls)
        assertEquals("active", result.items.single().id)
    }

    @Test fun repeatedCursorFailsInsteadOfLooping() = runBlocking {
        try {
            loadHomeTaskBoardPage("loop") {
                TaskPageDto(listOf(task("done", "COMPLETED")), "loop")
            }
            fail("Expected repeated cursor failure")
        } catch (expected: IllegalStateException) {
            assertTrue(expected.message!!.contains("游标"))
        }
    }

    @Test fun authoritativeTaskStateWinsAndPendingRemainsVisible() = runBlocking {
        val completed = task("done", "ACTIVE").copy(completionStatus = "COMPLETED")
        val pending = task("pending", "ACTIVE").copy(completionStatus = "AWAITING_ACCEPTANCE")
        val active = task("active", "COMPLETED").copy(completionStatus = "NOT_COMPLETED")
        assertEquals(listOf(pending,active), loadHomeTaskBoardPage { TaskPageDto(listOf(completed,pending,active)) }.items)
    }
}
