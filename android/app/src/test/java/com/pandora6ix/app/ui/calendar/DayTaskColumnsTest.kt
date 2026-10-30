package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.TaskItem
import com.pandora6ix.app.mock.*
import org.junit.Assert.*
import org.junit.Test

class DayTaskColumnsTest {
    private val date = DemoDate(2026, 10, 30)
    private fun timed(id: String, from: Int, until: Int) =
        TaskItem(id = id, kind = "PERSONAL", title = id, description = null, priorityLevel = 3,
            start = date, end = date, timeKind = "TIMED", startMinute = from, endMinute = until,
            reviewStatus = "NONE", tags = emptyList(), displayColorHex = null,
            assignments = emptyList(), allowedActions = emptyList())

    @Test fun overlappingPairUsesTwoColumnsAndKeepsExactTimes() {
        val placed = parallelTimedTasks(listOf(timed("a", 540, 660), timed("b", 600, 720)), date)
        assertEquals(listOf(0, 1), placed.map { it.depth })
        assertEquals(listOf(2, 2), placed.map { it.columnCount })
        assertEquals(listOf(540 until 660, 600 until 720), placed.map { it.range })
    }

    @Test fun chainedOverlapsReuseLeftmostColumnAndSeparateGroupsResetWidth() {
        val tasks = listOf(timed("a", 60, 180), timed("b", 120, 240),
            timed("c", 180, 300), timed("d", 300, 360))
        val placed = parallelTimedTasks(tasks, date)
        assertEquals(listOf(0, 1, 0, 0), placed.map { it.depth })
        assertEquals(listOf(2, 2, 2, 1), placed.map { it.columnCount })
    }

    @Test fun nestedAndEqualStartsHaveStableColumns() {
        val tasks = listOf(timed("b", 60, 180), timed("a", 60, 180),
            timed("long", 0, 600), timed("later", 180, 240))
        val placed = parallelTimedTasks(tasks, date)
        assertEquals(listOf("long", "a", "b", "later"), placed.map { it.task.id })
        assertEquals(listOf(0, 1, 2, 1), placed.map { it.depth })
        assertTrue(placed.all { it.columnCount == 3 })
        assertEquals(placed, parallelTimedTasks(tasks.reversed(), date))
    }

    @Test fun clipsCrossDayAndAllDayAndExcludesOtherDates() {
        val all = timed("all", 0, 1).copy(startMinute = null, endMinute = null)
        val cross = timed("cross", 1320, 120).copy(start = date.plusDays(-1))
        val tasks = listOf(all, cross, timed("inner", 60, 180),
            timed("other", 60, 180).copy(start = date.plusDays(1), end = date.plusDays(1)))
        val placed = parallelTimedTasks(tasks, date)
        assertEquals(listOf(0 until 1440, 0 until 120, 60 until 180), placed.map { it.range })
        assertTrue(placed.all { it.columnCount == 3 })
        assertEquals(tasks.take(3), placed.map { it.task })
        assertTrue(parallelTimedTasks(emptyList(), date).isEmpty())
    }

    @Test fun denseScheduleHasNoColumnCollisionsAndUsesPeakConcurrency() {
        val tasks = (0 until 60).map { i -> timed("task-$i", i * 17, i * 17 + 30 + (i % 5) * 13) }
        val placed = parallelTimedTasks(tasks, date)
        assertEquals(tasks.size, placed.size)
        for (minute in 0 until 1440) {
            val active = placed.filter { minute in it.range }
            assertEquals(active.size, active.map { it.depth }.distinct().size)
            assertTrue(active.all { it.depth in 0 until it.columnCount })
        }
        val peak = (0 until 1440).maxOf { minute -> placed.count { minute in it.range } }
        assertTrue(placed.all { it.columnCount == peak })
        assertEquals(placed, parallelTimedTasks(tasks.shuffled(kotlin.random.Random(42)), date))
    }
}
