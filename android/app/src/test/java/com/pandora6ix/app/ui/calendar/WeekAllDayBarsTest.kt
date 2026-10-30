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

    @Test fun continuousInteriorUsesBarsButEndpointsRemainCards() {
        val continuous = task("continuous", 0, 6).copy(startMinute = 540, endMinute = 600, timeKind = "TIMED")
        val segment = weekAllDayRows(listOf(continuous), dates).single().single()
        assertEquals(1 to 5, segment.first to segment.last)
        assertEquals(continuous, segment.task)
        dates.forEachIndexed { index, date ->
            assertEquals(index in 1..5, coversFullDay(continuous, date))
            assertEquals(index == 0 || index == 6, weekCardTasksOnDate(listOf(continuous), date).isNotEmpty())
        }
    }

    @Test fun midnightBoundariesDoNotDuplicateOrShowEmptyFinalDate() {
        val continuous = task("midnight", 0, 3).copy(startMinute = 0, endMinute = 0, timeKind = "TIMED")
        val segment = weekAllDayRows(listOf(continuous), dates).single().single()
        assertEquals(0 to 2, segment.first to segment.last)
        assertTrue(weekCardTasksOnDate(listOf(continuous), dates[3]).isEmpty())
        assertFalse(coversFullDay(continuous, dates[3]))
    }

    @Test fun explicit2359CoverageUsesBarButShorterIntervalDoesNot() {
        val full = task("full", 1, 1).copy(startMinute = 0, endMinute = 1439, timeKind = "TIMED")
        assertTrue(coversFullDay(full, dates[1]))
        assertFalse(coversFullDay(full.copy(endMinute = 1438), dates[1]))
        assertFalse(coversFullDay(full.copy(startMinute = 1), dates[1]))
    }

    @Test fun longContinuousTaskClipsFullDaysAcrossWeekBoundary() {
        val continuous = task("long", -30, 40).copy(startMinute = 540, endMinute = 600, timeKind = "TIMED")
        val all = task("all", 1, 2)
        val rows = weekAllDayRows(listOf(all, continuous), dates)
        assertEquals(2, rows.size)
        assertEquals(0 to 6, rows[0].single().let { it.first to it.last })
        assertTrue(dates.all { weekCardTasksOnDate(listOf(all, continuous), it).isEmpty() })
    }
}
