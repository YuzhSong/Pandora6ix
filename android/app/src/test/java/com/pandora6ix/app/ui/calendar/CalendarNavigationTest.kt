package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.mock.DemoDate
import com.pandora6ix.app.mock.mondayOfWeek
import org.junit.Assert.*
import org.junit.Test

class CalendarNavigationTest {
    @Test fun distantMonthTargetsAndNeighborsHaveRealContent() {
        for (year in listOf(1900, 1990, 2000, 2030, 2090, 2100)) {
            val index = buildMonthScrollIndex(year - MonthScrollSpanYears, year + MonthScrollSpanYears)
            for (month in listOf(1, 2, 6, 12)) {
                val selected = DemoDate(year, month, 15)
                for (target in listOf(selected.plusMonths(-1), selected, selected.plusMonths(1))) {
                    val position = index.monthItemIndex.getValue(monthKey(target))
                    assertEquals(DemoDate(target.year, target.month, 1), index.items[position].month)
                    assertTrue(index.items[position].isTitle)
                    val days = index.items.drop(position + 1).takeWhile { !it.isTitle }.flatMap { it.week }
                    assertTrue("Missing selected day $target", target in days)
                    assertTrue(days.all { kotlin.math.abs(it.year - year) <= 1 })
                }
            }
        }
    }

    @Test fun distantDayAndWeekSwipesStayRelativeToSelection() {
        for (year in listOf(1900, 1990, 2030, 2090, 2100)) {
            val lastDay = DemoDate(year, 12, 31)
            assertEquals(DemoDate(year + 1, 1, 1), advanceCalendarDate(CalendarMode.DAY, lastDay, true))
            assertEquals(DemoDate(year, 12, 30), advanceCalendarDate(CalendarMode.DAY, lastDay, false))
            val nextWeek = advanceCalendarDate(CalendarMode.WEEK, lastDay, true)
            assertEquals(DemoDate(year + 1, 1, 7), nextWeek)
            assertEquals(lastDay, advanceCalendarDate(CalendarMode.WEEK, nextWeek, false))
            assertEquals(mondayOfWeek(lastDay).plusDays(7), mondayOfWeek(nextWeek))
        }
    }

    @Test fun monthNavigationPreservesLeapYearAndClampsDays() {
        assertEquals(DemoDate(2000, 2, 29), advanceCalendarDate(CalendarMode.MONTH, DemoDate(2000, 1, 31), true))
        assertEquals(DemoDate(1900, 2, 28), advanceCalendarDate(CalendarMode.MONTH, DemoDate(1900, 3, 31), false))
        assertEquals(DemoDate(2100, 2, 28), advanceCalendarDate(CalendarMode.MONTH, DemoDate(2100, 1, 31), true))
    }

    @Test fun headingsExposeSelectedYearIncludingCrossYearWeeks() {
        assertTrue(calendarPeriodTitle(CalendarMode.DAY, DemoDate(2030, 6, 15)).startsWith("2030年6月15日"))
        assertEquals("1990年6月", calendarPeriodTitle(CalendarMode.MONTH, DemoDate(1990, 6, 15)))
        assertEquals("2029年12月31日—2030年1月6日", calendarPeriodTitle(CalendarMode.WEEK, DemoDate(2030, 1, 1)))
    }
}
