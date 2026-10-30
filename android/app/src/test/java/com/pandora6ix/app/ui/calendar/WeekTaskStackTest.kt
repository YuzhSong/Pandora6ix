package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.TaskItem
import com.pandora6ix.app.mock.DemoDate
import org.junit.Assert.*
import org.junit.Test

class WeekTaskStackTest {
    private val date = DemoDate(2026, 10, 30)
    private fun task(id: String, from: Int?, until: Int?, end: DemoDate = date) = TaskItem(
        id, "PERSONAL", id, null, 3, date, end, if (from == null) "ALL_DAY" else "TIMED",
        from, until, "APPROVED", emptyList(), null, emptyList(), emptyList())

    @Test fun sortsByStartBeforeDurationOrPriority() {
        val early = task("early", 480, 510).copy(priorityLevel = 1)
        val late = task("late", 600, 1200).copy(priorityLevel = 5)
        assertEquals(listOf(early, late), weekTasksOnDate(listOf(late, early), date))
    }

    @Test fun sameStartUsesTotalDurationThenStableId() {
        val tasks = listOf(task("short", 480, 540), task("b", 480, 600),
            task("a", 480, 600), task("cross", 480, 500, date.plusDays(1)))
        val expected = listOf("cross", "a", "b", "short")
        assertEquals(expected, weekTasksOnDate(tasks, date).map { it.id })
        assertEquals(expected, weekTasksOnDate(tasks.reversed(), date).map { it.id })
    }

    @Test fun allDayMidnightAndInclusiveDurationAreRespected() {
        val tasks = listOf(task("timed", 0, 60), task("one-day", null, null),
            task("two-day", null, null, date.plusDays(1)))
        assertEquals(listOf("two-day", "one-day", "timed"), weekTasksOnDate(tasks, date).map { it.id })
        assertEquals(listOf("two-day"), weekTasksOnDate(tasks, date.plusDays(1)).map { it.id })
        assertTrue(weekTasksOnDate(tasks, date.plusDays(2)).isEmpty())
    }

    @Test fun continuationUsesOriginalStartAndExcludesZeroLengthLastDay() {
        val cross = task("cross", 1200, 600).copy(start = date.plusDays(-1))
        val early = task("early", 60, 120)
        assertEquals(listOf(cross, early), weekTasksOnDate(listOf(early, cross), date))
        val midnightEnd = cross.copy(endMinute = 0)
        assertTrue(weekTasksOnDate(listOf(midnightEnd), date).isEmpty())
        assertTrue(weekTasksOnDate(emptyList(), date).isEmpty())
    }
}
