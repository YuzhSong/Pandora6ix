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

    /** 登录要素（企业域+登录名）：登录时保存，供改密后用新密码静默重登（profile 接口不下发 loginName） */
    fun saveCredentials(loginDomain: String, loginName: String) = prefs.edit()
        .putString("login_domain", loginDomain).putString("login_name", loginName).apply()

    val savedLoginDomain: String?
        get() = prefs.getString("login_domain", null)

    val savedLoginName: String?
        get() = prefs.getString("login_name", null)

    fun bindOwner(ownerId: String) = prefs.edit().putString("owner_id", ownerId).apply()

    fun clear() = prefs.edit().clear().apply()

    private companion object {
        const val KEY_TOKEN = "access_token"
    }
}
