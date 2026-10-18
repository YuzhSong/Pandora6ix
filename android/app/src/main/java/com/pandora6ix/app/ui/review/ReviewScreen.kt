package com.pandora6ix.app.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.data.ApiException
import com.pandora6ix.app.data.SessionExpiredException
import com.pandora6ix.app.data.businessDateTimeLabel
import com.pandora6ix.app.data.TagDto
import com.pandora6ix.app.data.TaskRepository
import com.pandora6ix.app.data.TaskReviewDto
import com.pandora6ix.app.ui.calendar.tagHexColor
import com.pandora6ix.app.ui.theme.CreamDeep
import com.pandora6ix.app.ui.theme.Ink
import com.pandora6ix.app.ui.theme.Mint
import com.pandora6ix.app.ui.theme.MintDeep
import com.pandora6ix.app.ui.theme.Peach
import com.pandora6ix.app.ui.theme.WarmCard
import com.pandora6ix.app.ui.theme.WarmOrange
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * 审批中心：
 * - 双队列 GET /task-reviews?scope=pending（待我审批）| created（我提交的申请）
 * - 详情展示 submittedSnapshot（提交时不可变内容），非当前任务正文
 * - 决定 POST /task-reviews/{id}/decision：APPROVE|REJECT + review.version（乐观锁）；
 *   409 时重读最新状态并换新 Idempotency-Key；成功批准后任务进入日历
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(taskRepo: TaskRepository, availableTags: List<TagDto>, businessTimezone: String? = null, onBack: () -> Unit) {
    var scope by rememberSaveable { mutableStateOf("pending") }
    var items by remember { mutableStateOf<List<TaskReviewDto>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadRetry by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<TaskReviewDto?>(null) }
    val uiScope = rememberCoroutineScope()

    fun load(append: Boolean, cursor: String? = null) {
        loading = true; loadError = null
        uiScope.launch {
            try {
                val page = taskRepo.reviews(scope, cursor = cursor)
                items = if (append) items + page.items else page.items
                nextCursor = page.nextCursor
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: Exception) {
                loadError = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
                if (!append) items = emptyList()
            } finally {
                loading = false
            }
        }
    }
    LaunchedEffect(scope, loadRetry) { load(append = false) }

    selected?.let { review ->
        ReviewDetail(review, taskRepo, availableTags, businessTimezone, onBack = { selected = null }, onChanged = { load(append = false) })
        return
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Column { Text("审批中心", fontWeight = FontWeight.Bold); Text("任务申请与审批结果", fontSize = 12.sp, color = Ink.copy(alpha = .6f)) } },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QueueTab("待我审批", scope == "pending", Modifier.weight(1f)) { scope = "pending" }
            QueueTab("我提交的", scope == "created", Modifier.weight(1f)) { scope = "created" }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                loading && items.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(36.dp), color = WarmOrange)
                loadError != null -> Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError!!, color = Ink.copy(alpha = .7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton({ loadRetry++ }) { Text("重试") }
                }
                items.isEmpty() -> Text(
                    if (scope == "pending") "暂无待审批的申请" else "暂无提交的申请",
                    Modifier.align(Alignment.Center), color = Ink.copy(alpha = .5f)
                )
                else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { review ->
                        ReviewCard(review, businessTimezone) { selected = review }
                    }
                    nextCursor?.let { cursor ->
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = WarmOrange)
                                else OutlinedButton({ load(append = true, cursor = cursor) }) { Text("加载更多") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) WarmOrange else WarmCard)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, fontSize = 14.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Ink else Ink.copy(alpha = .65f)) }
}

@Composable
private fun ReviewCard(review: TaskReviewDto, businessTimezone: String?, onOpen: () -> Unit) {
    val snapshot = parseSnapshot(review.submittedSnapshot)
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(snapshot.title.ifBlank { "未命名任务" }, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                DecisionBadge(review.decision)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(kindLabel(snapshot.kind), fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                Text("第 ${review.attemptNo} 次提交", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                review.submittedAt?.let { Text(timeLabel(it, businessTimezone), fontSize = 12.sp, color = Ink.copy(alpha = .6f)) }
            }
            if (review.decision != "PENDING" && review.decisionNote?.isNotBlank() == true) {
                Text("批注：${review.decisionNote}", fontSize = 12.sp, color = Ink.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 详情：快照字段 + 审批操作。409 后重读最新 review；成功后回调刷新队列 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewDetail(review: TaskReviewDto, taskRepo: TaskRepository, availableTags: List<TagDto>, businessTimezone: String?, onBack: () -> Unit, onChanged: () -> Unit) {
    var current by remember { mutableStateOf(review) }
    var comment by rememberSaveable { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var idempotencyKey by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var confirmAction by remember { mutableStateOf<String?>(null) }
    var doneMessage by remember { mutableStateOf<String?>(null) }
    val uiScope = rememberCoroutineScope()
    val snapshot = parseSnapshot(current.submittedSnapshot)
    val canDecide = current.decision == "PENDING" && current.allowedActions.isNotEmpty()
    val stale = current.taskVersion in 1L until current.currentTaskVersion

    fun refresh() {
        uiScope.launch {
            try {
                current = taskRepo.review(current.id)
            } catch (_: SessionExpiredException) {
            } catch (_: Exception) { }
        }
    }
    fun submit(decision: String) {
        submitting = true; error = null
        uiScope.launch {
            try {
                taskRepo.decide(current.id, decision, current.version, comment.trim().ifBlank { null }, idempotencyKey)
                doneMessage = if (decision == "APPROVE") "已批准，任务已生效并出现在相关日历" else "已驳回，创建者可修改后重新提交"
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: ApiException) {
                error = e.friendlyMessage()
                if (e.httpStatus == 409) {
                    // 版本/状态冲突：重读最新状态并换新幂等键（原请求未生效）
                    idempotencyKey = UUID.randomUUID().toString()
                    refresh()
                }
            } catch (e: Exception) {
                error = "操作失败，请稍后重试"
            } finally {
                submitting = false
                confirmAction = null
            }
        }
    }

    androidx.activity.compose.BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Column { Text("审批详情", fontWeight = FontWeight.Bold); Text("第 ${current.attemptNo} 次提交 · ${timeLabel(current.submittedAt, businessTimezone)}", fontSize = 12.sp, color = Ink.copy(alpha = .6f)) } },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        LazyColumn(Modifier.fillMaxSize().weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(snapshot.title.ifBlank { "未命名任务" }, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            DecisionBadge(current.decision)
                        }
                        InfoRow("类型", kindLabel(snapshot.kind))
                        snapshot.description?.takeIf { it.isNotBlank() }?.let { InfoRow("说明", it) }
                        InfoRow("重要程度", "${snapshot.priorityLevel ?: 3} 星")
                        InfoRow("日期", dateRangeLabel(snapshot.startDate, snapshot.endDate))
                        if (snapshot.timeKind == "TIMED") InfoRow("时间", "${shortTime(snapshot.startTime)} – ${shortTime(snapshot.endTime)}")
                        InfoRow("执行人", recipientLabel(snapshot))
                        val tagNames = snapshot.tagIds.mapNotNull { id -> availableTags.firstOrNull { it.id == id } }
                        if (tagNames.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("标签", fontSize = 13.sp, color = Ink.copy(alpha = .6f))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    tagNames.forEach { tag ->
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Box(Modifier.size(10.dp).clip(CircleShape).background(tagHexColor(tag.colorHex)))
                                            Text("${tag.name}${if (tag.id == snapshot.primaryTagId) " · 主" else ""}", fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (stale) item {
                Text("任务在提交后已有修改，以下为提交当时的内容快照。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
            }
            if (current.decision == "PENDING" && current.allowedActions.isEmpty()) item {
                Text("该申请当前不可操作（权限或人员范围已变化），请由创建者修改后重新提交。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
            }
            if (current.decision != "PENDING") item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (current.decision == "APPROVED") Mint else Peach), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (current.decision == "APPROVED") "已批准" else "已驳回", fontWeight = FontWeight.Bold)
                        current.decisionNote?.takeIf { it.isNotBlank() }?.let { Text("批注：$it", fontSize = 13.sp, color = Ink.copy(alpha = .75f)) }
                        current.decidedAt?.let { Text("决定时间 ${timeLabel(it, businessTimezone)}", fontSize = 12.sp, color = Ink.copy(alpha = .6f)) }
                    }
                }
            }
            if (canDecide) item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("批注（驳回时建议填写）") },
                        minLines = 2,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .7f), unfocusedBorderColor = Ink.copy(alpha = .3f), focusedLabelColor = Ink, cursorColor = Ink)
                    )
                    if (error != null) Text(error!!, color = com.pandora6ix.app.ui.theme.CoralDark, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            { confirmAction = "APPROVE" },
                            Modifier.weight(1f),
                            enabled = !submitting,
                            colors = ButtonDefaults.buttonColors(containerColor = MintDeep, contentColor = Color.White)
                        ) { Text("批准") }
                        OutlinedButton(
                            { confirmAction = "REJECT" },
                            Modifier.weight(1f),
                            enabled = !submitting,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = com.pandora6ix.app.ui.theme.CoralDark)
                        ) { Text("驳回") }
                    }
                }
            }
        }
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { if (!submitting) confirmAction = null },
            title = { Text(if (action == "APPROVE") "批准该申请？" else "驳回该申请？") },
            text = { Text(if (action == "APPROVE") "批准后任务立即生效，接收人可见。" else "驳回后创建者可修改内容重新提交。") },
            confirmButton = {
                if (submitting) CircularProgressIndicator(Modifier.size(22.dp), color = WarmOrange)
                else TextButton({ submit(action) }) { Text(if (action == "APPROVE") "批准" else "驳回", color = if (action == "APPROVE") MintDeep else com.pandora6ix.app.ui.theme.CoralDark) }
            },
            dismissButton = { if (!submitting) TextButton({ confirmAction = null }) { Text("取消") } }
        )
    }
    doneMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("操作完成") },
            text = { Text(message) },
            confirmButton = { TextButton({ doneMessage = null; onChanged(); onBack() }) { Text("好的") } }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.padding(top = 1.dp), fontSize = 13.sp, color = Ink.copy(alpha = .6f))
        Text(value, Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun DecisionBadge(decision: String) {
    val (bg, fg) = when (decision) {
        "APPROVED" -> Mint to MintDeep
        "REJECTED" -> Peach to com.pandora6ix.app.ui.theme.CoralDark
        else -> CreamDeep to Ink
    }
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(when (decision) { "APPROVED" -> "已批准"; "REJECTED" -> "已驳回"; else -> "待审批" }, fontSize = 12.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

/** submittedSnapshot 宽松解析：新快照字段与 TaskCreateDto 对应，旧快照可能缺字段或含历史字段 */
private fun parseSnapshot(snapshot: JsonObject?): SnapshotInfo {
    val obj = snapshot ?: return SnapshotInfo(null, "", null, null, null, null, null, null, null, emptyList(), emptyList(), null)
    fun str(key: String): String? = runCatching { obj[key]?.jsonPrimitive?.content }.getOrNull()
    fun int(key: String): Int? = runCatching { obj[key]?.jsonPrimitive?.intOrNull }.getOrNull()
    fun strings(key: String): List<String> = runCatching { obj[key]?.jsonArray?.map { it.jsonPrimitive.content } }.getOrNull().orEmpty()
    return SnapshotInfo(
        kind = str("kind"),
        title = str("title").orEmpty(),
        description = str("description"),
        priorityLevel = int("priorityLevel"),
        timeKind = str("timeKind"),
        startDate = str("startDate"),
        endDate = str("endDate"),
        startTime = str("startTime"),
        endTime = str("endTime"),
        recipientIds = strings("recipientIds"),
        tagIds = strings("tagIds"),
        primaryTagId = str("primaryTagId")
    )
}

private data class SnapshotInfo(
    val kind: String?,
    val title: String,
    val description: String?,
    val priorityLevel: Int?,
    val timeKind: String?,
    val startDate: String?,
    val endDate: String?,
    val startTime: String?,
    val endTime: String?,
    val recipientIds: List<String>,
    val tagIds: List<String>,
    val primaryTagId: String?
)

private fun kindLabel(kind: String?) = if (kind == "DELEGATED") "派发任务" else "个人任务"

private fun recipientLabel(snapshot: SnapshotInfo): String =
    if (snapshot.kind == "DELEGATED" && snapshot.recipientIds.isNotEmpty()) "接收人 ${snapshot.recipientIds.size} 人"
    else "本人执行"

private fun dateRangeLabel(start: String?, end: String?): String {
    val s = start?.takeIf { it.isNotBlank() } ?: return "未设置"
    val e = end?.takeIf { it.isNotBlank() } ?: s
    return if (s == e) s.replace("-", "/") else "${s.replace("-", "/")} – ${e.replace("-", "/")}"
}

private fun shortTime(time: String?): String = time?.take(5).orEmpty().ifBlank { "未设置" }

/** 提交时间按公司业务时区展示："MM-dd HH:mm"（后端返回 UTC 时间戳，直接切片会差一个时区） */
private fun timeLabel(iso: String?, timezone: String?): String {
    val label = businessDateTimeLabel(iso, timezone)
    return label.ifBlank { iso?.take(10).orEmpty() }
}
