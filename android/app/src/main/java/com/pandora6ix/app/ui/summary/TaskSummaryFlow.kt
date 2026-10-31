package com.pandora6ix.app.ui.summary

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
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                    Text(title, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
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
                    }) { Icon(Icons.Default.Refresh, "刷新") }
                }
                when (page) {
                    "organization" -> OrganizationContent(people, canManage, userRepo, { openMember(it) }, { scope.launch { people.load() } })
                    "tasks" -> SummaryPanel(Modifier.weight(1f).padding(16.dp)) {
                        val allowed = canRead && (listTaskScope != "managed" || canManage)
                        val records = listMemberId?.let { responsibleTasks(fullTasks.items, it) } ?: fullTasks.items
                        PanelHeading(listMemberId?.let { "${name(it)} · 负责的任务" } ?: TaskScopes.first { it.first == listTaskScope }.second)
                        if (!allowed) Note("当前账号没有相关任务查看权限")
                        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(records, key = { it.id }) { task -> SummaryTaskRow(task, name, userRepo) { taskId = task.id } }
                            item { CollectionFooter(fullTasks, records.isEmpty(), "暂无相关任务", filtered = listMemberId != null) { scope.launch { fullTasks.load() } } }
                        }
                    }
                    "journals" -> SummaryPanel(Modifier.weight(1f).padding(16.dp)) {
                        val records = listMemberId?.let { memberJournals(journals.items, it) } ?: journals.items
                        PanelHeading(listMemberId?.let { "${name(it)} · 日志" } ?: if (canManage) "下属日志" else "我的日志")
                        if (!canRead) Note("当前账号没有日志查看权限")
                        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(records, key = { it.id }) { journal -> JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id } }
                            item { CollectionFooter(journals, records.isEmpty(), "暂无相关日志", filtered = listMemberId != null) { scope.launch { journals.load() } } }
                        }
                    }
                    "member" -> {
                        val member = people.items.firstOrNull { it.id == memberId }
                        if (member == null || !canManage) Note("成员信息不可用，请返回组织架构重新加载。")
                        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                SummaryPanel {
                                    PanelHeading("个人信息")
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        RemoteAvatar(member.id, name(member.id), 56.dp, 23.sp, userRepo)
                                        Column(Modifier.weight(1f)) {
                                            Text(name(member.id), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                            Text(member.organizationName ?: "未提供所属组织", color = Ink.copy(alpha = .75f))
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
                    else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            SummaryPanel {
                                TabRow(selectedTabIndex = TaskScopes.indexOfFirst { it.first == taskScope }, containerColor = MaterialTheme.colorScheme.surface, contentColor = Ink) {
                                    TaskScopes.forEach { (value, label) ->
                                        Tab(selected = taskScope == value, onClick = { taskScope = value }, selectedContentColor = ThemeButton,
                                            unselectedContentColor = Ink.copy(alpha = .75f), text = { Text(label, maxLines = 2) })
                                    }
                                }
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
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(reports.take(PeoplePreviewLimit), key = { it.id }) { person ->
                                            Column(Modifier.width(68.dp).clip(RoundedCornerShape(8.dp)).clickable { openMember(person) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                RemoteAvatar(person.id, name(person.id), 44.dp, 18.sp, userRepo)
                                                Text(name(person.id), Modifier.padding(top = 6.dp), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
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
        Text("查看全部任务", color = ThemeButton)
        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = ThemeButton)
    }
}

@Composable
private fun Modifier.summaryEntry(onClick: () -> Unit): Modifier {
    val shape = RoundedCornerShape(10.dp)
    return clip(shape).background(LocalAppTheme.current.backgroundDeep.copy(alpha = .25f))
        .border(BorderStroke(.75.dp, Ink.copy(alpha = .12f)), shape).clickable(onClick = onClick)
}

@Composable
private fun SummaryPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, HairLine.copy(alpha = .8f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
private fun PanelHeading(title: String, onMore: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        if (onMore != null) TextButton(onMore, contentPadding = PaddingValues(start = 12.dp)) {
            Text("查看全部", color = Ink, maxLines = 1)
            Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = Ink)
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(text, Modifier.padding(vertical = 8.dp), fontSize = 13.sp, lineHeight = 20.sp, color = Ink.copy(alpha = .75f))
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.widthIn(min = 42.dp), fontSize = 13.sp, color = Ink.copy(alpha = .75f))
        Text(value, Modifier.weight(1f), fontSize = 14.sp)
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
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("电话", Modifier.widthIn(min = 42.dp), fontSize = 13.sp, color = Ink.copy(alpha = .75f))
        Text(when (val current = state) {
            MemberPhoneState.Loading -> "加载中…"
            is MemberPhoneState.Loaded -> current.phone?.takeIf { it.isNotBlank() } ?: "未填写"
            is MemberPhoneState.Failed -> current.text
        }, Modifier.weight(1f), fontSize = 14.sp)
        if (state is MemberPhoneState.Failed) TextButton({ attempt++ }) { Text("重试", color = ThemeButton) }
    }
}

@Composable
private fun <T> CollectionFooter(
    pager: SummaryPager<T>, empty: Boolean, emptyText: String, filtered: Boolean = false,
    showMore: Boolean = true, partialEmptyText: String = "已加载范围暂无相关内容，可继续加载", onMore: () -> Unit
) {
    when {
        pager.loading || !pager.initialized -> Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(Modifier.size(18.dp), color = ThemeButton, strokeWidth = 2.dp)
            Note("正在加载…")
        }
        pager.error != null -> Column {
            Text(pager.error!!, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            TextButton(onMore) { Text("重试", color = ThemeButton) }
        }
        else -> {
            if (empty) Note(if (filtered && pager.nextCursor != null) partialEmptyText else emptyText)
            if (showMore && pager.nextCursor != null) TextButton(onMore) { Text("加载更多", color = ThemeButton) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryTaskRow(task: TaskDto, name: (String) -> String, userRepo: UserRepository, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().summaryEntry(onOpen).padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(task.title, Modifier.weight(1f), fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val states = task.authorizedAssignments.map { it.executionStatus }.distinct()
            val completed = states.isNotEmpty() && states.all { it == "COMPLETED" }
            val label = summaryTaskStatus(task)
            val bg = when { completed -> Mint; task.reviewStatus == "REJECTED" -> Peach; else -> CreamDeep }
            Text(label, Modifier.widthIn(max = 96.dp).clip(RoundedCornerShape(6.dp)).background(bg).padding(horizontal = 6.dp, vertical = 3.dp),
                fontSize = 10.sp, lineHeight = 14.sp, color = if (completed) MintDeep else Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            task.tags.forEach { tag ->
                Text(tag.name, Modifier.widthIn(max = 180.dp).clip(RoundedCornerShape(6.dp)).background(tagHexColor(tag.colorHex).copy(alpha = .35f)).padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val priority = priorityMeta(task.priorityLevel)
            Text("★ ${priority.second}", Modifier.clip(RoundedCornerShape(6.dp)).background(CreamDeep).padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 11.sp, lineHeight = 14.sp, color = Ink)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Default.Event, null, Modifier.size(13.dp), tint = Ink.copy(alpha = .75f))
            Text("截止 ${task.endDate}" + if (task.timeKind == "TIMED") " ${task.endTime.orEmpty()}" else "", Modifier.weight(1f), fontSize = 11.sp, lineHeight = 16.sp, color = Ink.copy(alpha = .75f))
            val executors = executorIds(task)
            LazyRow(Modifier.widthIn(max = 112.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(executors, key = { it }) { id -> RemoteAvatar(id, name(id), 22.dp, 10.sp, userRepo) }
            }
            if (executors.isEmpty()) Text("暂无可见执行人", fontSize = 11.sp, color = Ink.copy(alpha = .75f))
        }
    }
}

@Composable
private fun JournalRow(journal: JournalDto, name: String, timezone: String?, userRepo: UserRepository, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().summaryEntry(onOpen).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RemoteAvatar(journal.authorUserId, name, 36.dp, 15.sp, userRepo)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(businessDateTimeLabel(journal.publishedAt, timezone).ifBlank { journal.workDate }, fontSize = 11.sp, color = Ink.copy(alpha = .75f))
            }
            Text(journalSummary(journal), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = Ink.copy(alpha = .8f))
        }
    }
}

@Composable
private fun OrganizationContent(people: SummaryPager<RecipientDto>, canManage: Boolean, userRepo: UserRepository, onMember: (RecipientDto) -> Unit, onMore: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    val groups = remember(people.items) { recipientOrganizationTree(people.items) }
    SummaryPanel(Modifier.fillMaxSize().padding(16.dp)) {
        if (!canManage) Note("当前账号没有组织架构查看权限")
        else LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            fun androidx.compose.foundation.lazy.LazyListScope.group(node: RecipientOrganizationGroup, depth: Int) {
                item(key = node.key) {
                    Row(Modifier.fillMaxWidth().padding(start = (depth * 12).dp).summaryEntry {
                        expanded = if (node.key in expanded) expanded - node.key else expanded + node.key
                    }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (node.key in expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, if (node.key in expanded) "收起" else "展开")
                        Text(node.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    }
                }
                if (node.key in expanded) {
                    items(node.members, key = { "member:${it.id}" }) { person ->
                        Row(Modifier.fillMaxWidth().padding(start = (12 + depth * 12).dp).summaryEntry { onMember(person) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            RemoteAvatar(person.id, person.displayName, 36.dp, 15.sp, userRepo)
                            Column(Modifier.weight(1f)) {
                                Text(person.displayName.ifBlank { "成员 ${person.id.takeLast(4)}" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                person.jobTitle?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = Ink.copy(alpha = .75f)) }
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
