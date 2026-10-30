package com.pandora6ix.app.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

/** 任务草稿（未提交的添加任务内容，持久化到缓存目录） */
@Serializable
data class TaskDraftDto(
    val kind: String,
    val title: String,
    val note: String,
    val allDay: Boolean,
    val startKey: String,
    val endKey: String,
    val startMinute: Int,
    val endMinute: Int,
    val priority: Int,
    val tags: List<String>,
    val recipientIds: List<String>,
    val recurrence: RecurrenceRule? = null,
    val customRepeat: Boolean = false
)

/** 日报草稿（未发布的编辑器内容；entryId 非空表示修改既有条目） */
@Serializable
data class JournalDraftDto(
    val dateKey: String,
    val entries: List<JournalDraftEntryDto>,
    val tagIds: List<String> = emptyList()
)

@Serializable
data class JournalDraftEntryDto(val entryId: String?, val content: String)

/**
 * 应用缓存（微信/飞书语义）：
 * - 草稿缓存：cacheDir/drafts/（任务/日报未发布内容，随时可清理后重新填写）
 * - 图片缓存：filesDir/avatars/ 中已被替换的旧头像副本（当前使用中的头像不清除）
 * 不计入缓存：登录会话、用户设置、当前头像原图（用户数据）。
 */
object AppCache {
    val json = Json { ignoreUnknownKeys = true }

    private fun draftDir(context: Context): File {
        val owner = SessionStore(context).ownerId ?: error("A verified account is required")
        val scope = MessageDigest.getInstance("SHA-256").digest(owner.toByteArray())
            .joinToString("") { "%02x".format(it) }
        // Shared legacy drafts have no reliable owner and are never restored into an account.
        return File(context.cacheDir, "drafts/$scope").apply { mkdirs() }
    }

    fun writeDraft(context: Context, name: String, content: String) {
        runCatching { File(draftDir(context), name).writeText(content) }
    }

    fun readDraft(context: Context, name: String): String? =
        runCatching { File(draftDir(context), name).takeIf { it.exists() }?.readText() }.getOrNull()

    fun deleteDraft(context: Context, name: String) {
        runCatching { File(draftDir(context), name).delete() }
    }

    data class CacheItem(val label: String, val files: List<File>, val size: Long)

    /** 清点可清理缓存：仅列出非空分类 */
    fun collect(context: Context, currentAvatarPath: String?): List<CacheItem> {
        val draftFiles = runCatching { draftDir(context).listFiles()?.filter { it.isFile } ?: emptyList() }
            .getOrDefault(emptyList())
        val avatarFiles = (File(context.filesDir, "avatars").listFiles()?.filter { it.isFile } ?: emptyList())
            .filter { it.absolutePath != currentAvatarPath }
        return buildList {
            if (draftFiles.isNotEmpty()) add(CacheItem("草稿缓存（任务/日报未发布内容）", draftFiles, draftFiles.sumOf { it.length() }))
            if (avatarFiles.isNotEmpty()) add(CacheItem("图片缓存（被替换的旧头像副本）", avatarFiles, avatarFiles.sumOf { it.length() }))
        }
    }

    fun formatSize(bytes: Long): String = when {
        bytes >= 1L shl 20 -> "%.1f MB".format(bytes / 1048576.0)
        bytes >= 1L shl 10 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
}
