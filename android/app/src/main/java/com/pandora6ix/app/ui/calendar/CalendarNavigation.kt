package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.mock.DemoDate
import com.pandora6ix.app.mock.mondayOfWeek

/** Content and initial scroll position always describe the same year window. */
internal data class YearGridWindow(
    val anchorYear: Int,
    val initialMonth: Int,
    val scrollOffset: Int = 0
) {
    val startYear: Int get() = anchorYear - SpanYears
    val itemCount: Int get() = SpanYears * 2 * 12
    val initialIndex: Int get() = SpanYears * 12 + initialMonth - 1

    fun monthAt(index: Int): DemoDate = DemoDate(startYear + index / 12, index % 12 + 1, 1)

    fun recenteredAt(index: Int, offset: Int): YearGridWindow {
        val firstMonth = monthAt(index)
        return if (firstMonth.year <= startYear + 1 || firstMonth.year >= startYear + SpanYears * 2 - 2) {
            YearGridWindow(firstMonth.year, firstMonth.month, offset)
        } else this
    }

    companion object {
        private const val SpanYears = 12
    }
}

internal fun advanceCalendarDate(mode: CalendarMode, cursor: DemoDate, forward: Boolean): DemoDate {
    val step = if (forward) 1 else -1
    return when (mode) {
        CalendarMode.YEAR -> cursor.plusYears(step)
        CalendarMode.MONTH -> cursor.plusMonths(step)
        CalendarMode.WEEK -> cursor.plusDays(7 * step)
        CalendarMode.DAY -> cursor.plusDays(step)
    }
}

/** Changes to a cursor within one queried period do not invalidate its tasks. */
internal fun calendarQueryDate(mode: CalendarMode, cursor: DemoDate): DemoDate = when (mode) {
    CalendarMode.YEAR -> DemoDate(cursor.year, 1, 1)
    CalendarMode.MONTH -> cursor.copy(day = 1)
    CalendarMode.WEEK -> mondayOfWeek(cursor)
    CalendarMode.DAY -> cursor
}

internal fun calendarPeriodTitle(mode: CalendarMode, cursor: DemoDate): String = when (mode) {
    CalendarMode.YEAR -> "${cursor.year}年"
    CalendarMode.MONTH -> cursor.monthLabel()
    CalendarMode.DAY -> "${cursor.year}年${cursor.shortLabelWithWeekday()}"
    CalendarMode.WEEK -> {
        val start = mondayOfWeek(cursor)
        val end = start.plusDays(6)
        val endYear = if (start.year == end.year) "" else "${end.year}年"
        "${start.year}年${start.shortLabel()}—$endYear${end.shortLabel()}"
    }
}
