package com.pandora6ix.app.data

import kotlinx.serialization.Serializable

/** POST /auth/login 请求体 */
@Serializable
data class LoginRequest(
    val enterpriseDomain: String,
    val loginName: String,
    val password: String
)

/** POST /auth/login 成功 data */
@Serializable
data class LoginResult(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresAt: String? = null,
    val user: UserDto
)

/** GET /me / 登录返回的用户资料 */
@Serializable
data class UserDto(
    val id: String,
    val version: Long = 0,
    val displayName: String = "",
    val name: String = "",
    val loginName: String = "",
    val jobTitle: String? = null,
    val status: String = "",
    val roles: List<String> = emptyList(),
    val businessTimezone: String? = null,
    val department: String? = null,
    val supervisorUserId: String? = null,
    /** 直属上级展示名（/me 附带）；未配置或上级不存在时为 null */
    val supervisorName: String? = null,
    val orgUnitId: String? = null,
    val organization: OrgUnitDto? = null,
    val enterprise: EnterpriseDto? = null,
    val capabilities: CapabilitiesDto = CapabilitiesDto()
)

@Serializable
data class OrgUnitDto(
    val id: String,
    val version: Long = 0,
    val name: String = "",
    val kind: String = "",
    val parentId: String? = null
)

@Serializable
data class EnterpriseDto(
    val id: String,
    val version: Long = 0,
    val name: String = "",
    val loginDomain: String = "",
    val businessTimezone: String? = null,
    val status: String = "",
    val rootOrgUnitId: String? = null,
    val adminUserId: String? = null
)

/** /me.capabilities：角色层面资格，替换 DemoRole 一刀切；不代表绕过服务端校验 */
@Serializable
data class CapabilitiesDto(
    val admin: Boolean = false,
    val business: Boolean = false,
    val manage: Boolean = false,
    val createPersonalTasks: Boolean = false,
    val createDelegatedTasks: Boolean = false,
    val reviewTasks: Boolean = false,
    val createTags: Boolean = false,
    val publishCompanyItems: Boolean = false
)

/** POST /me/password 请求体：newPassword 至少 10 字符、最多 72 个 UTF-8 字节 */
@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)
