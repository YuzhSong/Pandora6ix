package com.pandora6ix.app.data

import kotlinx.coroutines.CancellationException

/**
 * 登录与会话仓库：
 * - POST /auth/login 保存 accessToken
 * - GET /me 读取真实资料与 capabilities
 * - POST /me/password 成功 204 后当前会话失效，需重新登录
 * - POST /auth/logout 204 退出
 */
class AuthRepository(private val api: ApiClient, private val session: SessionStore) {

    suspend fun login(enterpriseDomain: String, loginName: String, password: String): UserDto {
        val expectedToken = session.token
        val body = api.json.encodeToString(
            LoginRequest.serializer(),
            LoginRequest(enterpriseDomain.trim(), loginName.trim(), password)
        )
        val result = api.post("/auth/login", body, LoginResult.serializer())
        // 保存登录要素：改密码后需要用新密码静默重登（profile 接口不下发 loginName，无法事后取回）
        if (!session.saveLoginIfCurrent(expectedToken, result.accessToken, result.user.id, enterpriseDomain.trim(), loginName.trim()))
            throw SessionExpiredException()
        return result.user
    }

    suspend fun me(): UserDto = api.get("/me", UserDto.serializer()).also { session.bindOwner(it.id) }

    suspend fun changePassword(currentPassword: String, newPassword: String): UserDto? {
        val token = session.token ?: throw SessionExpiredException()
        val domain = session.savedLoginDomain
        val name = session.savedLoginName
        val body = api.json.encodeToString(
            ChangePasswordRequest.serializer(),
            ChangePasswordRequest(currentPassword, newPassword)
        )
        var changed = false
        session.beginPasswordRenewal(token)
        try {
            api.postEmpty("/me/password", body)
            changed = true
            if (domain == null || name == null) return null
            return try {
                login(domain, name, newPassword)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
        } finally {
            // Never clear a replacement session belonging to another login.
            if (changed) session.expireIfCurrent(token, ignoreRenewal = true)
            session.endPasswordRenewal(token)
        }
    }

    suspend fun logout() {
        try {
            api.postEmpty("/auth/logout")
        } catch (_: SessionExpiredException) {
            // 会话本已过期：退出目的已达成
        } finally {
            session.clear()
        }
    }
}
