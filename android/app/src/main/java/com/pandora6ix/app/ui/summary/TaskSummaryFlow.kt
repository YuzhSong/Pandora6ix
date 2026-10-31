package com.pandora6ix.app.ui.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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

/** Pagers live above destinations: opening details never discards loaded pages or parent state. */
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
        listOf("mine", "created", "managed").associateWith { taskScope ->
            SummaryPager<TaskDto>({ it.id }) { cursor ->
                calendarRepo.tasks(scope = taskScope, cursor = cursor, limit = 20).let { SummaryPage(it.items, it.nextCursor) }
            }
        }
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
    var taskId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var journalId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    val uiStates = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val selectedTasks = taskPagers.getValue(taskScope)
    val managedTasks = taskPagers.getValue("managed")

    LaunchedEffect(taskScope, canRead, canManage) {
        if (canRead && (taskScope != "managed" || canManage)) selectedTasks.ensureLoaded()
    }
    LaunchedEffect(canManage) { if (canManage) people.ensureLoaded() }
    LaunchedEffect(canRead, canManage) { if (canRead) journals.ensureLoaded() }
    LaunchedEffect(page, canManage) { if (page == "member" && canManage) managedTasks.ensureLoaded() }

    fun back() {
        when {
            taskId != null -> taskId = null
            journalId != null -> journalId = null
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
    fun refreshTasks() {
        taskPagers.values.filter { it.initialized }.forEach { pager -> scope.launch { pager.load(refresh = true) } }
    }
    BackHandler { back() }

    when {
        taskId != null -> taskDetail(taskId!!, { taskId = null }, { refreshTasks() })
        journalId != null -> journalDetail(journalId!!) { journalId = null }
        else -> uiStates.SaveableStateProvider(if (page == "member") "member:$memberId" else page) {
            Column(Modifier.fillMaxSize()) {
                val title = when (page) { "organization" -> "组织架构"; "member" -> "成员详情"; "journals" -> "最近日志动态"; else -> "任务汇总" }
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton({ back() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                    Text(title, Modifier.weight(1f), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink)
                    IconButton({
                        when (page) {
                            "organization" -> if (canManage) scope.launch { people.load(refresh = true) }
                            "journals" -> if (canRead) scope.launch { journals.load(refresh = true) }
                            "member" -> { refreshTasks(); if (canRead) scope.launch { journals.load(refresh = true) } }
                            else -> {
                                if (canRead && (taskScope != "managed" || canManage)) scope.launch { selectedTasks.load(refresh = true) }
                                if (canManage) scope.launch { people.load(refresh = true) }
                                if (canRead) scope.launch { journals.load(refresh = true) }
                            }
                        }
                    }) { Icon(Icons.Default.Refresh, "刷新") }
                }
                val name: (String) -> String = { summaryPersonName(it, user, people.items) }
                when (page) {
                    "organization" -> OrganizationContent(people, canManage, userRepo, { openMember(it) }, { scope.launch { people.load() } })
                    "member" -> {
                        val member = people.items.firstOrNull { it.id == memberId }
                        if (member == null || !canManage) Note("成员信息不可用，请返回组织架构重新加载。")
                        else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item {
                                SummaryPanel {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        RemoteAvatar(member.id, member.displayName, 56.dp, 23.sp, userRepo)
                                        Column(Modifier.weight(1f)) {
                                            Text(name(member.id), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                            Text(member.organizationName ?: "未提供所属组织", color = Ink.copy(alpha = .75f))
                                        }
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    InfoLine("部门", member.departmentName ?: "未提供")
                                    InfoLine("职位", member.jobTitle?.takeIf { it.isNotBlank() } ?: "未设置")
                                    InfoLine("职级", "暂未提供")
                                    InfoLine("电话", "暂未提供")
                                }
                            }
                            taskCollection(
                                managedTasks, responsibleTasks(managedTasks.items, member.id), "负责的任务", name, userRepo,
                                { taskId = it }, { scope.launch { managedTasks.load() } }, filtered = true
                            )
                            item {
                                SummaryPanel {
                                    PanelHeading("最近日志")
                                    memberJournals(journals.items, member.id).forEach { journal ->
                                        JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id }
                                    }
                                    CollectionFooter(journals, memberJournals(journals.items, member.id).isEmpty(), "暂无相关日志", true) { scope.launch { journals.load() } }
                                }
                            }
                        }
                    }
                    "journals" -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        item { Note(if (canManage) "管理范围内下属的日志" else "我的日志") }
                        items(journals.items, key = { it.id }) { journal ->
                            Surface(color = WarmCard) { JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id } }
                        }
                        item { CollectionFooter(journals, journals.items.isEmpty(), "暂无相关日志") { scope.launch { journals.load() } } }
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(16.dp)) {
                        item {
                            Surface(color = WarmCard, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                                Column(Modifier.padding(12.dp)) {
                                    TabRow(
                                        selectedTabIndex = listOf("mine", "created", "managed").indexOf(taskScope),
                                        containerColor = WarmCard, contentColor = Ink
                                    ) {
                                        listOf("mine" to "我的任务", "created" to "我创建的", "managed" to "下属任务").forEach { (value, label) ->
                                            Tab(selected = taskScope == value, onClick = { taskScope = value }, selectedContentColor = ThemeButton,
                                                unselectedContentColor = Ink.copy(alpha = .75f), text = { Text(label, maxLines = 2) })
                                        }
                                    }
                                    if (taskScope == "managed" && canManage) Note("管理范围内下属创建或执行的任务")
                                }
                            }
                        }
                        if (!canRead || (taskScope == "managed" && !canManage)) {
                            item { Surface(color = WarmCard) { Note(if (!canRead) "当前账号没有业务任务查看权限" else "当前账号没有下属任务查看权限") } }
                        } else taskCollection(selectedTasks, selectedTasks.items, null, name, userRepo, { taskId = it }, { scope.launch { selectedTasks.load() } })
                        item {
                            Spacer(Modifier.height(16.dp))
                            SummaryPanel {
                                PanelHeading("直属下属", if (canManage) ({ page = "organization" }) else null)
                                if (!canManage) Note("当前账号没有下属查看权限")
                                else {
                                    val reports = directReports(people.items)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        items(reports, key = { it.id }) { person ->
                                            Column(Modifier.width(68.dp).clip(RoundedCornerShape(8.dp)).clickable { openMember(person) }.padding(vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                RemoteAvatar(person.id, name(person.id), 44.dp, 18.sp, userRepo)
                                                Text(name(person.id), Modifier.padding(top = 6.dp), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                    CollectionFooter(people, reports.isEmpty(), "暂无直属下属", filtered = true) { scope.launch { people.load() } }
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                        item {
                            SummaryPanel {
                                PanelHeading("最近日志动态", if (canRead) ({ page = "journals" }) else null)
                                if (!canRead) Note("当前账号没有日志查看权限")
                                else {
                                    journals.items.take(5).forEach { journal ->
                                        JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id }
                                    }
                                    if (journals.items.isNotEmpty()) Note(if (canManage) "管理范围内下属的最近日志" else "我的最近日志")
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

/** Tasks are individual lazy items, but visually form one panel with the tabs/footer. */
private fun LazyListScope.taskCollection(
    pager: SummaryPager<TaskDto>, tasks: List<TaskDto>, title: String?, name: (String) -> String,
    userRepo: UserRepository, onTask: (String) -> Unit, onMore: () -> Unit, filtered: Boolean = false
) {
    if (title != null) item { Surface(color = WarmCard, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) { Box(Modifier.padding(horizontal = 16.dp)) { PanelHeading(title) } } }
    items(tasks, key = { "task:${it.id}" }) { task ->
        Surface(color = WarmCard) { SummaryTaskRow(task, name, userRepo) { onTask(task.id) } }
    }
    item {
        Surface(color = WarmCard, shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                CollectionFooter(pager, tasks.isEmpty(), "暂无相关任务", filtered, onMore = onMore)
            }
        }
    }
}

@Composable
private fun SummaryPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = WarmCard, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun PanelHeading(title: String, onMore: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f), fontWeight = FontWeight.Bold, fontSize = 17.sp)
        if (onMore != null) TextButton(onMore, contentPadding = PaddingValues(start = 12.dp)) {
            Text("更多", color = Ink)
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

@Composable
private fun <T> CollectionFooter(
    pager: SummaryPager<T>, empty: Boolean, emptyText: String, filtered: Boolean = false,
    showMore: Boolean = true, onMore: () -> Unit
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
            if (empty) Note(if (filtered && pager.nextCursor != null) "已加载范围暂无相关内容，可继续加载" else emptyText)
            if (showMore && pager.nextCursor != null) TextButton(onMore) { Text("加载更多", color = ThemeButton) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryTaskRow(task: TaskDto, name: (String) -> String, userRepo: UserRepository, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(task.title, Modifier.weight(1f), fontSize = 16.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val states = task.authorizedAssignments.map { it.executionStatus }.distinct()
            val completed = states.isNotEmpty() && states.all { it == "COMPLETED" }
            val label = summaryTaskStatus(task)
            val bg = when { completed -> Mint; task.reviewStatus == "REJECTED" -> Peach; else -> CreamDeep }
            Text(label, Modifier.widthIn(max = 96.dp).clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp, color = if (completed) MintDeep else Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            task.tags.forEach { tag ->
                Text(tag.name, Modifier.widthIn(max = 180.dp).clip(RoundedCornerShape(8.dp)).background(tagHexColor(tag.colorHex).copy(alpha = .35f)).padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val priority = priorityMeta(task.priorityLevel)
            Text("★ ${priority.second}", Modifier.clip(RoundedCornerShape(8.dp)).background(CreamDeep).padding(horizontal = 8.dp, vertical = 3.dp), fontSize = 12.sp, color = Ink)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Event, null, Modifier.size(15.dp), tint = Ink.copy(alpha = .75f))
            Text("截止 ${task.endDate}" + if (task.timeKind == "TIMED") " ${task.endTime.orEmpty()}" else "", Modifier.weight(1f), fontSize = 12.sp, color = Ink.copy(alpha = .75f))
            val executors = executorIds(task)
            LazyRow(Modifier.widthIn(max = 112.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(executors, key = { it }) { id -> RemoteAvatar(id, name(id), 28.dp, 12.sp, userRepo) }
            }
            if (executors.isEmpty()) Text("暂无可见执行人", fontSize = 11.sp, color = Ink.copy(alpha = .75f))
        }
        HorizontalDivider(color = HairLine.copy(alpha = .5f))
    }
}

@Composable
private fun JournalRow(journal: JournalDto, name: String, timezone: String?, userRepo: UserRepository, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 12.dp, horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RemoteAvatar(journal.authorUserId, name, 36.dp, 15.sp, userRepo)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(name, Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(businessDateTimeLabel(journal.publishedAt, timezone).ifBlank { journal.workDate }, fontSize = 11.sp, color = Ink.copy(alpha = .75f))
            }
            Text(journalSummary(journal), maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = Ink.copy(alpha = .8f))
            HorizontalDivider(Modifier.padding(top = 6.dp), color = HairLine.copy(alpha = .5f))
        }
    }
}

@Composable
private fun OrganizationContent(people: SummaryPager<RecipientDto>, canManage: Boolean, userRepo: UserRepository, onMember: (RecipientDto) -> Unit, onMore: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(listOf<String>()) }
    val groups = remember(people.items) { recipientOrganizationTree(people.items) }
    LazyColumn(contentPadding = PaddingValues(16.dp)) {
        item { Note("仅展示权限范围内的部门、团队及成员；逐人的汇报层级暂不可用。") }
        if (!canManage) item { Note("当前账号没有组织架构查看权限") }
        else {
            fun LazyListScope.group(node: RecipientOrganizationGroup, depth: Int) {
                item(key = node.key) {
                    Surface(color = WarmCard) {
                        Row(Modifier.fillMaxWidth().clickable {
                            expanded = if (node.key in expanded) expanded - node.key else expanded + node.key
                        }.padding(start = (12 + depth * 16).dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (node.key in expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight, if (node.key in expanded) "收起" else "展开")
                            Text(node.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (node.key in expanded) {
                    items(node.members, key = { "member:${it.id}" }) { person ->
                        Surface(color = WarmCard) {
                            Row(Modifier.fillMaxWidth().clickable { onMember(person) }.padding(start = (28 + depth * 16).dp, end = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                RemoteAvatar(person.id, person.displayName, 36.dp, 15.sp, userRepo)
                                Column(Modifier.weight(1f)) {
                                    Text(person.displayName.ifBlank { "成员 ${person.id.takeLast(4)}" }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
                                    person.jobTitle?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 12.sp, color = Ink.copy(alpha = .75f)) }
                                }
                                Icon(Icons.Default.ChevronRight, "成员详情", Modifier.size(18.dp))
                            }
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
