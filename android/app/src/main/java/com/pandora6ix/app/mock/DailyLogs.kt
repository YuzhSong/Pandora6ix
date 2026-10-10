package com.pandora6ix.app.mock

fun saveDailyLog(logs: MutableList<WorkLog>, content: String, date: DemoDate, time: String) {
    val index = logs.indexOfFirst { it.date == date && it.user == MockData.userName }
    if (index >= 0) {
        logs[index] = logs[index].copy(content = content, time = time)
    } else {
        logs.add(0, WorkLog("daily-${date.year}-${date.month}-${date.day}", content, MockData.userName, date, time))
    }
}
