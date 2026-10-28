package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.mock.*
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class CalendarScrollCalculationTest {
    @Test fun fastOrderingMatchesCalendarOrderingForValidDates() {
        for (year in listOf(1900, 1990, 2000, 2026, 2028, 2030, 2090, 2100)) {
            for (month in 1..12) {
                val first = DemoDate(year, month, 1)
                val last = first.plusMonths(1).plusDays(-1)
                val dates = listOf(first.plusDays(-1), first, last, last.plusDays(1))
                for (left in dates) for (right in dates) {
                    assertEquals(left.ordinal().compareTo(right.ordinal()).compareTo(0), left.compareTo(right).compareTo(0))
                }
            }
        }
    }

    @Test fun generatedWeeksMatchPreviousAlgorithmAcrossLeapYearsAndDst() {
        val originalZone = TimeZone.getDefault()
        try {
            for (zone in listOf("Asia/Shanghai", "America/New_York", "Europe/Berlin")) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone))
                for (year in listOf(1900, 2000, 2026, 2028, 2030, 2100)) for (month in 1..12) {
                    val first = DemoDate(year, month, 1)
                    val monday = mondayOfWeek(first)
                    val last = first.plusMonths(1).plusDays(-1)
                    val count = ((daysBetween(monday, last) + 7) / 7) * 7
                    val previous = (0 until count).map { monday.plusDays(it) }.chunked(7)
                    assertEquals("$zone $year-$month", previous, monthWeeks(first))
                }
            }
        } finally {
            TimeZone.setDefault(originalZone)
        }
    }

    @Test fun monthRowsHaveDistinctKeysEvenWithMatchingNeighborDayNumbers() {
        val index = buildMonthScrollIndex(2021, 2023)
        assertEquals(index.items.size, index.items.map { it.key }.toSet().size)
    }

    @Test fun selectionWithinOnePeriodKeepsQueryAnchor() {
        val date = DemoDate(2030, 6, 12)
        assertEquals(calendarQueryDate(CalendarMode.YEAR, date), calendarQueryDate(CalendarMode.YEAR, DemoDate(2030, 12, 31)))
        assertEquals(calendarQueryDate(CalendarMode.MONTH, date), calendarQueryDate(CalendarMode.MONTH, DemoDate(2030, 6, 30)))
        val monday = mondayOfWeek(date)
        for (day in 0..6) {
            assertEquals(monday, calendarQueryDate(CalendarMode.WEEK, monday.plusDays(day)))
        }
        assertEquals(date, calendarQueryDate(CalendarMode.DAY, date))
    }

    @Test fun crossingPeriodBoundariesInvalidatesQuery() {
        val date = DemoDate(2030, 12, 31)
        assertNotEquals(calendarQueryDate(CalendarMode.YEAR, date), calendarQueryDate(CalendarMode.YEAR, date.plusDays(1)))
        assertNotEquals(calendarQueryDate(CalendarMode.MONTH, date), calendarQueryDate(CalendarMode.MONTH, date.plusDays(1)))
        val monday = mondayOfWeek(date)
        assertNotEquals(calendarQueryDate(CalendarMode.WEEK, monday), calendarQueryDate(CalendarMode.WEEK, monday.plusDays(7)))
        assertNotEquals(calendarQueryDate(CalendarMode.DAY, date), calendarQueryDate(CalendarMode.DAY, date.plusDays(1)))
    }
}
