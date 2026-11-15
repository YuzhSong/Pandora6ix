package com.pandora6ix.app.ui.logs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.data.ApiException
import com.pandora6ix.app.data.JournalCommentDto
import com.pandora6ix.app.data.JournalRepository
import com.pandora6ix.app.data.SessionExpiredException
import com.pandora6ix.app.data.UserRepository
import com.pandora6ix.app.data.businessDateTimeLabel
import com.pandora6ix.app.ui.components.PandoraButton
import com.pandora6ix.app.ui.components.PandoraButtonSize
import com.pandora6ix.app.ui.components.PandoraButtonVariant
import com.pandora6ix.app.ui.components.PandoraDivider
import com.pandora6ix.app.ui.components.PandoraPanel
import com.pandora6ix.app.ui.components.PandoraPanelTone
import com.pandora6ix.app.ui.components.PandoraTextField
import com.pandora6ix.app.ui.components.RemoteAvatar
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import kotlinx.coroutines.launch

const val JournalCommentMaxLength = 500

/**
 * 日志详情下方的“上级留言”区：能读日志即可查看；上级（canComment）有较大的多行输入框，
 * 作者只读。发送失败保留已输入的文字；留言发送后不可删除。
 */
@Composable
fun JournalCommentsSection(repo: JournalRepository, journalId: String, businessTimezone: String?, userRepo: UserRepository) {
    val c = PandoraTheme.colors
    val scope = rememberCoroutineScope()
    var comments by remember(journalId) { mutableStateOf<List<JournalCommentDto>>(emptyList()) }
    var canComment by remember(journalId) { mutableStateOf(false) }
    var loading by remember(journalId) { mutableStateOf(true) }
    var loadError by remember(journalId) { mutableStateOf<String?>(null) }
    var reload by remember(journalId) { mutableIntStateOf(0) }
    var draft by rememberSaveable(journalId) { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    var sendError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(journalId, reload) {
        loading = true; loadError = null
        try {
            val page = repo.comments(journalId)
            comments = page.items; canComment = page.canComment
        } catch (_: SessionExpiredException) {
        } catch (e: Exception) {
            loadError = (e as? ApiException)?.friendlyMessage() ?: "留言加载失败，请重试"
        } finally {
            loading = false
        }
    }

    fun send() {
        val text = draft.trim()
        if (text.isEmpty() || sending) return
        sending = true; sendError = null
        scope.launch {
            try {
                comments = comments + repo.addComment(journalId, text)
                draft = ""
            } catch (_: SessionExpiredException) {
            } catch (e: Exception) {
                sendError = (e as? ApiException)?.friendlyMessage() ?: "发送失败，请检查网络后重试"
            } finally {
                sending = false
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Text("上级留言", Modifier.semantics { heading() }, style = PandoraTypography.labelLarge, color = c.textSecondary)
        Spacer(Modifier.height(PandoraSpacing.space8))
        PandoraPanel(Modifier.fillMaxWidth(), tone = PandoraPanelTone.White) {
            when {
                loading -> Row(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space16), horizontalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(Modifier.padding(PandoraSpacing.space4), color = c.primary, strokeWidth = 2.dp)
                }
                loadError != null -> Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    Text(loadError!!, style = PandoraTypography.bodyMedium, color = c.error)
                    PandoraButton("重试", { reload++ }, variant = PandoraButtonVariant.Outline, size = PandoraButtonSize.Compact)
                }
                else -> Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                    if (comments.isEmpty()) Text(
                        if (canComment) "还没有留言，写下你对这篇日志的反馈吧。" else "暂无上级留言。",
                        style = PandoraTypography.bodyMedium, color = c.textTertiary
                    )
                    comments.forEachIndexed { index, comment ->
                        if (index > 0) PandoraDivider()
                        CommentRow(comment, businessTimezone, userRepo)
                    }
                    if (canComment) {
                        if (comments.isNotEmpty()) PandoraDivider()
                        PandoraTextField(
                            draft, { if (it.length <= JournalCommentMaxLength) draft = it }, Modifier.fillMaxWidth(),
                            enabled = !sending, minLines = 4, maxLines = 10,
                            placeholder = { Text("写下对这篇日志的意见或鼓励…") },
                            supportingText = { Text(sendError ?: "${draft.length}/$JournalCommentMaxLength", color = if (sendError != null) c.error else c.textSecondary) },
                            isError = sendError != null
                        )
                        PandoraButton("发送留言", { send() }, Modifier.fillMaxWidth(), enabled = draft.isNotBlank(), loading = sending, loadingText = "发送中…")
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: JournalCommentDto, businessTimezone: String?, userRepo: UserRepository) {
    val c = PandoraTheme.colors
    val name = comment.authorName?.takeIf { it.isNotBlank() } ?: "上级"
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
        RemoteAvatar(comment.authorUserId, name, PandoraDimensions.avatarMedium, 14.sp, userRepo)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
            Column {
                Text(name + (comment.authorJobTitle?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""), style = PandoraTypography.labelLarge, color = c.textPrimary)
                Text(businessDateTimeLabel(comment.createdAt, businessTimezone), style = PandoraTypography.caption, color = c.textTertiary)
            }
            Text(comment.content, style = PandoraTypography.bodyLarge, color = c.textPrimary)
        }
    }
}
