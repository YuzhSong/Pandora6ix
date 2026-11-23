package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.DailyItem
import com.pandora6ix.app.data.DailyItemSchedule
import com.pandora6ix.app.data.schedule
import com.pandora6ix.app.data.withSchedule
import java.time.LocalDate
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class DailyItemSchedulingTest {
    private val monday = LocalDate.of(2026, 11, 30)
    private val tuesday = monday.plusDays(1)

    @Test fun oldRecordIsASingleAllDayItemAndKeepsCompletionState() {
        val old = Json.decodeFromString<DailyItem>(
            """{"id":"old","date":"2026-11-30","content":"旧事项","done":true}"""
        )
        assertEquals(DailyItemSchedule(monday, monday, null, null), old.schedule)
        assertEquals("旧事项", old.content)
        assertTrue(old.done)
    }

    @Test fun timedRangeRequiresLaterEndAndAllDayRangeIncludesEndDate() {
        assertFalse(DailyItemSchedule(monday, monday, 540, 540).isValid)
        assertFalse(DailyItemSchedule(monday, monday, 540, 539).isValid)
        assertFalse(DailyItemSchedule(monday, tuesday, null, 60).isValid)
        assertFalse(DailyItemSchedule(tuesday, monday, null, null).isValid)
        assertTrue(DailyItemSchedule(monday, monday, null, null).isValid)
        assertTrue(tuesday in DailyItemSchedule(monday, tuesday, null, null))
        assertTrue(DailyItemSchedule(monday, tuesday, 1320, 60).isValid)
    }

    @Test fun midnightEndDoesNotOccupyTheNextDate() {
        val schedule = DailyItemSchedule(monday, tuesday, 1320, 0)
        assertTrue(schedule.isValid)
        assertTrue(monday in schedule)
        assertFalse(tuesday in schedule)
    }

    @Test fun movingFromTheContinuationPreservesTimesAndIdentityAcrossMonthEnd() {
        val schedule = DailyItemSchedule(monday, tuesday, 1320, 60)
        val item = DailyItem("original", monday.toString(), "跨日事项", done = true).withSchedule(schedule)
        val thursday = tuesday.plusDays(2)
        val moved = checkNotNull(schedule.moved(tuesday, thursday))
        assertEquals(DailyItemSchedule(thursday.minusDays(1), thursday, 1320, 60), moved)
        val updated = item.withSchedule(moved)
        assertEquals(item.id, updated.id)
        assertEquals(item.content, updated.content)
        assertTrue(updated.done)
        assertEquals(updated, Json.decodeFromString<DailyItem>(Json.encodeToString(DailyItem.serializer(), updated)))
        assertNull(schedule.moved(tuesday, tuesday))
    }

    @Test fun movingAnAllDayRangeFromItsContinuationStartsItAtTheDropDate() {
        val schedule = DailyItemSchedule(monday, tuesday.plusDays(1), null, null)
        val dropDate = tuesday.plusDays(2)
        assertEquals(DailyItemSchedule(dropDate, dropDate.plusDays(2), null, null),
            schedule.moved(tuesday, dropDate))
        assertNull(schedule.moved(tuesday, tuesday))
        assertNull(schedule.moved(tuesday, monday))
        assertNull(schedule.moved(tuesday, LocalDate.of(2100, 12, 31)))
    }

    @Test fun sharedLayoutClipsContinuationAndCountsOverflowPerCoveredDate() {
        val dates = listOf(tuesday, tuesday.plusDays(1), tuesday.plusDays(2))
        val items = listOf(
            DailyItemSchedule(monday, dates[1], 1320, 60),
            DailyItemSchedule(tuesday, dates[2], null, null),
            DailyItemSchedule(dates[1], dates[2], 1320, 0)
        )
        val lanes = packCalendarRows(items, dates, { it.start }, { it.lastCoveredDay })
        assertEquals(listOf(0 to 1, 0 to 2, 1 to 1), lanes.flatten().map { it.first to it.last })
        assertArrayEquals(intArrayOf(1, 2, 1), hiddenCalendarCounts(lanes, dates.size, capacity = 1))
    }
}
