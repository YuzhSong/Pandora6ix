package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.mock.*

enum class CalendarMode(val label: String) { DAY("日视图"), WEEK("周视图"), MONTH("月视图") }
data class TaskSegment(val task: WorkTask, val first: Int, val last: Int)

fun monthWeeks(date: DemoDate): List<List<DemoDate>> {
    val first = DemoDate(date.year, date.month, 1)
    val monday = mondayOfWeek(first)
    val last = first.plusMonths(1).plusDays(-1)
    val count = ((daysBetween(monday, last) + 7) / 7) * 7
    return (0 until count).map { monday.plusDays(it) }.chunked(7)
}

fun packTaskRows(tasks: List<WorkTask>, dates: List<DemoDate>): List<List<TaskSegment>> {
    val rows = mutableListOf<MutableList<TaskSegment>>()
    tasks.filter { it.end >= dates.first() && it.start <= dates.last() }
        .sortedWith(compareByDescending<WorkTask> { it.priorityLevel }.thenBy { it.start }.thenBy { it.id })
        .forEach { task ->
            val segment = TaskSegment(task, maxOf(0, daysBetween(dates.first(), task.start)), minOf(dates.lastIndex, daysBetween(dates.first(), task.end)))
            val row = rows.firstOrNull { lane -> lane.none { segment.first <= it.last && segment.last >= it.first } }
                ?: mutableListOf<TaskSegment>().also { rows.add(it) }
            row.add(segment)
        }
    return rows.map { it.sortedBy { segment -> segment.first } }
}

fun isAllDay(task: WorkTask) = task.startMinute == null || task.endMinute == null
fun minuteLabel(value: Int): String = "%02d:%02d".format(value / 60, value % 60)
fun taskTimeLabel(task: WorkTask): String = if (isAllDay(task)) "全天" else "${minuteLabel(task.startMinute!!)}—${minuteLabel(task.endMinute!!)}"
fun timeRangeOnDate(task: WorkTask, date: DemoDate): IntRange? {
    if (isAllDay(task) || date < task.start || date > task.end) return null
    val from = if (date == task.start) task.startMinute!! else 0
    val until = if (date == task.end) task.endMinute!! else 1440
    return from until until
}

fun validTaskInterval(start: DemoDate, end: DemoDate, startMinute: Int?, endMinute: Int?): Boolean {
    if (end < start || (startMinute == null) != (endMinute == null)) return false
    if (startMinute == null) return true
    if (startMinute !in 0..1439 || endMinute!! !in 0..1439) return false
    return end > start || endMinute > startMinute
}
