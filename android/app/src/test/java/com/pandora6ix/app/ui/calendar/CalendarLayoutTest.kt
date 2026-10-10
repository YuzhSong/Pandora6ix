package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.mock.*
import org.junit.Assert.*
import org.junit.Test

class CalendarLayoutTest {
    private val monday = DemoDate(2026, 9, 14)
    private val week = (0..6).map { monday.plusDays(it) }
    private fun task(id: String, first: Int, last: Int, priority: Int = 3) = WorkTask(id, id, monday.plusDays(first), monday.plusDays(last), "演示", "用户", 0, priorityLevel = priority)

    @Test fun monthHasOnlyRequiredWeeksAndNeverDropsEndOfMonth() {
        assertEquals(5, monthWeeks(DemoDate(2026, 9, 19)).size)
        assertEquals(DemoDate(2026, 10, 4), monthWeeks(DemoDate(2026, 9, 19)).last().last())
        assertEquals(6, monthWeeks(DemoDate(2026, 3, 1)).size)
        assertEquals(4, monthWeeks(DemoDate(2027, 2, 1)).size)
        for (year in 2025..2028) for (month in 1..12) {
            val dates = monthWeeks(DemoDate(year, month, 1)).flatten()
            assertTrue(dates.contains(DemoDate(year, month, 1)))
            assertTrue(dates.contains(DemoDate(year, month, 1).plusMonths(1).plusDays(-1)))
            assertEquals("一", dates.first().weekdayLabel())
            assertEquals("日", dates.last().weekdayLabel())
        }
    }
    @Test fun changingMonthsClampsEndOfMonth() {
        assertEquals(DemoDate(2026, 2, 28), DemoDate(2026, 1, 31).plusMonths(1))
        assertEquals(DemoDate(2028, 2, 29), DemoDate(2028, 3, 31).plusMonths(-1))
    }
    @Test fun continuousTasksClipAtWeekBoundaryAndReuseNonOverlappingLanes() {
        val lanes = packTaskRows(listOf(task("a", -2, 2), task("b", 3, 8)), week)
        assertEquals(1, lanes.size)
        assertEquals(listOf(0 to 2, 3 to 6), lanes.first().map { it.first to it.last })
    }
    @Test fun overlappingTasksNeverShareLaneAndPriorityComesFirst() {
        val lanes = packTaskRows(listOf(task("a", 1, 5), task("b", 2, 3, 5), task("c", 9, 10)), week)
        assertEquals(2, lanes.size)
        assertEquals("b", lanes.first().first().task.id)
        for (lane in lanes) for (i in lane.indices) for (j in i + 1 until lane.size) assertTrue(lane[i].last < lane[j].first)
    }
    @Test fun customTimesAndAllDayRemainSeparateAndCrossMidnightClipsCorrectly() {
        val timed = task("timed", 0, 2).copy(startMinute = 22 * 60, endMinute = 60)
        assertEquals(1320 until 1440, timeRangeOnDate(timed, monday))
        assertEquals(0 until 1440, timeRangeOnDate(timed, monday.plusDays(1)))
        assertEquals(0 until 60, timeRangeOnDate(timed, monday.plusDays(2)))
        assertNull(timeRangeOnDate(timed, monday.plusDays(3)))
        assertNull(timeRangeOnDate(task("allDay", 0, 1), monday))
    }
    @Test fun invalidIntervalsAreRejectedAndEmployeesHaveNoCreatePermission() {
        assertTrue(validTaskInterval(monday, monday, null, null))
        assertTrue(validTaskInterval(monday, monday, 540, 600))
        assertFalse(validTaskInterval(monday, monday, 600, 540))
        assertFalse(validTaskInterval(monday, monday, 600, 600))
        assertFalse(validTaskInterval(monday, monday.plusDays(-1), null, null))
        assertFalse(validTaskInterval(monday, monday, 600, null))
        assertFalse(validTaskInterval(monday, monday, -1, 600))
        assertFalse(DemoRole.EMPLOYEE.canCreateTasks)
        assertTrue(DemoRole.MANAGER.canCreateTasks)
    }
}
