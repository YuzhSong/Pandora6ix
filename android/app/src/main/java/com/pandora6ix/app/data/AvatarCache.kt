package com.pandora6ix.app.data

/** A cached absence expires too, so another device's upload can become visible. */
internal class AvatarCache<T>(private val nowMillis: () -> Long) {
    data class Entry<T>(val value: T?, val expiresAt: Long)
    private val entries = LinkedHashMap<String, Entry<T>>()

    fun get(userId: String): Entry<T>? {
        val entry = entries[userId] ?: return null
        if (nowMillis() < entry.expiresAt) return entry
        entries.remove(userId)
        return null
    }
    fun put(userId: String, value: T?) {
        entries.entries.removeAll { nowMillis() >= it.value.expiresAt }
        if (userId !in entries && entries.size >= 128) entries.remove(entries.keys.first())
        entries[userId] = Entry(value, nowMillis() + if (value == null) 10_000 else 30_000)
    }
}
