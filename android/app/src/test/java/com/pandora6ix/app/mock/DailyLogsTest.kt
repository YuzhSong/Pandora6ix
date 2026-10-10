package com.pandora6ix.app.mock

import org.junit.Assert.*
import org.junit.Test

class DailyLogsTest {
    @Test fun supplementUpdatesSeededReportWithoutDuplicatingIt() {
        val seeded = MockData.logs.first()
        val logs = MockData.logs.toMutableList()
        saveDailyLog(logs, "Updated report", seeded.date, "18:30")
        assertEquals(MockData.logs.size, logs.size)
        val report = logs.single { it.date == seeded.date && it.user == seeded.user }
        assertEquals(seeded.id, report.id)
        assertEquals("Updated report", report.content)
        assertEquals("18:30", report.time)
    }

    @Test fun repeatedPublicationKeepsOneReportPerDayAndPreservesHistory() {
        val logs = MockData.logs.toMutableList()
        val date = DemoDate(2027, 1, 1)
        saveDailyLog(logs, "First report", date, "10:00")
        saveDailyLog(logs, "Supplement", date, "11:00")
        assertEquals(MockData.logs.size + 1, logs.size)
        assertEquals("Supplement", logs.single { it.date == date }.content)
        assertTrue(logs.containsAll(MockData.logs))
    }
}
