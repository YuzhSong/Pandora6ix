package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.mock.DemoDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSpanFollowTest {
    private val nov3 = DemoDate(2026, 11, 3)
    private val nov4 = DemoDate(2026, 11, 4)

    @Test fun laterStartTimePushesEndByPreviousDuration() {
        val span = followStart(TaskSpan(nov3, 9 * 60, nov3, 10 * 60 + 30), nov3, 14 * 60, allDay = false, repeating = false)
        assertEquals(TaskSpan(nov3, 14 * 60, nov3, 15 * 60 + 30), span)
    }

    @Test fun validLaterEndIsKept() {
        val span = followStart(TaskSpan(nov3, 9 * 60, nov4, 18 * 60), nov3, 14 * 60, allDay = false, repeating = false)
        assertEquals(TaskSpan(nov3, 14 * 60, nov4, 18 * 60), span)
    }

    @Test fun lateStartCrossesMidnight() {
        val span = followStart(TaskSpan(nov3, 9 * 60, nov3, 10 * 60), nov3, 23 * 60 + 30, allDay = false, repeating = false)
        assertEquals(TaskSpan(nov3, 23 * 60 + 30, nov4, 30), span)
    }

    @Test fun laterStartDateMovesEndDateKeepingTime() {
        val span = followStart(TaskSpan(nov3, 9 * 60, nov3, 10 * 60), DemoDate(2026, 11, 10), 9 * 60, allDay = false, repeating = false)
        assertEquals(TaskSpan(DemoDate(2026, 11, 10), 9 * 60, DemoDate(2026, 11, 10), 10 * 60), span)
    }

    @Test fun allDayKeepsDaySpan() {
        val span = followStart(TaskSpan(nov3, 0, nov4, 0), DemoDate(2026, 11, 30), 0, allDay = true, repeating = false)
        assertEquals(DemoDate(2026, 12, 1), span.endDate)
    }

    @Test fun zeroDurationFallsBackToOneHour() {
        val span = followStart(TaskSpan(nov3, 9 * 60, nov3, 9 * 60), nov3, 9 * 60, allDay = false, repeating = false)
        assertEquals(10 * 60, span.endMinute)
    }

    @Test fun repeatingKeepsOccurrenceWithinTheDay() {
        val span = followStart(TaskSpan(nov3, 9 * 60, DemoDate(2026, 11, 30), 10 * 60), nov3, 23 * 60 + 30, allDay = false, repeating = true)
        assertEquals(DemoDate(2026, 11, 30), span.endDate)
        assertEquals(23 * 60 + 59, span.endMinute)
    }
}
