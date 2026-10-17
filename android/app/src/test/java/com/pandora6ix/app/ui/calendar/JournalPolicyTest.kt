package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.journalBusinessDate
import com.pandora6ix.app.mock.DemoDate
import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class JournalPolicyTest {
    @Test fun companyMidnightChangesBusinessDateRegardlessOfDeviceZone() {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear(); set(2026, Calendar.OCTOBER, 17, 15, 59, 59)
        }.timeInMillis
        val previous = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(DemoDate(2026, 10, 17), journalBusinessDate("Asia/Shanghai", utc))
            assertEquals(DemoDate(2026, 10, 18), journalBusinessDate("Asia/Shanghai", utc + 1000))
            assertEquals(DemoDate(2026, 10, 17), journalBusinessDate("UTC", utc + 1000))
        } finally { TimeZone.setDefault(previous) }
    }
}
