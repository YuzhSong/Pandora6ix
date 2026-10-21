package com.pandora6ix.app.data

import android.content.Context
import android.content.SharedPreferences

/**
 * 会话存储：accessToken 只保存在应用私有目录，不写入日志、不进版本库。
 * 切换账号前必须先 clear，保证账号隔离。
 */
class SessionStore internal constructor(private val prefs: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences("pandora_session", Context.MODE_PRIVATE))

    @Volatile private var renewingToken: String? = null

    @Synchronized fun beginPasswordRenewal(token: String) { renewingToken = token }
    fun isPasswordRenewing(token: String): Boolean = renewingToken == token
    @Synchronized fun endPasswordRenewal(token: String) { if (renewingToken == token) renewingToken = null }

    @Synchronized fun expireIfCurrent(token: String, ignoreRenewal: Boolean = false): Boolean {
        if (this.token != token || (!ignoreRenewal && renewingToken == token)) return false
        clear()
        return true
    }

    @Synchronized fun saveLoginIfCurrent(expected: String?, token: String, ownerId: String, domain: String, name: String): Boolean {
        if (this.token != expected) return false
        prefs.edit().putString(KEY_TOKEN, token).putString("owner_id", ownerId)
            .putString("login_domain", domain).putString("login_name", name).apply()
        return true
    }

    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)

    val ownerId: String?
        get() = prefs.getString("owner_id", null)

    @Synchronized fun save(token: String, ownerId: String) = prefs.edit()
        .putString(KEY_TOKEN, token).putString("owner_id", ownerId).apply()

    /** 登录要素（企业域+登录名）：登录时保存，供改密后用新密码静默重登（profile 接口不下发 loginName） */
    fun saveCredentials(loginDomain: String, loginName: String) = prefs.edit()
        .putString("login_domain", loginDomain).putString("login_name", loginName).apply()

    val savedLoginDomain: String?
        get() = prefs.getString("login_domain", null)

    val savedLoginName: String?
        get() = prefs.getString("login_name", null)

    fun bindOwner(ownerId: String) = prefs.edit().putString("owner_id", ownerId).apply()

    @Synchronized fun clear() { renewingToken = null; prefs.edit().clear().apply() }

    private companion object {
        const val KEY_TOKEN = "access_token"
    }
}
