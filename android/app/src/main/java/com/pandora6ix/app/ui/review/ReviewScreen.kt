package com.pandora6ix.app.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.data.ApiException
import com.pandora6ix.app.data.SessionExpiredException
import com.pandora6ix.app.data.ReviewActorDto
import com.pandora6ix.app.data.UserRepository
import com.pandora6ix.app.ui.components.RemoteAvatar
import com.pandora6ix.app.data.businessDateTimeLabel
import com.pandora6ix.app.data.TagDto
import com.pandora6ix.app.data.TaskRepository
import com.pandora6ix.app.data.TaskReviewDto
import com.pandora6ix.app.data.reviewResultKey
import com.pandora6ix.app.ui.calendar.tagHexColor
import com.pandora6ix.app.ui.theme.CoralDark
import com.pandora6ix.app.ui.theme.CreamDeep
import com.pandora6ix.app.ui.theme.Ink
import com.pandora6ix.app.ui.theme.Mint
import com.pandora6ix.app.ui.theme.MintDeep
import com.pandora6ix.app.ui.theme.Peach
import com.pandora6ix.app.ui.theme.WarmCard
import com.pandora6ix.app.ui.theme.WarmDashboard
import com.pandora6ix.app.ui.theme.WarmOrange
import com.pandora6ix.app.ui.theme.priorityMeta
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
fun ReviewScreen(taskRepo: TaskRepository, availableTags: List<TagDto>, businessTimezone: String? = null, userRepo: UserRepository? = null, canReview: Boolean = true, readResultKeys: Set<String> = emptySet(), refreshRevision: Int = 0, focusReviewId: String? = null, onResultRead: (TaskReviewDto) -> Unit = {}, onBack: () -> Unit) {
    // 无审批权限时直接隐藏"待我审批"入口，仅展示自己提交的申请
    var scope by rememberSaveable { mutableStateOf(if (canReview) "pending" else "created") }
    var items by remember(scope) { mutableStateOf<List<TaskReviewDto>>(emptyList()) }
    var nextCursor by remember(scope) { mutableStateOf<String?>(null) }
    var loadedPageCount by remember(scope) { mutableStateOf(0) }
    // 位于详情分支之外，进入详情时不会随 LazyColumn 一同销毁。
    val listState = rememberSaveable(scope, saver = LazyListState.Saver) { LazyListState() }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    // 403 无权限：重试无意义，改为引导查看自己提交的申请
    var loadDenied by remember { mutableStateOf(false) }
    var loadRetry by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<TaskReviewDto?>(null) }
    var loadGeneration by remember { mutableStateOf(0) }
    val uiScope = rememberCoroutineScope()

    fun load(append: Boolean, cursor: String? = null) {
        val generation = ++loadGeneration
        val requestedScope = scope
        val pagesToRefresh = loadedPageCount.coerceAtLeast(1)
        loading = true; loadError = null
        uiScope.launch {
            try {
                val window = if (append) {
                    val page = taskRepo.reviews(requestedScope, cursor = cursor)
                    ReviewListWindow((items + page.items).distinctBy { it.id }, page.nextCursor, pagesToRefresh + 1)
                } else {
                    refreshReviewWindow(pagesToRefresh) { pageCursor ->
                        taskRepo.reviews(requestedScope, cursor = pageCursor)
                    }
                }
                if (generation != loadGeneration || requestedScope != scope) return@launch
                items = window.items
                nextCursor = window.nextCursor
                loadedPageCount = window.pageCount
                loadDenied = false
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: Exception) {
                if (generation != loadGeneration || requestedScope != scope) return@launch
                val api = e as? ApiException
                loadDenied = api?.httpStatus == 403
                loadError = when {
                    loadDenied -> "当前账号没有审批权限，无需处理审批，可查看自己提交的申请"
                    api != null -> api.friendlyMessage()
                    else -> "加载失败，请重试"
                }
                // 刷新中间页失败时保留完整的旧窗口；权限失效则隐藏旧内容。
                if (loadDenied) { items = emptyList(); nextCursor = null; loadedPageCount = 0 }
            } finally {
                if (generation == loadGeneration) loading = false
            }
        }
    }
    LaunchedEffect(scope, loadRetry, refreshRevision, selected?.id) {
        if (selected == null) load(append = false)
    }
    // 从消息进入：本页列表里能找到该条审批时，直接展开它的详情
    LaunchedEffect(focusReviewId, items) {
        val target = focusReviewId ?: return@LaunchedEffect
        if (selected?.id == target) return@LaunchedEffect
        items.firstOrNull { it.id == target }?.let { selected = it }
    }

    selected?.let { review ->
        ReviewDetail(review, taskRepo, availableTags, businessTimezone, userRepo, onBack = { selected = null }, onChanged = { load(append = false) })
        return
    }

    // 背景与设置页一致（主题 dashboard 色），避免同一主题下不同页面底色深浅不一
    Column(Modifier.fillMaxSize().background(WarmDashboard)) {
        TopAppBar(
            title = { Text("审批中心", fontWeight = FontWeight.Bold, maxLines = 1) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canReview) QueueTab("待我审批", scope == "pending", Modifier.weight(1f)) { scope = "pending" }
            QueueTab("我提交的", scope == "created", Modifier.weight(1f)) { scope = "created" }
        }
        Box(Modifier.fillMaxSize()) {
            when {
                loading && items.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(36.dp), color = WarmOrange)
                loadError != null && items.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError!!, color = Ink.copy(alpha = .7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    if (loadDenied) OutlinedButton({ scope = "created" }) { Text("查看我提交的") }
                    else OutlinedButton({ loadRetry++ }) { Text("重试") }
                }
                items.isEmpty() -> Text(
                    if (scope == "pending") "暂无待审批的申请" else "暂无提交的申请",
                    Modifier.align(Alignment.Center), color = Ink.copy(alpha = .5f)
                )
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { review ->
                        val unread = scope == "created" && reviewResultKey(review)?.let { it !in readResultKeys } == true
                        ReviewCard(review, businessTimezone, unread) {
                            if (scope == "created") onResultRead(review)
                            selected = review
                        }
                    }
                    if (loadError != null) {
                        item(key = "review-load-error") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Text(loadError!!, color = Ink.copy(alpha = .7f))
                                OutlinedButton({ loadRetry++ }, enabled = !loading) { Text("重试") }
                            }
                        }
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
private fun ReviewCard(review: TaskReviewDto, businessTimezone: String?, unread: Boolean = false, onOpen: () -> Unit) {
    val snapshot = parseSnapshot(review.submittedSnapshot)
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(snapshot.title.ifBlank { "未命名任务" }, Modifier.weight(1f), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                DecisionBadge(review.decision)
                if (unread) Text("新结果", color = WarmOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("${kindLabel(snapshot.kind)} · 请求人 ${personName(review.submittedByName, review.submittedByUserId)}", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                review.submittedAt?.let { Text(timeLabel(it, businessTimezone), fontSize = 12.sp, color = Ink.copy(alpha = .6f)) }
            }
            // 已有结果时直接展示审批人与结果时间，不用进详情才知道
            if (review.decision != "PENDING") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("审批人 ${personName(review.reviewerName, review.reviewerUserId)}", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                    review.decidedAt?.let { Text(timeLabel(it, businessTimezone), fontSize = 12.sp, color = Ink.copy(alpha = .6f)) }
                }
            }
            if (review.decision != "PENDING" && review.decisionNote?.isNotBlank() == true) {
                Text("批注：${review.decisionNote}", fontSize = 12.sp, color = Ink.copy(alpha = .6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 详情：快照字段 + 审批操作。409 后重读最新 review；成功后回调刷新队列 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReviewDetail(review: TaskReviewDto, taskRepo: TaskRepository, availableTags: List<TagDto>, businessTimezone: String?, userRepo: UserRepository?, onBack: () -> Unit, onChanged: () -> Unit) {
    var current by remember { mutableStateOf(review) }
    var comment by rememberSaveable { mutableStateOf("") }
    // 批注输入条展开状态：点击批注入口后在键盘上方显示输入条（微信样式）
    var commentOpen by rememberSaveable { mutableStateOf(false) }
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
    // 背景与设置页一致（主题 dashboard 色）
    Column(Modifier.fillMaxSize().background(WarmDashboard)) {
        TopAppBar(
            title = { Text("审批详情", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        val tagNames = snapshot.tagIds.mapNotNull { id -> availableTags.firstOrNull { it.id == id } }
        val executors = executorActors(current, snapshot)
        LazyColumn(Modifier.fillMaxSize().weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // 概览：标题 + 决策 + 类型/重要程度 + 说明
            item {
                SectionCard("任务信息") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(snapshot.title.ifBlank { "未命名任务" }, Modifier.weight(1f), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        DecisionBadge(current.decision)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        val level = (snapshot.priorityLevel ?: 3).coerceIn(1, 5)
                        val (starColor, starText) = priorityMeta(level)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("★".repeat(level) + "☆".repeat(5 - level), color = starColor, fontSize = 13.sp)
                            Text(starText, fontSize = 13.sp, color = Ink.copy(alpha = .75f))
                        }
                    }
                    snapshot.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, fontSize = 14.sp, lineHeight = 21.sp, color = Ink.copy(alpha = .85f))
                    }
                }
            }
            // 时间：全天/指定时间、跨天日期范围
            item {
                SectionCard("时间安排") {
                    snapshot.recurrenceLabel?.let { InfoRow("重复安排", it) }
                    InfoRow("日期", dateRangeLabel(snapshot.startDate, snapshot.endDate))
                    InfoRow(
                        "时间",
                        if (snapshot.timeKind == "TIMED") "${shortTime(snapshot.startTime)} – ${shortTime(snapshot.endTime)}" else "全天"
                    )
                }
            }
            // 人员：请求人 / 审批人 / 执行人（真实姓名 + 职位 + 头像）
            item {
                SectionCard("人员") {
                    PersonRow(
                        "请求人",
                        personName(current.submittedByName, current.submittedByUserId),
                        current.submittedByJobTitle,
                        current.submittedByUserId,
                        userRepo,
                        "提交时间：${timeLabel(current.submittedAt, businessTimezone)}"
                    )
                    PersonRow(
                        "审批人",
                        personName(current.reviewerName, current.reviewerUserId),
                        current.reviewerJobTitle,
                        current.reviewerUserId,
                        userRepo,
                        null
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("执行人", fontSize = 13.sp, color = Ink)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            executors.forEach { PersonChip(it, userRepo) }
                        }
                    }
                }
            }
            if (tagNames.isNotEmpty()) item {
                SectionCard("标签") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        tagNames.forEach { tag ->
                            // 标签用服务端 colorHex 自己的颜色（与任务详情、日志一致），不跟随主题色
                            Text(
                                "${tag.name}${if (tag.id == snapshot.primaryTagId) " · 主" else ""}",
                                Modifier.clip(RoundedCornerShape(10.dp))
                                    .background(tagHexColor(tag.colorHex))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                fontSize = 12.sp,
                                color = Ink
                            )
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
                SectionCard("审批结果") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (current.decision == "APPROVED") "已批准" else "已驳回", Modifier.weight(1f), fontWeight = FontWeight.Bold, color = if (current.decision == "APPROVED") MintDeep else CoralDark)
                        DecisionBadge(current.decision)
                    }
                    InfoRow("审批人", personName(current.reviewerName, current.reviewerUserId))
                    current.decisionNote?.takeIf { it.isNotBlank() }?.let { InfoRow("批注", it) }
                    current.decidedAt?.let { InfoRow("决定时间", timeLabel(it, businessTimezone)) }
                }
            }
            if (canDecide) item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // 批注入口：点击后在屏幕底部、键盘上方展开输入条（微信打字框样式）
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CreamDeep).clickable { commentOpen = true }.padding(horizontal = 12.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (comment.isBlank()) "写批注（驳回时建议填写）" else "批注：$comment",
                            Modifier.weight(1f), fontSize = 13.sp,
                            color = if (comment.isBlank()) Ink.copy(alpha = .5f) else Ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.Edit, "编辑批注", Modifier.size(16.dp), tint = Ink.copy(alpha = .5f))
                    }
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
        // 底部批注输入条：固定在键盘上方（adjustResize 已避让），最多 5 行、超出内部滚动，可完整查看已输入内容
        if (canDecide && commentOpen) {
            val keyboard = LocalSoftwareKeyboardController.current
            val commentFocus = remember { FocusRequester() }
            LaunchedEffect(commentOpen) { if (commentOpen) commentFocus.requestFocus() }
            Surface(color = WarmCard, shadowElevation = 8.dp) {
                // targetSdk 35 强制 edge-to-edge，adjustResize 不生效：必须用 imePadding 把输入条顶到键盘上方
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        Modifier.weight(1f).focusRequester(commentFocus).heightIn(min = 44.dp, max = 168.dp),
                        placeholder = { Text("输入批注（驳回时建议填写）") },
                        minLines = 1,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .7f), unfocusedBorderColor = Ink.copy(alpha = .3f), focusedLabelColor = Ink, cursorColor = Ink)
                    )
                    TextButton({ keyboard?.hide(); commentOpen = false }, Modifier.padding(bottom = 6.dp)) { Text("完成", color = Ink, fontWeight = FontWeight.Bold) }
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

/** 详情分区：小标题 + 主题卡片，卡片内统一间距 */
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = .5f))
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.width(64.dp), fontSize = 13.sp, color = Ink.copy(alpha = .6f))
        Text(value, Modifier.weight(1f), fontSize = 14.sp, lineHeight = 20.sp)
    }
}

/** 文字小标签：类型等次要信息 */
@Composable
private fun MetaChip(text: String) {
    Box(Modifier.clip(RoundedCornerShape(10.dp)).background(CreamDeep).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, fontSize = 12.sp, color = Ink.copy(alpha = .8f))
    }
}

/** 人员行：角色标签（左对齐、与“执行人”同线）+ 头像 + 姓名 + 职位胶囊 + 补充信息 */
@Composable
private fun PersonRow(label: String, name: String, jobTitle: String?, userId: String, userRepo: UserRepository?, sub: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 13.sp, color = Ink)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RemoteAvatar(userId, name, 38.dp, 16.sp, userRepo)
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
                    jobTitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            Modifier.clip(RoundedCornerShape(8.dp)).background(CreamDeep).padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            color = Ink.copy(alpha = .65f)
                        )
                    }
                }
                if (!sub.isNullOrBlank()) Text(sub, fontSize = 12.sp, color = Ink.copy(alpha = .5f))
            }
        }
    }
}

/** 执行人：与请求人/审批人同行内格式一致（38dp 头像、14sp 姓名），身份职位用黄色小标签，无背景胶囊 */
@Composable
private fun PersonChip(actor: ReviewActorDto, userRepo: UserRepository?) {
    val name = personName(actor.displayName, actor.id)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RemoteAvatar(actor.id, name, 38.dp, 16.sp, userRepo)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(name, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
            actor.jobTitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, Modifier.clip(RoundedCornerShape(8.dp)).background(CreamDeep).padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, color = Ink.copy(alpha = .65f))
            }
        }
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
        primaryTagId = str("primaryTagId"),
        recurrenceLabel = str("recurrenceLabel")
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
    val primaryTagId: String?,
    val recurrenceLabel: String? = null
)

private fun kindLabel(kind: String?) = if (kind == "DELEGATED") "派发任务" else "个人任务"

/** 姓名缺失（旧数据/已停用账号）时回退显示 ID 后四位 */
private fun personName(name: String?, id: String): String =
    name?.takeIf { it.isNotBlank() } ?: "成员 ${id.takeLast(4)}"

/** 执行人：优先用后端返回的执行关系（真实姓名 + 职位），缺省时回退到快照的统计口径 */
private fun executorActors(review: TaskReviewDto, snapshot: SnapshotInfo): List<ReviewActorDto> {
    val actors = review.recipients.filter { it.id.isNotBlank() || !it.displayName.isNullOrBlank() }
    if (actors.isNotEmpty()) return actors
    return listOf(
        ReviewActorDto(
            id = "",
            displayName =
                if (snapshot.kind == "DELEGATED" && snapshot.recipientIds.isNotEmpty()) "接收人 ${snapshot.recipientIds.size} 人"
                else "本人执行"
        )
    )
}

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
