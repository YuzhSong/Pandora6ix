package com.pandora6ix.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/** 每日事项：个人 to-do，date 为 ISO yyyy-MM-dd；与任务体系无关联 */
@Serializable
data class DailyItem(
    val id: String,
    val date: String,
    val content: String,
    val done: Boolean = false
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
