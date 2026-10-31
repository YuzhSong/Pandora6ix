package com.pandora6ix.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import java.io.ByteArrayOutputStream

/** PUT /users/{id}/avatar 请求体：压缩后的图片 data URL */
@Serializable
data class UserAvatarBody(
    @SerialName("dataUrl") val dataUrl: String
)

@Serializable
data class MemberContactDto(val id: String, val phoneNumber: String? = null)

/**
 * 用户头像：服务端以 data URL 存取（客户端压缩后上传，服务端不做图像处理）。
 * - `avatar(userId)`：按人短期缓存，空头像也会过期；上传后立即更新缓存与订阅者
 * - `uploadAvatar`：把本地图片压缩成 256px JPEG 后上传，避免原图直传
 */
class UserRepository(private val api: ApiClient) {

    /** Member-detail contact only; the server checks the current management scope. */
    suspend fun memberContact(userId: String): MemberContactDto =
        api.get("/users/$userId/contact", MemberContactDto.serializer())

    private val cache = AvatarCache<Bitmap> { System.nanoTime() / 1_000_000 }
    private val revision = MutableStateFlow(0L)
    val avatarRevision = revision.asStateFlow()
    private val lock = Mutex()
    private val avatarLocks = HashMap<String, Mutex>()

    suspend fun avatar(userId: String): Bitmap? {
        if (userId.isBlank()) return null
        val avatarLock = lock.withLock { avatarLocks.getOrPut(userId) { Mutex() } }
        return avatarLock.withLock {
            lock.withLock { cache.get(userId)?.let { return it.value } }
            val bytes = api.getBytes("/users/$userId/avatar")
            val bitmap = bytes?.let { checkNotNull(BitmapFactory.decodeByteArray(it, 0, it.size)) }
            lock.withLock { cache.put(userId, bitmap) }
            bitmap
        }
    }

    /** 上传头像：压缩到 256px、JPEG 质量 82，再编码为 data URL */
    suspend fun uploadAvatar(userId: String, source: Bitmap) {
        val scaled = scaleTo(source, 256)
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        val dataUrl = "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        val avatarLock = lock.withLock { avatarLocks.getOrPut(userId) { Mutex() } }
        avatarLock.withLock {
            api.put("/users/$userId/avatar", api.json.encodeToString(UserAvatarBody(dataUrl)), Unit.serializer())
            lock.withLock {
                cache.put(userId, scaled)
                revision.value++
            }
        }
    }

    private fun scaleTo(source: Bitmap, target: Int): Bitmap {
        val max = maxOf(source.width, source.height)
        if (max <= target) return source
        val ratio = target.toFloat() / max
        return Bitmap.createScaledBitmap(
            source,
            (source.width * ratio).toInt().coerceAtLeast(1),
            (source.height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
