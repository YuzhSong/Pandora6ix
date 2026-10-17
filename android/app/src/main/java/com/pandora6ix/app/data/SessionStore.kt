package com.pandora6ix.app.data

import android.content.Context

/**
 * 会话存储：accessToken 只保存在应用私有目录，不写入日志、不进版本库。
 * 切换账号前必须先 clear，保证账号隔离。
 */
class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("pandora_session", Context.MODE_PRIVATE)

    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)

    val ownerId: String?
        get() = prefs.getString("owner_id", null)

    fun save(token: String, ownerId: String) = prefs.edit()
        .putString(KEY_TOKEN, token).putString("owner_id", ownerId).apply()

    fun bindOwner(ownerId: String) = prefs.edit().putString("owner_id", ownerId).apply()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN = "access_token"
    }
}
