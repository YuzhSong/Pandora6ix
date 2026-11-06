package com.pandora6ix.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskCompletionTest {
    @Test fun taskStateWinsOverPartialAssignments() {
        val partial = listOf(AssignmentDto("a", executionStatus = "ACTIVE", workStatus = "NOT_STARTED"))
        val task = TaskDto("t", authorizedAssignments = partial, completionStatus = "AWAITING_ACCEPTANCE")
        assertEquals("已完成，待验收", taskCompletionLabel(task.taskCompletionState()))
        assertEquals("已完成", taskCompletionLabel(task.copy(completionStatus = "COMPLETED").taskCompletionState()))
    }

    @Test fun legacyMixedStatesHaveOneCompletionResult() {
        val task = TaskDto("t", authorizedAssignments = listOf(
            AssignmentDto("a", executionStatus = "COMPLETED"), AssignmentDto("b", executionStatus = "ACTIVE")))
        assertEquals("已完成", taskCompletionLabel(task.taskCompletionState()))
        assertEquals("未完成", taskCompletionLabel(TaskDto("empty").taskCompletionState()))
        assertEquals("未完成", executionStatusLabel("ACTIVE"))
        assertEquals("已完成，待验收", executionStatusLabel("AWAITING_ACCEPTANCE"))
    }
}
