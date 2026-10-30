package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.mock.*
import com.pandora6ix.app.data.TaskItem
import org.junit.Assert.*
import org.junit.Test

class CalendarLayoutTest {
    @Test fun timelineSelectionOnlyIncludesTasksAtTappedMinute() {
        val all = task("all", 0, 0)
        val short = task("short", 0, 0).copy(startMinute = 600, endMinute = 660)
        val later = task("later", 0, 0).copy(startMinute = 900, endMinute = 960)
        val tasks = listOf(later, short, all)
        assertEquals(listOf(all), timelineTasksAt(tasks, monday, 540))
        assertEquals(listOf(all, short), timelineTasksAt(tasks, monday, 630))
        assertEquals(listOf(all, short), timelineTasksAt(tasks, monday, 600))
        assertEquals(listOf(all), timelineTasksAt(tasks, monday, 660))
        assertEquals(listOf(all), timelineTasksAt(tasks, monday, 690))
        assertTrue(timelineTasksAt(tasks, monday.plusDays(1), 630).isEmpty())
        assertTrue(timelineTasksAt(tasks, monday, 1440).isEmpty())
        assertTrue(timelineTasksAt(tasks, monday, -1).isEmpty())
    }

    @Test fun timelineSelectionHandlesEqualStartsAndClippedCrossDayTasks() {
        val cross = task("cross", -1, 0).copy(startMinute = 1320, endMinute = 60)
        val a = task("a", 0, 0).copy(startMinute = 0, endMinute = 60)
        val b = a.copy(id = "b")
        val tasks = listOf(b, cross, a)
        assertEquals(listOf("a", "b", "cross"), timelineTasksAt(tasks, monday, 30).map { it.id })
        assertTrue(timelineTasksAt(tasks, monday, 60).isEmpty())
        assertEquals(listOf(cross), timelineTasksAt(tasks, monday.plusDays(-1), 1439))
        assertEquals(timelineTasksAt(tasks, monday, 0), timelineTasksAt(tasks.reversed(), monday, 0))
    }
    @Test fun allDayAndTimedTasksShareDayAndWeekTimelineWithoutChangingTheirTimeData() {
        val fullDay = task("full", 0, 0).copy(startMinute = 0, endMinute = 1439)
        val multiDay = fullDay.copy(id = "multi", start = monday.plusDays(-2), end = monday.plusDays(8))
        val nearStart = fullDay.copy(id = "near-start", startMinute = 1)
        val nearEnd = fullDay.copy(id = "near-end", endMinute = 1438)
        val partialCrossDay = multiDay.copy(id = "partial", startMinute = 1320, endMinute = 60)
        val allDay = task("all", 0, 1)
        assertEquals(0 until 1440, timelineTimeRangeOnDate(allDay, monday))
        assertEquals(0 until 1440, timelineTimeRangeOnDate(allDay, monday.plusDays(1)))
        assertNull(timelineTimeRangeOnDate(allDay, monday.plusDays(-1)))
        assertNull(timelineTimeRangeOnDate(allDay, monday.plusDays(2)))
        assertNull(timeRangeOnDate(allDay, monday))
        assertEquals(0 until 1439, timelineTimeRangeOnDate(fullDay, monday))
        assertEquals(1 until 1439, timelineTimeRangeOnDate(nearStart, monday))
        assertEquals(0 until 1438, timelineTimeRangeOnDate(nearEnd, monday))
        val placed = staggerTimedTasks(listOf(fullDay, allDay), monday)
        assertEquals(listOf("all", "full"), placed.map { it.task.id })
        assertEquals(listOf(0, 1), placed.map { it.depth })
        assertEquals(allDay, placed.first().task)
        assertFalse(isAllDay(fullDay))
        assertEquals(0 until 1439, timeRangeOnDate(fullDay, monday))
        assertEquals(0 until 1440, timeRangeOnDate(partialCrossDay, monday))
        val segment = packTaskRows(listOf(multiDay), week).single().single()
        assertEquals(0, segment.first)
        assertEquals(6, segment.last)
        assertEquals(multiDay, segment.task)
    }
    @Test fun zoomNavigatesAllModesAndStopsAtBoundaries() {
        assertEquals(CalendarMode.DAY, CalendarMode.DAY.zoomIn())
        assertEquals(CalendarMode.DAY, CalendarMode.WEEK.zoomIn())
        assertEquals(CalendarMode.WEEK, CalendarMode.MONTH.zoomIn())
        assertEquals(CalendarMode.MONTH, CalendarMode.YEAR.zoomIn())
        assertEquals(CalendarMode.WEEK, CalendarMode.DAY.zoomOut())
        assertEquals(CalendarMode.MONTH, CalendarMode.WEEK.zoomOut())
        assertEquals(CalendarMode.YEAR, CalendarMode.MONTH.zoomOut())
        assertEquals(CalendarMode.YEAR, CalendarMode.YEAR.zoomOut())
    }
    private val monday = DemoDate(2026, 9, 14)
    private val week = (0..6).map { monday.plusDays(it) }
    private fun task(id: String, first: Int, last: Int, priority: Int = 3) = TaskItem(
        id, "PERSONAL", id, null, priority, monday.plusDays(first), monday.plusDays(last),
        "ALL_DAY", null, null, "APPROVED", emptyList(), null, emptyList(), emptyList())

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

    @Test fun nestedTasksKeepUnderlyingEdgesAndResetAfterTouchingEndpoint() {
        val tasks = listOf(
            task("long", 0, 0).copy(startMinute = 0, endMinute = 600),
            task("inner", 0, 0).copy(startMinute = 60, endMinute = 120),
            task("later", 0, 0).copy(startMinute = 180, endMinute = 240),
            task("touching", 0, 0).copy(startMinute = 600, endMinute = 660))
        val placed = staggerTimedTasks(tasks.reversed(), monday)
        assertEquals(listOf(0, 1, 1, 0), placed.map { it.depth })
        assertEquals(0 until 600, placed.first().range)
    }

    @Test fun equalStartsAreStableAndChainedOverlapKeepsIncreasingDepth() {
        val tasks = listOf(
            task("b", 0, 0).copy(startMinute = 60, endMinute = 180),
            task("a", 0, 0).copy(startMinute = 60, endMinute = 180),
            task("long", 0, 0).copy(startMinute = 60, endMinute = 240),
            task("chain", 0, 0).copy(startMinute = 200, endMinute = 300))
        val placed = staggerTimedTasks(tasks, monday)
        assertEquals(listOf("long", "a", "b", "chain"), placed.map { it.task.id })
        assertEquals(listOf(0, 1, 2, 1), placed.map { it.depth })
        assertEquals(placed, staggerTimedTasks(tasks.reversed(), monday))
    }

    @Test fun crossDayOverlapUsesClippedRangeAndIncludesAllDayTasks() {
        val tasks = listOf(task("all", 0, 1),
            task("cross", -1, 0).copy(startMinute = 1320, endMinute = 120),
            task("inner", 0, 0).copy(startMinute = 60, endMinute = 180))
        val placed = staggerTimedTasks(tasks, monday)
        assertEquals(listOf(0, 1, 2), placed.map { it.depth })
        assertEquals(0 until 1440, placed.first().range)
        assertEquals(0 until 120, placed[1].range)
        assertEquals(listOf(0, 0, 1), staggerTimedTasks(listOf(
            task("a", 0, 0).copy(startMinute = 0, endMinute = 60),
            task("b", 0, 0).copy(startMinute = 60, endMinute = 180),
            task("c", 0, 0).copy(startMinute = 120, endMinute = 240)), monday).map { it.depth })
    }
}
