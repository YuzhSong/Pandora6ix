package com.pandora6ix.app.data

import com.pandora6ix.app.mock.DemoDate
import org.junit.Assert.*
import org.junit.Test

class RecurrenceRuleTest {
    @Test fun monthlySkipsMissingDayWithoutDrifting() {
        assertEquals(listOf(DemoDate(2028,1,31),DemoDate(2028,3,31)),
            recurrenceDates(DemoDate(2028,1,31),DemoDate(2028,4,30),RecurrenceRule("MONTHLY",1)))
    }
    @Test fun everyTwoWeeksUsesMondayAnchor() {
        assertEquals(listOf(DemoDate(2026,10,30),DemoDate(2026,11,9),DemoDate(2026,11,13)),
            recurrenceDates(DemoDate(2026,10,28),DemoDate(2026,11,13),RecurrenceRule("WEEKLY",2,listOf(1,5))))
    }
    @Test fun customDailyIntervalIncludesEnd() {
        assertEquals(listOf(1,4,7),recurrenceDates(DemoDate(2030,1,1),DemoDate(2030,1,7),RecurrenceRule("DAILY",3)).map { it.day })
    }
    @Test fun rejectsUnboundedGenerationAndEmptyWeekdays() {
        assertTrue(runCatching { recurrenceDates(DemoDate(2030,1,1),DemoDate(2031,1,3),RecurrenceRule("DAILY",1)) }.isFailure)
        assertTrue(runCatching { recurrenceDates(DemoDate(2030,1,1),DemoDate(2030,1,7),RecurrenceRule("WEEKLY",1)) }.isFailure)
    }
}
