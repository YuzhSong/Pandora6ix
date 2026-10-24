package com.pandora6ix.app.data

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/*
 * 服务端返回的时间戳统一是 UTC（BusinessDb.view() 用 toInstant().toString()，形如 2026-10-18T08:00:00Z）；
 * 展示或比较前必须先按公司业务时区换算，否则会把 UTC 当成现实时间显示（东八区正好差 8 小时）。
 * minSdk 24，不能使用 java.time，统一走 Calendar。
 */

/** 解析服务端 UTC 时间戳为 epoch millis；格式不符返回 null */
private fun serverEpochMillis(iso: String): Long? {
    // 形如 2026-10-18T08:00:00Z，秒后可能带 .1 / .123 / .123456（Instant.toString 的变体）
    if (iso.length < 19) return null
    return try {
        Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                iso.substring(0, 4).toInt(),
                iso.substring(5, 7).toInt() - 1,
                iso.substring(8, 10).toInt(),
                iso.substring(11, 13).toInt(),
                iso.substring(14, 16).toInt(),
                iso.substring(17, 19).toInt()
            )
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (e: NumberFormatException) {
        null
    }
}

private fun businessCalendar(iso: String?, timezone: String?): Calendar? {
    val raw = iso ?: return null
    val millis = serverEpochMillis(raw) ?: return null
    val zone = TimeZone.getTimeZone(timezone?.takeIf { it.isNotBlank() } ?: "UTC")
    return Calendar.getInstance(zone).apply { timeInMillis = millis }
}

/** UTC 时间戳 → 业务时区 "HH:mm"；无法解析返回空串 */
fun businessTimeLabel(iso: String?, timezone: String?): String {
    val cal = businessCalendar(iso, timezone) ?: return ""
    return String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

/** UTC 时间戳 → 业务时区 "MM-dd HH:mm"；无法解析返回空串 */
fun businessDateTimeLabel(iso: String?, timezone: String?): String {
    val cal = businessCalendar(iso, timezone) ?: return ""
    return String.format(
        Locale.US,
        "%02d-%02d %02d:%02d",
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE)
    )
}

private fun nowIn(timezone: String?): Calendar =
    Calendar.getInstance(TimeZone.getTimeZone(timezone?.takeIf { it.isNotBlank() } ?: "UTC"))

/** 业务时区当前日期 "YYYY-MM-DD" */
fun businessTodayKey(timezone: String?): String = nowIn(timezone).let {
    String.format(Locale.US, "%04d-%02d-%02d", it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
}

/** 业务时区当前分钟数 0–1439 */
fun businessNowMinute(timezone: String?): Int = nowIn(timezone).let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }

/** 业务时区当前时间 "HH:mm" */
fun businessNowLabel(timezone: String?): String = businessNowMinute(timezone).let { String.format(Locale.US, "%02d:%02d", it / 60, it % 60) }

/**
 * 截止时间是否仍在业务时区的当下之后：
 * 全天任务按当日 23:59 截止（当天仍可选），指定时间任务必须严格晚于当前时刻。
 */
fun deadlineNotPast(endKey: String, endMinute: Int?, allDay: Boolean, timezone: String?): Boolean {
    val today = businessTodayKey(timezone)
    if (allDay) return endKey >= today
    if (endKey > today) return true
    if (endKey < today) return false
    return (endMinute ?: 0) > businessNowMinute(timezone)
}

/** 业务时区内 "YYYY-MM-DD"(+ "HH:mm"，全天任务按当日 23:59) 对应的时间戳；非法输入返回 null */
fun businessDayMillis(date: String, time: String? = null, timezone: String? = null): Long? {
    val d = parseIsoDate(date) ?: return null
    val minute = parseHhMm(time) ?: (23 * 60 + 59)
    val zone = TimeZone.getTimeZone(timezone?.takeIf { it.isNotBlank() } ?: "UTC")
    return Calendar.getInstance(zone).apply {
        clear()
        set(d.year, d.month - 1, d.day, minute / 60, minute % 60, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
