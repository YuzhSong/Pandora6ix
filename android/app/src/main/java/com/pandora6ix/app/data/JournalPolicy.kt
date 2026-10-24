package com.pandora6ix.app.data

import com.pandora6ix.app.mock.DemoDate
import java.util.Calendar
import java.util.TimeZone

/** UI date follows the authenticated company timezone; the server checks every write. */
fun journalBusinessDate(timezone: String?, nowMillis: Long = System.currentTimeMillis()): DemoDate {
    val calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone ?: "UTC"))
    calendar.timeInMillis = nowMillis
    return DemoDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
}
