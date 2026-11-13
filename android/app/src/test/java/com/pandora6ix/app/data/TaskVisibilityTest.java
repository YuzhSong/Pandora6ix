package com.pandora6ix.app.data;

import java.util.List;
import kotlinx.serialization.json.Json;
import org.junit.Test;
import static org.junit.Assert.*;

public class TaskVisibilityTest {
    private TaskDto task(String id, String review) {
        return Json.Default.decodeFromString(TaskDto.Companion.serializer(),
                "{\"id\":\"" + id + "\",\"reviewStatus\":\"" + review + "\"}");
    }

    @Test public void ordinaryPagesKeepPendingAndApprovedTasksInOrder() {
        TaskDto pending = task("pending", "PENDING");
        TaskDto approved = task("approved", "APPROVED");
        TaskDto rejected = task("rejected", "REJECTED");
        TaskPageDto original = new TaskPageDto(List.of(pending, rejected, approved), "next", "/tasks");
        TaskPageDto result = TaskVisibilityKt.withoutRejectedTasks(original);
        assertEquals(List.of(pending, approved), result.getItems());
        assertEquals("next", result.getNextCursor());
        assertEquals("/tasks", result.getMoreUrl());
        assertEquals(3, original.getItems().size());
    }

    @Test public void emptyFilteredPageStillAllowsLoadingTheFollowingPage() {
        TaskPageDto first = TaskVisibilityKt.withoutRejectedTasks(
                new TaskPageDto(List.of(task("rejected", "REJECTED")), "second", "/tasks"));
        assertTrue(first.getItems().isEmpty());
        assertEquals("second", first.getNextCursor());
        TaskDto visible = task("visible", "APPROVED");
        TaskPageDto second = TaskVisibilityKt.withoutRejectedTasks(new TaskPageDto(List.of(visible), null, "/tasks"));
        assertEquals(List.of(visible), second.getItems());
        assertNull(second.getNextCursor());
    }

    @Test public void completionRejectionDoesNotHideAnApprovedTask() {
        TaskDto task = Json.Default.decodeFromString(TaskDto.Companion.serializer(),
                "{\"id\":\"active\",\"reviewStatus\":\"APPROVED\",\"authorizedAssignments\":[{\"id\":\"a\",\"executionStatus\":\"REJECTED\"}]}");
        assertEquals(List.of(task), TaskVisibilityKt.withoutRejectedTasks(new TaskPageDto(List.of(task), null, null)).getItems());
    }

    @Test public void calendarFilteringRetainsServerDateRangeAndPagination() {
        for (String mode : List.of("day", "week", "month")) {
            TaskDto visible = task("visible", "APPROVED");
            CalendarDto calendar = new CalendarDto(mode, "2026-11-13", "2026-11-01", "2026-11-30",
                    "2026-11-13", "Asia/Shanghai", List.of(task("rejected", "REJECTED"), visible), "next", 2L);
            CalendarDto result = TaskVisibilityKt.withoutRejectedTasks(calendar);
            assertEquals(List.of(visible), result.getTasks());
            assertEquals(calendar.getMode(), result.getMode());
            assertEquals(calendar.getRangeStart(), result.getRangeStart());
            assertEquals(calendar.getRangeEnd(), result.getRangeEnd());
            assertEquals(calendar.getBusinessTimezone(), result.getBusinessTimezone());
            assertEquals(calendar.getNextCursor(), result.getNextCursor());
            assertEquals(calendar.getTotalCount(), result.getTotalCount());
        }
    }
}
