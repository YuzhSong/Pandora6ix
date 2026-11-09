package com.pandora6ix.app.ui.summary

import com.pandora6ix.app.ui.components.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.data.*
import com.pandora6ix.app.ui.calendar.RecipientOrganizationGroup
import com.pandora6ix.app.ui.calendar.recipientOrganizationTree
import com.pandora6ix.app.ui.calendar.tagHexColor
import com.pandora6ix.app.ui.components.RemoteAvatar
import com.pandora6ix.app.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

/** 任务汇总与通讯录共用的面板、行与分页页脚 */
internal const val PreviewLimit = 3

@Composable
internal fun TaskPreview(
    pager: SummaryPager<TaskDto>, records: List<TaskDto>, name: (String) -> String, userRepo: UserRepository,
    onTask: (String) -> Unit, onAll: () -> Unit, onRetry: () -> Unit, filtered: Boolean = false
) {
    records.take(PreviewLimit).forEach { task -> SummaryTaskRow(task, name, userRepo) { onTask(task.id) } }
    CollectionFooter(pager, records.isEmpty(), "暂无相关任务", filtered, showMore = false,
        partialEmptyText = "已加载范围暂无相关任务，可查看全部任务", onMore = onRetry)
    TextButton(onAll, Modifier.fillMaxWidth()) {
        Text("查看全部任务", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary)
        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = PandoraTheme.colors.primary)
    }
}

@Composable
internal fun Modifier.summaryEntry(onClick: () -> Unit): Modifier {
    val shape = PandoraShapes.medium
    // 任务/日志独立条目：Surface 底 + 1dp 中性边框 + 12 角（规范 8 节 TaskItem/JournalItem）
    return clip(shape).background(PandoraTheme.colors.surface)
        .border(BorderStroke(PandoraDimensions.borderWidth, PandoraTheme.colors.border), shape).clickable(onClick = onClick)
}

@Composable
internal fun SummaryPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    // 用户确认的白色功能面板：panelSurface（与浮层同一白色值），0 阴影 + 1dp 边框
    PandoraPanel(modifier.fillMaxWidth(), tone = PandoraPanelTone.White) {
        Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12), content = content)
    }
}

@Composable
internal fun PanelHeading(title: String, onMore: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = PandoraTypography.titleSmall)
        if (onMore != null) TextButton(onMore, contentPadding = PaddingValues(start = PandoraSpacing.space12)) {
            Text("查看全部", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary, maxLines = 1)
            Icon(Icons.Default.ChevronRight, null, Modifier.size(PandoraDimensions.iconSmall + 2.dp), tint = PandoraTheme.colors.primary)
        }
    }
}

@Composable
internal fun Note(text: String) {
    Text(text, Modifier.padding(vertical = PandoraSpacing.space8), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
}

@Composable
internal fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space4), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
        Text(label, Modifier.widthIn(min = 42.dp), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
        Text(value, Modifier.weight(1f), style = PandoraTypography.bodyMedium)
    }
}

private sealed interface MemberPhoneState {
    data object Loading : MemberPhoneState
    data class Loaded(val phone: String?) : MemberPhoneState
    data class Failed(val text: String) : MemberPhoneState
}

@Composable
internal fun MemberPhone(memberId: String, userRepo: UserRepository, refresh: Int) {
    var attempt by remember(memberId) { mutableIntStateOf(0) }
    val state by produceState<MemberPhoneState>(MemberPhoneState.Loading, memberId, refresh, attempt) {
        value = MemberPhoneState.Loading
        value = try {
            val contact = userRepo.memberContact(memberId)
            check(contact.id == memberId)
            MemberPhoneState.Loaded(contact.phoneNumber)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            MemberPhoneState.Failed(when {
                error is ApiException && error.httpStatus == 404 -> "暂不可用"
                error is ApiException && error.httpStatus == 403 -> "无查看权限"
                else -> "加载失败"
            })
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space4), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12), verticalAlignment = Alignment.CenterVertically) {
        Text("电话", Modifier.widthIn(min = 42.dp), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
        Text(when (val current = state) {
            MemberPhoneState.Loading -> "加载中…"
            is MemberPhoneState.Loaded -> current.phone?.takeIf { it.isNotBlank() } ?: "未填写"
            is MemberPhoneState.Failed -> current.text
        }, Modifier.weight(1f), style = PandoraTypography.bodyMedium)
        if (state is MemberPhoneState.Failed) TextButton({ attempt++ }) { Text("重试", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
    }
}

@Composable
internal fun <T> CollectionFooter(
    pager: SummaryPager<T>, empty: Boolean, emptyText: String, filtered: Boolean = false,
    showMore: Boolean = true, partialEmptyText: String = "已加载范围暂无相关内容，可继续加载", onMore: () -> Unit
) {
    when {
        pager.loading || !pager.initialized -> Row(Modifier.padding(vertical = PandoraSpacing.space12), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
            CircularProgressIndicator(Modifier.size(18.dp), color = PandoraTheme.colors.primary, strokeWidth = 2.dp)
            Note("正在加载…")
        }
        pager.error != null -> Column {
            Text(pager.error!!, color = MaterialTheme.colorScheme.error, style = PandoraTypography.bodyMedium)
            TextButton(onMore) { Text("重试", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
        }
        else -> {
            if (empty) Note(if (filtered && pager.nextCursor != null) partialEmptyText else emptyText)
            if (showMore && pager.nextCursor != null) TextButton(onMore) { Text("加载更多", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SummaryTaskRow(task: TaskDto, name: (String) -> String, userRepo: UserRepository, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().summaryEntry(onOpen).padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
            Text(task.title, Modifier.weight(1f), style = PandoraTypography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val label = summaryTaskStatus(task)
            val tone = when (task.taskCompletionState()) {
                "COMPLETED" -> StatusTone.SUCCESS
                "AWAITING_ACCEPTANCE" -> StatusTone.WARNING
                else -> StatusTone.NEUTRAL
            }
            PandoraStatusChip(label, tone, maxWidth = 144.dp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space4), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
            if (task.reviewStatus != "APPROVED") PandoraStatusChip(reviewStatusLabel(task.reviewStatus), reviewStatusTone(task.reviewStatus))
            task.tags.sortedBy { if (it.id == task.primaryTagId) 0 else 1 }.forEach { tag ->
                PandoraTagChip(tag.name, tag.colorHex, primary = tag.id == task.primaryTagId)
            }
            PandoraStatusChip("★ ${priorityLabel(task.priorityLevel)}", priorityTone(task.priorityLevel))
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
            Icon(Icons.Default.Event, null, Modifier.size(PandoraDimensions.iconSmall), tint = PandoraTheme.colors.textSecondary)
            Text("截止 ${task.endDate}" + if (task.timeKind == "TIMED") " ${task.endTime.orEmpty()}" else "", Modifier.weight(1f), style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
            val executors = executorIds(task)
            LazyRow(Modifier.widthIn(max = 112.dp), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
                items(executors, key = { it }) { id -> RemoteAvatar(id, executorName(task, id, name), PandoraDimensions.avatarSmall, 10.sp, userRepo) }
            }
            if (executors.isEmpty()) Text("暂无可见执行人", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
        }
    }
}

@Composable
internal fun JournalRow(journal: JournalDto, name: String, timezone: String?, userRepo: UserRepository, onOpen: () -> Unit) {
    // 列表接口返回作者资料时优先使用（本地成员名单可能不包含停用/跨部门作者，退化成「成员 xxxx」）
    val shownName = journal.author?.displayName?.takeIf { it.isNotBlank() } ?: name
    Row(Modifier.fillMaxWidth().summaryEntry(onOpen).padding(PandoraSpacing.space12), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
        RemoteAvatar(journal.authorUserId, shownName, PandoraDimensions.avatarList, 16.sp, userRepo)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                Text(shownName, Modifier.weight(1f), style = PandoraTypography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(businessDateTimeLabel(journal.publishedAt, timezone).ifBlank { journal.workDate }, style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
            }
            Text(journalSummary(journal), maxLines = 1, overflow = TextOverflow.Ellipsis, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
        }
    }
}

@Composable
internal fun OrganizationContent(people: SummaryPager<RecipientDto>, canManage: Boolean, userRepo: UserRepository, onMember: (RecipientDto) -> Unit, onMore: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    val groups = remember(people.items) { recipientOrganizationTree(people.items) }
    SummaryPanel(Modifier.fillMaxSize().padding(PandoraSpacing.space16)) {
        if (!canManage) Note("当前账号没有组织架构查看权限")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
            fun androidx.compose.foundation.lazy.LazyListScope.group(node: RecipientOrganizationGroup, depth: Int) {
                item(key = node.key) {
                    Row(Modifier.fillMaxWidth().padding(start = (depth * 12).dp).summaryEntry {
                        expanded = if (node.key in expanded) expanded - node.key else expanded + node.key
                    }.padding(PandoraSpacing.space12), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (node.key in expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, if (node.key in expanded) "收起" else "展开")
                        Text(node.name, Modifier.weight(1f), style = PandoraTypography.labelLarge)
                    }
                }
                if (node.key in expanded) {
                    items(node.members, key = { "member:${it.id}" }) { person ->
                        Row(Modifier.fillMaxWidth().padding(start = (12 + depth * 12).dp).summaryEntry { onMember(person) }.padding(PandoraSpacing.space12),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                            RemoteAvatar(person.id, person.displayName, PandoraDimensions.avatarList, 16.sp, userRepo)
                            Column(Modifier.weight(1f)) {
                                Text(person.displayName.ifBlank { "成员 ${person.id.takeLast(4)}" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                person.jobTitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary) }
                            }
                            Icon(Icons.Default.ChevronRight, "成员详情", Modifier.size(18.dp))
                        }
                    }
                    node.children.forEach { group(it, depth + 1) }
                }
            }
            groups.forEach { group(it, 0) }
            item { CollectionFooter(people, people.items.isEmpty(), "暂无可查看的组织成员", onMore = onMore) }
        }
    }
}
