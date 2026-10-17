package com.pandora6ix.app.data

/**
 * 登录与会话仓库：
 * - POST /auth/login 保存 accessToken
 * - GET /me 读取真实资料与 capabilities
 * - POST /me/password 成功 204 后当前会话失效，需重新登录
 * - POST /auth/logout 204 退出
 */
class AuthRepository(private val api: ApiClient, private val session: SessionStore) {

    suspend fun login(enterpriseDomain: String, loginName: String, password: String): UserDto {
        val body = api.json.encodeToString(
            LoginRequest.serializer(),
            LoginRequest(enterpriseDomain.trim(), loginName.trim(), password)
        )
        val result = api.post("/auth/login", body, LoginResult.serializer())
        session.save(result.accessToken, result.user.id)
        return result.user
    }

    suspend fun me(): UserDto = api.get("/me", UserDto.serializer()).also { session.bindOwner(it.id) }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val body = api.json.encodeToString(
            ChangePasswordRequest.serializer(),
            ChangePasswordRequest(currentPassword, newPassword)
        )
        api.postEmpty("/me/password", body)
        // 服务端已使当前会话失效：清本地会话，界面引导重新登录
        session.clear()
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
