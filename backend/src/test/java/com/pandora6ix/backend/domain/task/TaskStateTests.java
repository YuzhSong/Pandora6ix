package com.pandora6ix.backend.domain.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class TaskStateTests {
    @Test
    void taskKindAndTimeKindRemainStable() {
        assertEquals(TaskKind.PERSONAL, TaskKind.valueOf("PERSONAL"));
        assertEquals(TaskKind.DELEGATED, TaskKind.valueOf("DELEGATED"));
        assertEquals(TimeKind.ALL_DAY, TimeKind.valueOf("ALL_DAY"));
        assertEquals(TimeKind.TIMED, TimeKind.valueOf("TIMED"));
    }

    @Test
    void workflowStatesExposeRequiredValues() {
        assertTrue(Arrays.asList(ReviewStatus.values()).containsAll(
                Arrays.asList(ReviewStatus.PENDING, ReviewStatus.APPROVED, ReviewStatus.REJECTED)));
        assertTrue(Arrays.asList(AssignmentExecutionStatus.values()).containsAll(
                Arrays.asList(
                        AssignmentExecutionStatus.PENDING_APPROVAL,
                        AssignmentExecutionStatus.ACTIVE,
                        AssignmentExecutionStatus.AWAITING_ACCEPTANCE,
                        AssignmentExecutionStatus.COMPLETED)));
    }
}
