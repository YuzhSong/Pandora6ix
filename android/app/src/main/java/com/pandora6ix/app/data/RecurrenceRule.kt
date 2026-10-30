package com.pandora6ix.app.data

import com.pandora6ix.app.mock.*
import kotlinx.serialization.Serializable
import java.util.Calendar
import java.util.TimeZone

@Serializable
data class RecurrenceRule(val frequency: String, val interval: Int, val weekdays: List<Int> = emptyList())

private fun dateOrdinal(date: DemoDate): Long = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
    clear()
    set(date.year, date.month - 1, date.day)
}.timeInMillis / 86_400_000L

fun recurrenceDates(start: DemoDate, end: DemoDate, rule: RecurrenceRule): List<DemoDate> {
    require(end >= start && end <= start.plusYears(10)) { "重复范围须在开始日期起十年以内" }
    require(rule.interval in 1..365 && rule.frequency in listOf("DAILY", "WEEKLY", "MONTHLY")) { "重复间隔须为 1–365" }
    require(rule.weekdays.all { it in 1..7 } && (if (rule.frequency == "WEEKLY") rule.weekdays.isNotEmpty() else rule.weekdays.isEmpty())) { "请选择重复的星期" }
    val monday = mondayOfWeek(start)
    val startOrdinal = dateOrdinal(start)
    val mondayOrdinal = dateOrdinal(monday)
    val dates = mutableListOf<DemoDate>()
    var d = start
    while (d <= end) {
        val matches = when (rule.frequency) {
            "DAILY" -> (dateOrdinal(d) - startOrdinal) % rule.interval == 0L
            "WEEKLY" -> (dateOrdinal(d) - mondayOrdinal) / 7 % rule.interval == 0L && ((dateOrdinal(d) - mondayOrdinal) % 7 + 1).toInt() in rule.weekdays
            else -> d.day == start.day && ((d.year - start.year) * 12 + d.month - start.month) % rule.interval == 0
        }
        if (matches) dates.add(d)
        require(dates.size <= 366) { "一次最多生成 366 次任务，请缩短范围" }
        d = d.plusDays(1)
    }
    require(dates.isNotEmpty()) { "所选范围没有符合规则的日期" }
    return dates
}

fun recurrenceRuleLabel(rule: RecurrenceRule, start: DemoDate): String {
    val unit = when (rule.frequency) { "DAILY" -> "天"; "WEEKLY" -> "周"; else -> "个月" }
    val detail = when (rule.frequency) {
        "WEEKLY" -> "，星期 " + rule.weekdays.sorted().joinToString("、") { listOf("一", "二", "三", "四", "五", "六", "日")[it - 1] }
        "MONTHLY" -> "，${start.day} 日（无该日则跳过）"
        else -> ""
    }
    return "每 ${rule.interval} $unit$detail"
}
