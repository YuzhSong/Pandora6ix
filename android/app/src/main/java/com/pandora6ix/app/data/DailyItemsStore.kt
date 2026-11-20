package com.pandora6ix.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

/** 每日事项：个人 to-do，date 为 ISO yyyy-MM-dd；与任务体系无关联 */
@Serializable
data class DailyItem(
    val id: String,
    val date: String,
    val content: String,
    val done: Boolean = false,
    val endDate: String = date,
    val startMinute: Int? = null,
    val endMinute: Int? = null
)

internal val DailyItemYears = 1990..2100

internal data class DailyItemSchedule(
    val start: LocalDate,
    val end: LocalDate,
    val startMinute: Int?,
    val endMinute: Int?
) {
    val isValid: Boolean
        get() = start.year in DailyItemYears && end.year in DailyItemYears &&
            validCalendarInterval(start, end, startMinute, endMinute)

    // A timed interval ending at midnight does not occupy any of the ending day.
    val lastCoveredDay: LocalDate get() = if (endMinute == 0) end.minusDays(1) else end

    operator fun contains(date: LocalDate): Boolean = date >= start && date <= lastCoveredDay

    fun moved(from: LocalDate, to: LocalDate): DailyItemSchedule? {
        if (from == to || from !in this || to.year !in DailyItemYears) return null
        val anchor = if (startMinute == null && endMinute == null) start else from
        val days = ChronoUnit.DAYS.between(anchor, to)
        if (days == 0L) return null
        return copy(start = start.plusDays(days), end = end.plusDays(days)).takeIf { it.isValid }
    }
}

internal val DailyItem.schedule: DailyItemSchedule?
    get() = try {
        DailyItemSchedule(LocalDate.parse(date), LocalDate.parse(endDate), startMinute, endMinute)
            .takeIf { it.isValid }
    } catch (_: DateTimeParseException) {
        null
    }

internal fun DailyItem.withSchedule(schedule: DailyItemSchedule): DailyItem = copy(
    date = schedule.start.toString(),
    endDate = schedule.end.toString(),
    startMinute = schedule.startMinute,
    endMinute = schedule.endMinute
)

/**
 * 每日事项本地存储（SharedPreferences，按账号隔离）。
 * 首页月历打点与当日清单共用同一份 JSON 列表；服务端接口接入后替换此实现即可。
 */
class DailyItemsStore(context: Context, ownerId: String?) {

    private val prefs = context.getSharedPreferences("daily_items_" + (ownerId ?: "anon"), Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<DailyItem> = runCatching {
        json.decodeFromString(ListSerializer(DailyItem.serializer()), prefs.getString(KEY, null) ?: "[]")
    }.getOrDefault(emptyList())

    fun save(items: List<DailyItem>) {
        prefs.edit().putString(KEY, json.encodeToString(ListSerializer(DailyItem.serializer()), items)).apply()
    }

    companion object {
        private const val KEY = "items"
    }
}
