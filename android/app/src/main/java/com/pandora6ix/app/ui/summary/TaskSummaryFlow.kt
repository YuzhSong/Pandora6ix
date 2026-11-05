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

private const val PreviewLimit = 3
private const val PeoplePreviewLimit = 4
private val TaskScopes = listOf("mine" to "我的任务", "created" to "我创建的", "managed" to "下属任务")

/** Pagers live above destinations; previews never truncate the full authorized collections. */
@Composable
fun TaskSummaryFlow(
    user: UserDto,
    calendarRepo: CalendarRepository,
    taskRepo: TaskRepository,
    journalRepo: JournalRepository,
    userRepo: UserRepository,
    taskDetail: @Composable (String, () -> Unit, () -> Unit) -> Unit,
    journalDetail: @Composable (String, () -> Unit) -> Unit,
    onClose: () -> Unit
) {
    val canManage = user.capabilities.manage && user.capabilities.business
    val canRead = user.capabilities.business
    val taskPagers = remember(user.id) {
        TaskScopes.associate { (taskScope, _) -> taskScope to SummaryPager<TaskDto>({ it.id }) { cursor ->
            calendarRepo.tasks(scope = taskScope, cursor = cursor, limit = 20).let { SummaryPage(it.items, it.nextCursor) }
        } }
    }
    val people = remember(user.id) {
        SummaryPager<RecipientDto>({ it.id }) { cursor ->
            taskRepo.recipientCandidatePage(cursor = cursor).let { SummaryPage(it.items, it.nextCursor) }
        }
    }
    val journals = remember(user.id, canManage) {
        SummaryPager<JournalDto>({ it.id }) { cursor ->
            journalRepo.journals(scope = if (canManage) "managed" else "mine", cursor = cursor, limit = 20)
                .let { SummaryPage(it.items, it.nextCursor) }
        }
    }
    var page by rememberSaveable(user.id) { mutableStateOf("summary") }
    var taskScope by rememberSaveable(user.id) { mutableStateOf("mine") }
    var memberId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var memberParent by rememberSaveable(user.id) { mutableStateOf("summary") }
    var listParent by rememberSaveable(user.id) { mutableStateOf("summary") }
    var listTaskScope by rememberSaveable(user.id) { mutableStateOf("mine") }
    var listMemberId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var taskId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var journalId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var memberRefresh by remember(user.id) { mutableIntStateOf(0) }
    val uiStates = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val selectedTasks = taskPagers.getValue(taskScope)
    val managedTasks = taskPagers.getValue("managed")
    val fullTasks = taskPagers.getValue(listTaskScope)
    val name: (String) -> String = { summaryPersonName(it, user, people.items) }

    LaunchedEffect(taskScope, canRead, canManage) {
        if (canRead && (taskScope != "managed" || canManage)) selectedTasks.ensureLoaded()
    }
    LaunchedEffect(canManage) { if (canManage) people.ensureLoaded() }
    LaunchedEffect(canRead, canManage) { if (canRead) journals.ensureLoaded() }
    LaunchedEffect(page, listTaskScope, canRead, canManage) {
        if (page == "member" && canManage) managedTasks.ensureLoaded()
        if (page == "tasks" && canRead && (listTaskScope != "managed" || canManage)) fullTasks.ensureLoaded()
    }

    fun back() {
        when {
            taskId != null -> taskId = null
            journalId != null -> journalId = null
            page == "tasks" || page == "journals" -> page = listParent
            page == "member" -> { page = memberParent; memberId = null }
            page != "summary" -> page = "summary"
            else -> onClose()
        }
    }
    fun openMember(person: RecipientDto) {
        memberParent = page
        memberId = person.id
        page = "member"
    }
    fun openTasks(personId: String? = null) {
        listParent = page
        listMemberId = personId
        listTaskScope = if (personId == null) taskScope else "managed"
        page = "tasks"
    }
    fun openJournals(personId: String? = null) {
        listParent = page
        listMemberId = personId
        page = "journals"
    }
    fun refreshTasks() {
        taskPagers.values.filter { it.initialized }.forEach { pager -> scope.launch { pager.load(refresh = true) } }
    }
    BackHandler { back() }
    val stateKey = when (page) {
        "member" -> "member:$memberId"
        "tasks" -> "tasks:$listTaskScope:$listMemberId"
        "journals" -> "journals:$listMemberId"
        else -> page
    }
    when {
        taskId != null -> taskDetail(taskId!!, { taskId = null }, { refreshTasks() })
        journalId != null -> journalDetail(journalId!!) { journalId = null }
        else -> uiStates.SaveableStateProvider(stateKey) {
            Column(Modifier.fillMaxSize()) {
                val title = when (page) {
                    "organization" -> "组织架构"; "member" -> "成员详情"
                    "tasks" -> "任务列表"; "journals" -> "日志列表"; else -> "任务汇总"
                }
                PandoraTopBar(title, onBack = { back() }) {
                    IconButton({
                        when (page) {
                            "organization" -> if (canManage) scope.launch { people.load(refresh = true) }
                            "tasks" -> if (canRead && (listTaskScope != "managed" || canManage)) scope.launch { fullTasks.load(refresh = true) }
                            "journals" -> if (canRead) scope.launch { journals.load(refresh = true) }
                            "member" -> { memberRefresh++; refreshTasks(); if (canRead) scope.launch { journals.load(refresh = true) } }
                            else -> {
                                if (canRead && (taskScope != "managed" || canManage)) scope.launch { selectedTasks.load(refresh = true) }
                                if (canManage) scope.launch { people.load(refresh = true) }
                                if (canRead) scope.launch { journals.load(refresh = true) }
                            }
                        }
                    }) { Icon(Icons.Default.Refresh, "刷新", tint = PandoraTheme.colors.textSecondary) }
                }
                when (page) {
                    "organization" -> OrganizationContent(people, canManage, userRepo, { openMember(it) }, { scope.launch { people.load() } })
                    "tasks" -> SummaryPanel(Modifier.weight(1f).padding(PandoraSpacing.space16)) {
                        val allowed = canRead && (listTaskScope != "managed" || canManage)
                        val records = listMemberId?.let { responsibleTasks(fullTasks.items, it) } ?: fullTasks.items
                        PanelHeading(listMemberId?.let { "${name(it)} · 负责的任务" } ?: TaskScopes.first { it.first == listTaskScope }.second)
                        if (!allowed) Note("当前账号没有相关任务查看权限")
                        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                            items(records, key = { it.id }) { task -> SummaryTaskRow(task, name, userRepo) { taskId = task.id } }
                            item { CollectionFooter(fullTasks, records.isEmpty(), "暂无相关任务", filtered = listMemberId != null) { scope.launch { fullTasks.load() } } }
                        }
                    }
                    "journals" -> SummaryPanel(Modifier.weight(1f).padding(PandoraSpacing.space16)) {
                        val records = listMemberId?.let { memberJournals(journals.items, it) } ?: journals.items
                        PanelHeading(listMemberId?.let { "${name(it)} · 日志" } ?: if (canManage) "下属日志" else "我的日志")
                        if (!canRead) Note("当前账号没有日志查看权限")
                        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                            items(records, key = { it.id }) { journal -> JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id } }
                            item { CollectionFooter(journals, records.isEmpty(), "暂无相关日志", filtered = listMemberId != null) { scope.launch { journals.load() } } }
                        }
                    }
                    "member" -> {
                        val member = people.items.firstOrNull { it.id == memberId }
                        if (member == null || !canManage) Note("成员信息不可用，请返回组织架构重新加载。")
                        else LazyColumn(contentPadding = PaddingValues(PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
                            item {
                                SummaryPanel {
                                    PanelHeading("个人信息")
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
                                        RemoteAvatar(member.id, name(member.id), PandoraDimensions.avatarProfile, 24.sp, userRepo)
                                        Column(Modifier.weight(1f)) {
                                            Text(name(member.id), style = PandoraTypography.titleMedium)
                                            Text(member.organizationName ?: "未提供所属组织", color = PandoraTheme.colors.textSecondary)
                                        }
                                    }
                                    InfoLine("部门", member.departmentName ?: "未提供")
                                    InfoLine("职位", member.jobTitle?.takeIf { it.isNotBlank() } ?: "未设置")
                                    InfoLine("职级", "暂未提供")
                                    MemberPhone(member.id, userRepo, memberRefresh)
                                }
                            }
                            item {
                                SummaryPanel {
                                    PanelHeading("负责的任务")
                                    TaskPreview(managedTasks, responsibleTasks(managedTasks.items, member.id), name, userRepo,
                                        { taskId = it }, { openTasks(member.id) }, { scope.launch { managedTasks.load() } }, filtered = true)
                                }
                            }
                            item {
                                SummaryPanel {
                                    PanelHeading("最近日志", { openJournals(member.id) })
                                    val records = memberJournals(journals.items, member.id)
                                    records.take(PreviewLimit).forEach { journal ->
                                        JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id }
                                    }
                                    CollectionFooter(journals, records.isEmpty(), "暂无相关日志", filtered = true, showMore = false,
                                        partialEmptyText = "已加载范围暂无相关日志，可查看全部") { scope.launch { journals.load() } }
                                }
                            }
                        }
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
                        item {
                            SummaryPanel {
                                PandoraTabRow(TaskScopes.map { it.second }, TaskScopes.indexOfFirst { it.first == taskScope }.coerceAtLeast(0), { taskScope = TaskScopes[it].first })
                                if (!canRead || (taskScope == "managed" && !canManage)) Note(if (!canRead) "当前账号没有业务任务查看权限" else "当前账号没有下属任务查看权限")
                                else {
                                    TaskPreview(selectedTasks, selectedTasks.items, name, userRepo, { taskId = it }, { openTasks() }, { scope.launch { selectedTasks.load() } })
                                }
                            }
                        }
                        item {
                            SummaryPanel {
                                PanelHeading("直属下属", if (canManage) ({ page = "organization" }) else null)
                                if (!canManage) Note("当前账号没有下属查看权限")
                                else {
                                    val reports = directReports(people.items)
                                    // 直属四人按面板内可用宽等分（规范 4 节栅格），360dp 屏也能完整显示，不再写死 68dp 宽
                                    val shown = reports.take(PeoplePreviewLimit)
                                    if (shown.isNotEmpty()) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraGrid.gutter)) {
                                        shown.forEach { person ->
                                            Column(Modifier.weight(1f).heightIn(min = PandoraDimensions.touchTarget).clip(PandoraShapes.small).clickable(onClickLabel = "查看${name(person.id)}") { openMember(person) }.padding(vertical = PandoraSpacing.space4), horizontalAlignment = Alignment.CenterHorizontally) {
                                                RemoteAvatar(person.id, name(person.id), PandoraDimensions.avatarList, 16.sp, userRepo)
                                                Text(name(person.id), Modifier.padding(top = PandoraSpacing.space8), style = PandoraTypography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                        repeat(PeoplePreviewLimit - shown.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                    CollectionFooter(people, reports.isEmpty(), "暂无直属下属", filtered = true, showMore = false,
                                        partialEmptyText = "已加载范围暂无直属下属，可查看完整组织架构") { scope.launch { people.load() } }
                                }
                            }
                        }
                        item {
                            SummaryPanel {
                                PanelHeading("最近日志动态", if (canRead) ({ openJournals() }) else null)
                                if (!canRead) Note("当前账号没有日志查看权限")
                                else {
                                    journals.items.take(PreviewLimit).forEach { journal ->
                                        JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id }
                                    }
                                    CollectionFooter(journals, journals.items.isEmpty(), "暂无相关日志", showMore = false) { scope.launch { journals.load() } }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskPreview(
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
private fun Modifier.summaryEntry(onClick: () -> Unit): Modifier {
    val shape = PandoraShapes.medium
    // 任务/日志独立条目：Surface 底 + 1dp 中性边框 + 12 角（规范 8 节 TaskItem/JournalItem）
    return clip(shape).background(PandoraTheme.colors.surface)
        .border(BorderStroke(PandoraDimensions.borderWidth, PandoraTheme.colors.border), shape).clickable(onClick = onClick)
}

@Composable
private fun SummaryPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    // 用户确认的白色功能面板：panelSurface（与浮层同一白色值），0 阴影 + 1dp 边框
    PandoraPanel(modifier.fillMaxWidth(), tone = PandoraPanelTone.White) {
        Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12), content = content)
    }
}

@Composable
private fun PanelHeading(title: String, onMore: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), style = PandoraTypography.titleSmall)
        if (onMore != null) TextButton(onMore, contentPadding = PaddingValues(start = PandoraSpacing.space12)) {
            Text("查看全部", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary, maxLines = 1)
            Icon(Icons.Default.ChevronRight, null, Modifier.size(PandoraDimensions.iconSmall + 2.dp), tint = PandoraTheme.colors.primary)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, Modifier.padding(vertical = PandoraSpacing.space8), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
}

@Composable
private fun InfoLine(label: String, value: String) {
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
private fun MemberPhone(memberId: String, userRepo: UserRepository, refresh: Int) {
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
private fun <T> CollectionFooter(
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
private fun SummaryTaskRow(task: TaskDto, name: (String) -> String, userRepo: UserRepository, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().summaryEntry(onOpen).padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
            Text(task.title, Modifier.weight(1f), style = PandoraTypography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            // 状态色由集中映射给出，文字仍沿用汇总规则（多执行人显示“多种执行状态”）
            val label = summaryTaskStatus(task)
            val assignmentStates = task.authorizedAssignments.map { it.executionStatus to it.workStatus }.distinct()
            val tone = when {
                task.reviewStatus != "APPROVED" -> reviewStatusTone(task.reviewStatus)
                assignmentStates.size == 1 -> executionStatusTone(assignmentStates.single().first, assignmentStates.single().second)
                else -> StatusTone.NEUTRAL
            }
            PandoraStatusChip(label, tone, maxWidth = 96.dp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space4), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
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
private fun JournalRow(journal: JournalDto, name: String, timezone: String?, userRepo: UserRepository, onOpen: () -> Unit) {
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
private fun OrganizationContent(people: SummaryPager<RecipientDto>, canManage: Boolean, userRepo: UserRepository, onMember: (RecipientDto) -> Unit, onMore: () -> Unit) {
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
