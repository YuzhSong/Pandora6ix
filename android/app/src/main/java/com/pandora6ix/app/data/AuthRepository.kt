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
        // 保存登录要素：改密码后需要用新密码静默重登（profile 接口不下发 loginName，无法事后取回）
        session.saveCredentials(enterpriseDomain.trim(), loginName.trim())
        return result.user
    }

    suspend fun me(): UserDto = api.get("/me", UserDto.serializer()).also { session.bindOwner(it.id) }

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        val body = api.json.encodeToString(
            ChangePasswordRequest.serializer(),
            ChangePasswordRequest(currentPassword, newPassword)
        )
        api.postEmpty("/me/password", body)
        // 成功 204：后端 password() 末尾 logout(actor) 已作废旧会话，调用方需用新密码重新 login()
    }

    /** 改密成功后用新密码静默重登，建立新会话并返回最新用户；失败（无登录要素/网络）返回 null，由调用方回退到重新登录 */
    suspend fun reloginWithNewPassword(newPassword: String): UserDto? {
        val domain = session.savedLoginDomain ?: return null
        val name = session.savedLoginName ?: return null
        return try {
            login(domain, name, newPassword)
        } catch (e: Exception) {
            null
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
