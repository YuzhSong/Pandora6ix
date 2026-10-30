package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.TaskItem
import com.pandora6ix.app.mock.DemoDate
import org.junit.Assert.*
import org.junit.Test

class WeekAllDayBarsTest {
    private val monday = DemoDate(2026, 10, 26)
    private val dates = (0..6).map { monday.plusDays(it) }
    private fun task(id: String, first: Int, last: Int) = TaskItem(
        id, "PERSONAL", id, null, 3, monday.plusDays(first), monday.plusDays(last),
        "ALL_DAY", null, null, "APPROVED", emptyList(), null, emptyList(), emptyList())

    @Test fun spansCoveredDaysAndClipsWeekBoundaries() {
        val rows = weekAllDayRows(listOf(task("whole", -2, 9), task("three", 1, 3)), dates)
        assertEquals(listOf(0 to 6, 1 to 3), rows.flatten().map { it.first to it.last })
        assertEquals(2, rows.size)
    }

    @Test fun overlapsSplitRowsAndDisjointBarsReuseRows() {
        val tasks = listOf(task("long", 0, 3), task("short", 0, 1), task("later", 2, 2))
        val rows = weekAllDayRows(tasks, dates)
        assertEquals(listOf("long"), rows[0].map { it.task.id })
        assertEquals(listOf("short", "later"), rows[1].map { it.task.id })
        assertEquals(rows, weekAllDayRows(tasks.reversed(), dates))
    }

    @Test fun ignoresTimedAndOutsideTasksAndHandlesEmptyWeek() {
        val timed = task("timed", 1, 2).copy(startMinute = 60, endMinute = 120, timeKind = "TIMED")
        assertTrue(weekAllDayRows(listOf(timed, task("before", -3, -1), task("after", 7, 9)), dates).isEmpty())
        assertTrue(weekAllDayRows(listOf(task("all", 0, 0)), emptyList()).isEmpty())
        assertEquals(0 to 0, weekAllDayRows(listOf(task("one", 0, 0)), dates).single().single().let { it.first to it.last })
    }
}
