package com.pandora6ix.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import java.util.UUID

@Serializable
data class PushDeviceRegistrationRequest(
    val installationId: String,
    val registrationId: String
)

@Serializable
data class PushDeviceRegistrationResult(
    val installationId: String,
    val active: Boolean
)

@Serializable
data class NotificationDto(
    val id: String,
    val kind: String,
    val eventType: String = "",
    val taskId: String,
    val assignmentId: String? = null,
    val title: String,
    val body: String,
    val dueAt: String? = null,
    val createdAt: String,
    val readAt: String? = null,
    /** 审批申请是否已被处理：服务端按 task_review.decision 判定，已审批/驳回为 true */
    val resolved: Boolean = false
)

@Serializable
data class NotificationPageDto(
    val items: List<NotificationDto> = emptyList(),
    val nextCursor: String? = null
)

@Serializable
data class NotificationUnreadCountDto(val unreadCount: Long = 0)

class PushDeviceRepository(context: Context, private val api: ApiClient) {
    private val installationPrefs = context.getSharedPreferences("push_installation", Context.MODE_PRIVATE)
    val installationId: String = installationPrefs.getString("installation_id", null)
        ?: UUID.randomUUID().toString().also { installationPrefs.edit().putString("installation_id", it).apply() }

    suspend fun register(registrationId: String) {
        val body = api.json.encodeToString(
            PushDeviceRegistrationRequest.serializer(),
            PushDeviceRegistrationRequest(installationId, registrationId)
        )
        api.post("/push-devices", body, PushDeviceRegistrationResult.serializer())
    }

    suspend fun deactivate() {
        api.delete("/push-devices/$installationId", PushDeviceRegistrationResult.serializer())
    }
}

class NotificationRepository(private val api: ApiClient) {
    suspend fun list(): List<NotificationDto> = api.get(
        "/notifications?limit=100",
        NotificationPageDto.serializer()
    ).items

    suspend fun markRead(id: String) {
        api.post("/notifications/$id/read", null, NotificationDto.serializer())
    }

    /** 未读提醒条数：导图页信封角标使用（99+ 由客户端封顶） */
    suspend fun unreadCount(): Int = api.get(
        "/notifications/unread-count",
        NotificationUnreadCountDto.serializer()
    ).unreadCount.toInt()
}
