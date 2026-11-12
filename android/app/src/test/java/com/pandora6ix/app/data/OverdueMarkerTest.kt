package com.pandora6ix.app.data

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 逾期标识：业务时区口径、未完成才标记、全天按当日 23:59（与后端 dueInstant 一致） */
class OverdueMarkerTest {
    private val shanghai = "Asia/Shanghai"
    // 2026-11-12T12:00:00Z = 上海 20:00
    private val now888 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear(); set(2026, 10, 12, 12, 0, 0)
    }.timeInMillis

    private fun task(
        endDate: String,
        timeKind: String = "ALL_DAY",
        endTime: String? = null,
        completionStatus: String? = "NOT_COMPLETED",
        reviewStatus: String = "APPROVED"
    ) = TaskDto(
        id = "t", title = "Task", endDate = endDate, timeKind = timeKind, endTime = endTime,
        completionStatus = completionStatus, reviewStatus = reviewStatus
    )

    @Test fun pastAllDayTaskIsOverdue() {
        assertTrue(task("2026-11-11").isTaskOverdue(shanghai, now888))
    }

    @Test fun sameDayAllDayTaskIsNotOverdueBefore2359() {
        assertFalse(task("2026-11-12").isTaskOverdue(shanghai, now888))
    }

    @Test fun timedTaskUsesEndTime() {
        assertTrue(task("2026-11-12", "TIMED", "19:00").isTaskOverdue(shanghai, now888))
        assertFalse(task("2026-11-12", "TIMED", "21:00").isTaskOverdue(shanghai, now888))
    }

    @Test fun completedOrAcceptedTasksAreNeverOverdue() {
        assertFalse(task("2026-11-11", completionStatus = "COMPLETED").isTaskOverdue(shanghai, now888))
        assertFalse(task("2026-11-11", completionStatus = "AWAITING_ACCEPTANCE").isTaskOverdue(shanghai, now888))
    }

    @Test fun unapprovedTaskIsNotMarked() {
        assertFalse(task("2026-11-11", reviewStatus = "PENDING").isTaskOverdue(shanghai, now888))
    }

    @Test fun timezoneChangesTheDeadlineInstant() {
        // 2026-11-12T22:00Z = 上海 11-13 06:00：全天任务 11-12 23:59(+08) 已过
        val now = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { clear(); set(2026, 10, 12, 22, 0, 0) }.timeInMillis
        assertTrue(task("2026-11-12").isTaskOverdue(shanghai, now))
        // 同一时刻按 UTC 业务时区，11-12 23:59 尚未到
        assertFalse(task("2026-11-12").isTaskOverdue("UTC", now))
    }
}
