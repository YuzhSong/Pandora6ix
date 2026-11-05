package com.pandora6ix.app.ui.review

import com.pandora6ix.app.ui.theme.*
import com.pandora6ix.app.ui.components.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.border
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
import com.pandora6ix.app.data.UserDto
import com.pandora6ix.app.data.TaskDraftDto
import com.pandora6ix.app.data.rejectedTaskDraft
import com.pandora6ix.app.mock.DemoDate
import com.pandora6ix.app.ui.calendar.TaskEntrySheet
import java.time.LocalDate
import com.pandora6ix.app.data.TagDto
import com.pandora6ix.app.data.TaskRepository
import com.pandora6ix.app.data.TaskReviewDto
import com.pandora6ix.app.data.reviewResultKey
import com.pandora6ix.app.ui.calendar.tagHexColor
import com.pandora6ix.app.ui.theme.LocalAppTheme
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import com.pandora6ix.app.ui.theme.priorityMeta
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
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
fun ReviewScreen(taskRepo: TaskRepository, availableTags: List<TagDto>, businessTimezone: String? = null, userRepo: UserRepository? = null, canReview: Boolean = true, readResultKeys: Set<String> = emptySet(), refreshRevision: Int = 0, focusReviewId: String? = null, onResultRead: (TaskReviewDto) -> Unit = {}, currentUser: UserDto? = null, onBack: () -> Unit) {
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
    // 从消息进入：本页列表里能找到该条审批时，直接展开它的详情；
    // 找不到（如审批结果在「我提交的申请」里）时切换到 created 范围再找
    LaunchedEffect(focusReviewId, items) {
        val target = focusReviewId ?: return@LaunchedEffect
        if (selected?.id == target) return@LaunchedEffect
        val found = items.firstOrNull { it.id == target }
        if (found != null) selected = found
        else if (scope != "created") scope = "created"
    }

    selected?.let { review ->
        ReviewDetail(review, taskRepo, availableTags, businessTimezone, userRepo, currentUser, onBack = { selected = null }, onChanged = { load(append = false) })
        return
    }

    // 列表页与详情页一致：系统返回关闭审批中心，而不是直接退出应用
    androidx.activity.compose.BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(PandoraTheme.colors.appBackground)) {
        PandoraTopBar("审批中心", onBack = onBack)
        // 两个审批队列统一为标准 Tab（48dp、Primary 2dp 下划线）
        val queues = (if (canReview) listOf("pending" to "待我审批") else emptyList()) + listOf("created" to "我提交的")
        PandoraTabRow(queues.map { it.second }, queues.indexOfFirst { it.first == scope }.coerceAtLeast(0), { scope = queues[it].first },
            Modifier.padding(horizontal = PandoraSpacing.pageHorizontal))
        Box(Modifier.fillMaxSize()) {
            when {
                loading && items.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(36.dp), color = PandoraTheme.colors.primary)
                loadError != null && items.isEmpty() -> Column(Modifier.align(Alignment.Center).padding(PandoraSpacing.space32), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError!!, color = PandoraTheme.colors.textSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(PandoraSpacing.space12))
                    if (loadDenied) PandoraButton("查看我提交的", { scope = "created" }, variant = PandoraButtonVariant.Outline)
                    else PandoraButton("重试", { loadRetry++ }, variant = PandoraButtonVariant.Outline)
                }
                items.isEmpty() -> Text(
                    if (scope == "pending") "暂无待审批的申请" else "暂无提交的申请",
                    Modifier.align(Alignment.Center), color = PandoraTheme.colors.textTertiary
                )
                else -> LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = androidx.compose.foundation.layout.PaddingValues(PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
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
                                Text(loadError!!, color = PandoraTheme.colors.textSecondary)
                                PandoraButton("重试", { loadRetry++ }, variant = PandoraButtonVariant.Outline, enabled = !loading)
                            }
                        }
                    }
                    nextCursor?.let { cursor ->
                        item {
                            Box(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space8), contentAlignment = Alignment.Center) {
                                if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = PandoraTheme.colors.primary)
                                else PandoraButton("加载更多", { load(append = true, cursor = cursor) }, variant = PandoraButtonVariant.Outline)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewCard(review: TaskReviewDto, businessTimezone: String?, unread: Boolean = false, onOpen: () -> Unit) {
    val snapshot = parseSnapshot(review.submittedSnapshot)
    PandoraPanel(Modifier.fillMaxWidth(), shape = PandoraShapes.medium, contentPadding = PaddingValues(0.dp), onClick = onOpen) {
        Column(Modifier.padding(horizontal = PandoraSpacing.space16, vertical = PandoraSpacing.space12), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                Text(snapshot.title.ifBlank { "未命名任务" }, Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis, style = PandoraTypography.labelLarge)
                DecisionBadge(review.decision)
                if (unread) PandoraStatusChip("新结果", StatusTone.INFO)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                Text("${kindLabel(snapshot.kind)} · 请求人 ${personName(review.submittedByName, review.submittedByUserId)}", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                review.submittedAt?.let { Text(timeLabel(it, businessTimezone), style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary) }
            }
            // 已有结果时直接展示审批人与结果时间，不用进详情才知道
            if (review.decision != "PENDING") {
                Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                    Text("审批人 ${personName(review.reviewerName, review.reviewerUserId)}", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                    review.decidedAt?.let { Text(timeLabel(it, businessTimezone), style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary) }
                }
            }
            if (review.decision != "PENDING" && review.decisionNote?.isNotBlank() == true) {
                Text("批注：${review.decisionNote}", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 详情：快照字段 + 审批操作。409 后重读最新 review；成功后回调刷新队列 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ReviewDetail(review: TaskReviewDto, taskRepo: TaskRepository, availableTags: List<TagDto>, businessTimezone: String?, userRepo: UserRepository?, currentUser: UserDto?, onBack: () -> Unit, onChanged: () -> Unit) {
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
    var resubmitDraft by remember(current.id) { mutableStateOf<TaskDraftDto?>(null) }
    resubmitDraft?.let { draft ->
        val date = LocalDate.parse(draft.startKey)
        TaskEntrySheet(initialDate = DemoDate(date.year, date.monthValue, date.dayOfMonth),
            canCreatePersonal = currentUser?.capabilities?.createPersonalTasks == true,
            canCreateDelegated = currentUser?.capabilities?.createDelegatedTasks == true,
            availableTags = availableTags, taskRepo = taskRepo, businessTimezone = businessTimezone,
            currentUser = currentUser, newTaskDraft = draft, draftKey = "resubmit_${current.id}.json",
            onDismiss = { resubmitDraft = null }, onCreated = {
                resubmitDraft = null
                doneMessage = "已提交新的任务，原驳回记录已保留"
            })
    }
    val snapshot = parseSnapshot(current.submittedSnapshot)
    val canDecide = current.decision == "PENDING" && current.allowedActions.isNotEmpty()
    val stale = current.taskVersion in 1L until current.currentTaskVersion

    fun refresh() {
        uiScope.launch {
            try {
                current = taskRepo.review(current.id)
                error = null
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
    Column(Modifier.fillMaxSize().background(PandoraTheme.colors.appBackground)) {
        PandoraTopBar("审批详情", onBack = onBack)
        val tagNames = snapshot.tagIds.sortedBy { if (it == snapshot.primaryTagId) 0 else 1 }.mapNotNull { id -> availableTags.firstOrNull { it.id == id } }
        val executors = executorActors(current, snapshot)
        LazyColumn(Modifier.fillMaxSize().weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
            // 概览：标题 + 决策 + 类型/重要程度 + 说明
            item {
                SectionCard("任务信息") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        Text(snapshot.title.ifBlank { "未命名任务" }, Modifier.weight(1f), style = PandoraTypography.titleSmall)
                        DecisionBadge(current.decision)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
                        val level = (snapshot.priorityLevel ?: 3).coerceIn(1, 5)
                        val (starColor, starText) = priorityMeta(level)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                            Text("★".repeat(level) + "☆".repeat(5 - level), color = starColor, style = PandoraTypography.bodyMedium)
                            Text(starText, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
                        }
                    }
                    snapshot.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
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
                    Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        Text("执行人", style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textPrimary)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                            executors.forEach { PersonChip(it, userRepo) }
                        }
                    }
                }
            }
            if (tagNames.isNotEmpty()) item {
                SectionCard("标签") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        tagNames.forEach { tag ->
                            // 标签用服务端 colorHex 自己的颜色（与任务详情、日志一致），不跟随主题色
                            PandoraTagChip(tag.name, tag.colorHex, primary = tag.id == snapshot.primaryTagId)
                        }
                    }
                }
            }
            if (stale) item {
                Text("任务在提交后已有修改，以下为提交当时的内容快照。", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
            }
            if (current.decision == "PENDING" && current.allowedActions.isEmpty()) item {
                Text("该申请当前不可操作（权限或人员范围已变化），请由创建者修改后重新提交。", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
            }
            if (current.decision != "PENDING") item {
                SectionCard("审批结果") {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        Text(if (current.decision == "APPROVED") "已批准" else "已驳回", Modifier.weight(1f), color = if (current.decision == "APPROVED") PandoraTheme.colors.success else PandoraTheme.colors.error, style = PandoraTypography.labelLarge)
                        DecisionBadge(current.decision)
                    }
                    InfoRow("审批人", personName(current.reviewerName, current.reviewerUserId))
                    current.decisionNote?.takeIf { it.isNotBlank() }?.let { InfoRow("批注", it) }
                    current.decidedAt?.let { InfoRow("决定时间", timeLabel(it, businessTimezone)) }
                    if (current.decision == "REJECTED" && current.reviewKind == "CREATE" &&
                        currentUser?.id == current.submittedByUserId &&
                        (currentUser.capabilities.createPersonalTasks || currentUser.capabilities.createDelegatedTasks)) {
                        PandoraButton("重新编辑并发布", {
                            runCatching { rejectedTaskDraft(current, currentUser.id, availableTags) }
                                .onSuccess { resubmitDraft = it; error = null }
                                .onFailure { error = "原申请内容不完整，请刷新后重试" }
                        })
                        error?.let {
                            Text(it, color = PandoraTheme.colors.error, style = PandoraTypography.bodyMedium)
                            TextButton({ refresh() }) { Text("重新加载申请") }
                        }
                    }

                }
            }
            if (canDecide) item {
                Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                    // 批注入口：点击后在屏幕底部、键盘上方展开输入条（微信打字框样式）
                    Row(
                        Modifier.fillMaxWidth().clip(PandoraShapes.medium).background(PandoraTheme.colors.surface).clickable { commentOpen = true }.padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space16),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (comment.isBlank()) "写批注（驳回时建议填写）" else "批注：$comment",
                            Modifier.weight(1f), style = PandoraTypography.bodyMedium,
                            color = if (comment.isBlank()) PandoraTheme.colors.textTertiary else PandoraTheme.colors.textPrimary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(PandoraSpacing.space8))
                        Icon(Icons.Default.Edit, "编辑批注", Modifier.size(16.dp), tint = PandoraTheme.colors.textTertiary)
                    }
                    if (error != null) Text(error!!, color = PandoraTheme.colors.error, style = PandoraTypography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                        PandoraButton("批准", { confirmAction = "APPROVE" }, Modifier.weight(1f), enabled = !submitting)
                        PandoraButton("驳回", { confirmAction = "REJECT" }, Modifier.weight(1f), variant = PandoraButtonVariant.Destructive, enabled = !submitting)
                    }
                }
            }
        }
        // 底部批注输入条：固定在键盘上方（adjustResize 已避让），最多 5 行、超出内部滚动，可完整查看已输入内容
        if (canDecide && commentOpen) {
            val keyboard = LocalSoftwareKeyboardController.current
            val commentFocus = remember { FocusRequester() }
            LaunchedEffect(commentOpen) { if (commentOpen) commentFocus.requestFocus() }
            // 键盘上方的浮动输入条：白色浮层 + 3dp 阴影（与 Sheet 同级），不再用 8dp 厚阴影
            Surface(color = PandoraTheme.colors.elevatedSurface, shadowElevation = 3.dp) {
                // targetSdk 35 强制 edge-to-edge，adjustResize 不生效：必须用 imePadding 把输入条顶到键盘上方
                Row(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    PandoraTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        Modifier.weight(1f).focusRequester(commentFocus).heightIn(min = 44.dp, max = 168.dp),
                        placeholder = { Text("输入批注（驳回时建议填写）") },
                        minLines = 1,
                        maxLines = 5
                    )
                    TextButton({ keyboard?.hide(); commentOpen = false }, Modifier.padding(bottom = PandoraSpacing.space8)) { Text("完成", color = PandoraTheme.colors.textPrimary, style = PandoraTypography.labelLarge) }
                }
            }
        }
    }

    confirmAction?.let { action ->
        PandoraAlertDialog(
            onDismissRequest = { if (!submitting) confirmAction = null },
            title = { Text(if (action == "APPROVE") "批准该申请？" else "驳回该申请？") },
            text = { Text(if (action == "APPROVE") "批准后任务立即生效，接收人可见。" else "驳回后创建者可修改内容重新提交。") },
            confirmButton = {
                if (submitting) CircularProgressIndicator(Modifier.size(22.dp), color = PandoraTheme.colors.primary)
                else TextButton({ submit(action) }) { Text(if (action == "APPROVE") "批准" else "驳回", style = PandoraTypography.labelLarge, color = if (action == "APPROVE") PandoraTheme.colors.primary else PandoraTheme.colors.error) }
            },
            dismissButton = { if (!submitting) PandoraDialogDismiss("取消", { confirmAction = null }) }
        )
    }
    doneMessage?.let { message ->
        PandoraAlertDialog(
            onDismissRequest = { },
            title = { Text("操作完成") },
            text = { Text(message) },
            confirmButton = { PandoraDialogConfirm("好的", { doneMessage = null; onChanged(); onBack() }) }
        )
    }
}

/** 详情分区：小标题 + 主题卡片，卡片内统一间距 */
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
        Text(title, style = PandoraTypography.labelLarge, color = PandoraTheme.colors.textSecondary, modifier = Modifier.semantics { heading() })
        PandoraPanel(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), content = content)
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
        Text(label, Modifier.widthIn(min = 64.dp), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
        Text(value, Modifier.weight(1f), style = PandoraTypography.bodyMedium)
    }
}

/** 文字小标签：类型等次要信息 */
@Composable
private fun MetaChip(text: String) {
    Box(Modifier.clip(PandoraShapes.pill).background(PandoraTheme.colors.neutralContainer).padding(horizontal = PandoraSpacing.space8, vertical = PandoraSpacing.space2)) {
        Text(text, style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
    }
}

/** 人员行：角色标签（左对齐、与“执行人”同线）+ 头像 + 姓名 + 职位胶囊 + 补充信息 */
@Composable
private fun PersonRow(label: String, name: String, jobTitle: String?, userId: String, userRepo: UserRepository?, sub: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
        Text(label, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textPrimary)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
            RemoteAvatar(userId, name, PandoraDimensions.avatarList, 16.sp, userRepo)
            Column(Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = PandoraTypography.labelLarge)
                    jobTitle?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            Modifier.clip(PandoraShapes.pill).background(PandoraTheme.colors.neutralContainer).padding(horizontal = PandoraSpacing.space8, vertical = PandoraSpacing.space2), style = PandoraTypography.caption,
                            color = PandoraTheme.colors.textSecondary
                        )
                    }
                }
                if (!sub.isNullOrBlank()) Text(sub, style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
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
        horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)
    ) {
        RemoteAvatar(actor.id, name, 38.dp, 16.sp, userRepo)
        Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
            Text(name, style = PandoraTypography.labelLarge)
            actor.jobTitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, Modifier.clip(PandoraShapes.small).background(PandoraTheme.colors.surface).padding(horizontal = PandoraSpacing.space8, vertical = PandoraSpacing.space2), style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
            }
        }
    }
}

@Composable
private fun DecisionBadge(decision: String) {
    PandoraStatusChip(when (decision) { "APPROVED" -> "已批准"; "REJECTED" -> "已驳回"; else -> "待审批" }, reviewStatusTone(decision))
}

/** submittedSnapshot 宽松解析：新快照字段与 TaskCreateDto 对应，旧快照可能缺字段或含历史字段 */
private fun parseSnapshot(snapshot: JsonObject?): SnapshotInfo {
    val obj = snapshot ?: return SnapshotInfo(null, "", null, null, null, null, null, null, null, emptyList(), emptyList(), null)
    // JsonNull.content 会返回字符串 "null"，这里按缺省处理
    fun str(key: String): String? = runCatching { obj[key]?.jsonPrimitive?.takeUnless { it is JsonNull }?.content }.getOrNull()
    fun int(key: String): Int? = runCatching { obj[key]?.jsonPrimitive?.intOrNull }.getOrNull()
    fun strings(key: String): List<String> = runCatching { obj[key]?.jsonArray?.mapNotNull { e -> e.jsonPrimitive.takeUnless { it is JsonNull }?.content } }.getOrNull().orEmpty()
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
