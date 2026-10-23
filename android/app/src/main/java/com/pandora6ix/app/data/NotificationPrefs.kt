package com.pandora6ix.app.data

import android.content.Context

/** 提醒类型：与设置中「通知提醒」的三个子开关一一对应 */
object ReminderKind {
    const val NEW_TASK = "NEW_TASK"
    const val DEADLINE = "DEADLINE"
    const val STATUS = "STATUS"
}

/**
 * 通知提醒偏好（本地保存，按账号隔离）：主开关 + 三个子开关 + 截止提前量。
 * 同时保存任务状态快照（识别新增/变化）与已提醒记录（避免重复打扰）。
 */
class NotificationPrefs(context: Context, val ownerId: String?) {

    private val prefs = context.getSharedPreferences("notify_" + (ownerId ?: "anon"), Context.MODE_PRIVATE)

    /** 总控，关闭则全部静默；默认开启（用户仍可在设置中关闭；关闭时不申请通知权限） */
    var master: Boolean
        get() = prefs.getBoolean("master", true)
        set(value) = prefs.edit().putBoolean("master", value).apply()

    /** 新任务提醒：你被派发了新任务 / 个人任务批准 */
    var newTask: Boolean
        get() = prefs.getBoolean("new_task", true)
        set(value) = prefs.edit().putBoolean("new_task", value).apply()

    /** 截止时间临近：任务截止前 leadHours 小时 */
    var deadline: Boolean
        get() = prefs.getBoolean("deadline", true)
        set(value) = prefs.edit().putBoolean("deadline", value).apply()

    /** 任务状态变化：他人更新了你参与任务的进度 / 验收 */
    var statusChange: Boolean
        get() = prefs.getBoolean("status_change", true)
        set(value) = prefs.edit().putBoolean("status_change", value).apply()

    /** 截止提前量（分钟，可精确到分钟）；默认 24 小时 = 1440 分钟 */
    var leadMinutes: Int
        get() = prefs.getInt("lead_minutes", DEFAULT_LEAD_MINUTES)
        set(value) = prefs.edit().putInt("lead_minutes", value).apply()

    /** 主开关关闭时一切静默 */
    fun enabled(kind: String): Boolean = when (kind) {
        ReminderKind.NEW_TASK -> master && newTask
        ReminderKind.DEADLINE -> master && deadline
        ReminderKind.STATUS -> master && statusChange
        else -> false
    }

    /** 任务状态快照：key = 任务id|执行关系id，value = 执行状态|审批状态 */
    fun hasSnapshot(): Boolean = prefs.contains("snapshot")

    fun snapshot(): Map<String, String> {
        val raw = prefs.getString("snapshot", null) ?: return emptyMap()
        if (raw.isBlank()) return emptyMap()
        return raw.split(ENTRY_SEP).mapNotNull { part ->
            val i = part.indexOf(KV_SEP)
            if (i <= 0) null else part.substring(0, i) to part.substring(i + 1)
        }.toMap()
    }

    fun saveSnapshot(values: Map<String, String>) {
        val raw = values.entries.joinToString(ENTRY_SEP) { it.key + KV_SEP + it.value }
        prefs.edit().putString("snapshot", raw).apply()
    }

    /** 已提醒过的事件不再重复弹窗 */
    fun isNotified(key: String): Boolean = prefs.getStringSet(NOTIFIED, emptySet<String>())?.contains(key) == true

    fun markNotified(keys: Collection<String>) {
        val current = prefs.getStringSet(NOTIFIED, emptySet<String>())?.toMutableSet() ?: mutableSetOf()
        if (current.addAll(keys)) prefs.edit().putStringSet(NOTIFIED, current).apply()
    }

    companion object {
        /** 默认提前量：24 小时 */
        const val DEFAULT_LEAD_MINUTES = 24 * 60
        /** 自定义上限：72 小时 */
        const val MAX_LEAD_MINUTES = 72 * 60

        const val NOTIFIED = "notified"
        // 分隔符：任务 id 为 UUID、状态为大写枚举，均不含 ; 与 =，可安全分隔快照
        const val ENTRY_SEP = ";"
        const val KV_SEP = "="
    }
}
