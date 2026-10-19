package com.pandora6ix.app.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TaskReminderTest {
    private val now = businessDayMillis("2026-10-18", "12:00", "Asia/Shanghai")!!
    private fun task(id: String = "task", owner: String = "me", status: String = "ACTIVE",
                     review: String = "APPROVED") = TaskDto(
        id = id, title = id, kind = "DELEGATED", endDate = "2026-10-18", endTime = "13:00",
        reviewStatus = review, authorizedAssignments = listOf(
            AssignmentDto(id = "assignment-$id", recipientUserId = owner, executionStatus = status)))

    private fun derive(tasks: List<TaskDto>, initialized: Boolean = true,
                       previous: Map<String, String> = emptyMap()) = deriveReminders(
        tasks, "me", previous, initialized, true, true, true, 120, "Asia/Shanghai", now)

    @Test fun emptyBaselineStillRecognizesFirstNewTask() {
        assertFalse(derive(listOf(task()), initialized = false).events.any { it.kind == ReminderKind.NEW_TASK })
        assertTrue(derive(listOf(task()), initialized = true).events.any { it.kind == ReminderKind.NEW_TASK })
    }

    @Test fun pendingRejectedAndOtherRecipientsAreSilent() {
        val batch = derive(listOf(task("pending", review = "PENDING"),
            task("rejected", review = "REJECTED"), task("other", owner = "other")))
        assertTrue(batch.events.isEmpty())
        assertTrue(batch.snapshot.isEmpty())
    }

    @Test fun anotherRecipientsPendingWorkDoesNotRemindCompletedOwner() {
        val completed = task(status = "COMPLETED").let { it.copy(authorizedAssignments =
            it.authorizedAssignments + AssignmentDto("other-assignment", recipientUserId = "other", executionStatus = "ACTIVE")) }
        assertTrue(derive(listOf(completed)).events.isEmpty())
    }

    @Test fun unchangedStateIsQuietButCompletionIsReported() {
        val previous = derive(listOf(task())).snapshot
        assertFalse(derive(listOf(task()), previous = previous).events.any { it.kind == ReminderKind.STATUS })
        val completed = derive(listOf(task(status = "COMPLETED")), previous = previous).events
        assertEquals(listOf(ReminderKind.STATUS), completed.map { it.kind })
    }

    @Test fun allPagesParticipateInBaselineAndNewTaskDetection() = runBlocking {
        val requested = mutableListOf<String?>()
        val tasks = loadReminderTasks { cursor ->
            requested += cursor
            if (cursor == null) TaskPageDto((1..100).map { task("old-$it") }, "next")
            else TaskPageDto(listOf(task("new")))
        }
        val previous = derive(tasks.dropLast(1)).snapshot
        assertEquals(listOf(null, "next"), requested)
        assertEquals(101, derive(tasks, previous = previous).snapshot.size)
        assertEquals(1, derive(tasks, previous = previous).events.count { it.kind == ReminderKind.NEW_TASK })
    }

    @Test fun repeatedCursorFailsInsteadOfSavingPartialBaseline() = runBlocking {
        try {
            loadReminderTasks { TaskPageDto(listOf(task()), "same") }
            fail("Expected a repeated cursor failure")
        } catch (_: IllegalStateException) { }
    }
}
