package com.pandora6ix.app.data

import kotlinx.serialization.Serializable
import java.net.URLEncoder

@Serializable
data class TagCreateDto(val name: String, val colorHex: String)

/** PATCH /tags/{id} 请求体：expectedVersion 乐观锁；name/colorHex 可选改 */
@Serializable
data class TagPatchDto(val expectedVersion: Long, val name: String? = null, val colorHex: String? = null)

@Serializable
data class TagDeletedDto(val tagId: String = "")

/**
 * 标签管理权限挂钩（暂不启用，接口先行）：
 * 未来按等级/角色填充——某些等级的管理者可创建/删除/更改标签内容及颜色；
 * 当前默认全部允许（UI 不门控）。服务端仍会校验并返回 403，由友好文案呈现。
 */
data class TagPermissions(val canCreate: Boolean = true, val canUpdate: Boolean = true, val canDelete: Boolean = true)

class TagRepository(private val api: ApiClient) {
    suspend fun tags(limit: Int = 100): List<TagDto> =
        api.get("/tags?limit=${limit.coerceIn(1, 100)}", TagPageDto.serializer()).items

    suspend fun create(name: String, colorHex: String): TagDto {
        val body = api.json.encodeToString(TagCreateDto.serializer(), TagCreateDto(name, colorHex))
        return api.post("/tags", body, TagDto.serializer())
    }

    suspend fun patch(id: String, expectedVersion: Long, name: String?, colorHex: String?): TagDto {
        val body = api.json.encodeToString(TagPatchDto.serializer(), TagPatchDto(expectedVersion, name, colorHex))
        return api.patch("/tags/${URLEncoder.encode(id, "UTF-8")}", body, TagDto.serializer())
    }

    suspend fun delete(id: String): TagDeletedDto =
        api.delete("/tags/${URLEncoder.encode(id, "UTF-8")}", TagDeletedDto.serializer())
}
