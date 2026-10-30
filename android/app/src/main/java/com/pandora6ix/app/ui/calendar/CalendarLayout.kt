package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.TaskItem
import com.pandora6ix.app.mock.*
import java.util.Calendar

enum class CalendarMode(val label: String) { DAY("日视图"), WEEK("周视图"), MONTH("月视图"), YEAR("年视图") }
data class TaskSegment(val task: TaskItem, val first: Int, val last: Int)

fun CalendarMode.zoomIn(): CalendarMode = when (this) {
    CalendarMode.YEAR -> CalendarMode.MONTH
    CalendarMode.MONTH -> CalendarMode.WEEK
    CalendarMode.WEEK -> CalendarMode.DAY
    CalendarMode.DAY -> CalendarMode.DAY
}

fun CalendarMode.zoomOut(): CalendarMode = when (this) {
    CalendarMode.DAY -> CalendarMode.WEEK
    CalendarMode.WEEK -> CalendarMode.MONTH
    CalendarMode.MONTH -> CalendarMode.YEAR
    CalendarMode.YEAR -> CalendarMode.YEAR
}

fun monthWeeks(date: DemoDate): List<List<DemoDate>> {
    val first = DemoDate(date.year, date.month, 1)
    val monday = mondayOfWeek(first)
    val last = first.plusMonths(1).plusDays(-1)
    val count = ((daysBetween(monday, last) + 7) / 7) * 7
    val calendar = Calendar.getInstance().apply {
        clear()
        set(monday.year, monday.month - 1, monday.day, 12, 0, 0)
    }
    return List(count) {
        val date = DemoDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        date
    }.chunked(7)
}

fun packTaskRows(tasks: List<TaskItem>, dates: List<DemoDate>): List<List<TaskSegment>> {
    val rows = mutableListOf<MutableList<TaskSegment>>()
    tasks.filter { it.end >= dates.first() && it.start <= dates.last() }
        .sortedWith(compareByDescending<TaskItem> { it.priorityLevel }.thenBy { it.start }.thenBy { it.id })
        .forEach { task ->
            val segment = TaskSegment(task, maxOf(0, daysBetween(dates.first(), task.start)), minOf(dates.lastIndex, daysBetween(dates.first(), task.end)))
            val row = rows.firstOrNull { lane -> lane.none { segment.first <= it.last && segment.last >= it.first } }
                ?: mutableListOf<TaskSegment>().also { rows.add(it) }
            row.add(segment)
        }
    return rows.map { it.sortedBy { segment -> segment.first } }
}

fun isAllDay(task: TaskItem) = task.startMinute == null || task.endMinute == null

fun weekAllDayRows(tasks: List<TaskItem>, dates: List<DemoDate>): List<List<TaskSegment>> {
    if (dates.isEmpty()) return emptyList()
    val rows = mutableListOf<MutableList<TaskSegment>>()
    tasks.filter { isAllDay(it) && it.start <= dates.last() && it.end >= dates.first() }
        .sortedWith(compareBy<TaskItem> { it.start }
            .thenByDescending { daysBetween(it.start, it.end) }.thenBy { it.id })
        .forEach { task ->
            val segment = TaskSegment(task, maxOf(0, daysBetween(dates.first(), task.start)),
                minOf(dates.lastIndex, daysBetween(dates.first(), task.end)))
            val row = rows.firstOrNull { lane -> lane.none { segment.first <= it.last && segment.last >= it.first } }
                ?: mutableListOf<TaskSegment>().also { rows.add(it) }
            row.add(segment)
        }
    return rows.map { it.toList() }
}

/** 周视图使用任务原始起点与总时长排序，不使用当天裁剪后的区间。 */
fun weekTasksOnDate(tasks: List<TaskItem>, date: DemoDate): List<TaskItem> =
    tasks.filter { timelineTimeRangeOnDate(it, date)?.isEmpty() == false }
        .sortedWith(compareBy<TaskItem> { it.start }
            .thenBy { if (isAllDay(it)) 0 else it.startMinute!! }
            .thenByDescending {
                if (isAllDay(it)) (daysBetween(it.start, it.end).toLong() + 1) * 1440
                else daysBetween(it.start, it.end).toLong() * 1440 + it.endMinute!! - it.startMinute!!
            }.thenBy { it.id })
// 日/周视图将全天任务映射为整日高度，不修改原任务时间数据。
fun timelineTimeRangeOnDate(task: TaskItem, date: DemoDate): IntRange? =
    if (isAllDay(task)) {
        if (date >= task.start && date <= task.end) 0 until 1440 else null
    } else timeRangeOnDate(task, date)
fun minuteLabel(value: Int): String = "%02d:%02d".format(value / 60, value % 60)
fun taskTimeLabel(task: TaskItem): String = if (isAllDay(task)) "全天" else "${minuteLabel(task.startMinute!!)}—${minuteLabel(task.endMinute!!)}"
fun timeRangeOnDate(task: TaskItem, date: DemoDate): IntRange? {
    if (isAllDay(task) || date < task.start || date > task.end) return null
    val from = if (date == task.start) task.startMinute!! else 0
    val until = if (date == task.end) task.endMinute!! else 1440
    return from until until
}

data class TimedTaskPlacement(val task: TaskItem, val range: IntRange, val depth: Int, val columnCount: Int = 1)

/** 日视图：每个相交区间组等宽分列，已结束的列可供后续任务复用。 */
fun parallelTimedTasks(tasks: List<TaskItem>, date: DemoDate): List<TimedTaskPlacement> {
    val entries = tasks.mapNotNull { task ->
        timelineTimeRangeOnDate(task, date)?.takeUnless { it.isEmpty() }?.let { TimedTaskPlacement(task, it, 0) }
    }.sortedWith(compareBy<TimedTaskPlacement> { it.range.first }
        .thenByDescending { it.range.last }.thenBy { it.task.id })
    val result = mutableListOf<TimedTaskPlacement>()
    val group = mutableListOf<TimedTaskPlacement>()
    val columnEnds = mutableListOf<Int>()
    var groupEnd = -1
    fun flushGroup() {
        result.addAll(group.map { it.copy(columnCount = columnEnds.size) })
        group.clear()
        columnEnds.clear()
    }
    entries.forEach { entry ->
        if (entry.range.first > groupEnd) flushGroup()
        var column = columnEnds.indexOfFirst { it < entry.range.first }
        if (column < 0) {
            column = columnEnds.size
            columnEnds.add(entry.range.last)
        } else columnEnds[column] = entry.range.last
        group.add(entry.copy(depth = column))
        groupEnd = maxOf(groupEnd, entry.range.last)
    }
    flushGroup()
    return result
}

fun timelineTasksAt(tasks: List<TaskItem>, date: DemoDate, minute: Int): List<TaskItem> =
    if (minute !in 0 until 1440) emptyList() else staggerTimedTasks(tasks, date)
        .filter { minute in it.range }.map { it.task }.distinctBy { it.id }

fun staggerTimedTasks(tasks: List<TaskItem>, date: DemoDate): List<TimedTaskPlacement> {
    val entries = tasks.mapNotNull { task ->
        timelineTimeRangeOnDate(task, date)?.takeUnless { it.isEmpty() }?.let { TimedTaskPlacement(task, it, 0) }
    }.sortedWith(compareBy<TimedTaskPlacement> { it.range.first }
        .thenByDescending { it.range.last }.thenBy { it.task.id })
    val active = mutableListOf<TimedTaskPlacement>()
    return entries.map { entry ->
        active.removeAll { it.range.last < entry.range.first }
        entry.copy(depth = (active.maxOfOrNull { it.depth } ?: -1) + 1).also { active.add(it) }
    }
}

fun validTaskInterval(start: DemoDate, end: DemoDate, startMinute: Int?, endMinute: Int?): Boolean {
    if (end < start || (startMinute == null) != (endMinute == null)) return false
    if (startMinute == null) return true
    if (startMinute !in 0..1439 || endMinute!! !in 0..1439) return false
    return end > start || endMinute > startMinute
}
