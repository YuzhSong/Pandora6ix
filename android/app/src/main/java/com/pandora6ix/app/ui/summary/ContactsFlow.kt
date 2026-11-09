package com.pandora6ix.app.ui.summary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.data.*
import com.pandora6ix.app.ui.calendar.DateWheelDialog
import com.pandora6ix.app.ui.components.*
import com.pandora6ix.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 通讯录：承接原任务汇总里的直属下属、组织架构、成员详情与下属日志；
 * 下属日志按工作日期筛选（服务端 from/to），切换筛选重新分页。
 */
@Composable
fun ContactsFlow(
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
    val people = remember(user.id) {
        SummaryPager<RecipientDto>({ it.id }) { cursor ->
            taskRepo.recipientCandidatePage(cursor = cursor).let { SummaryPage(it.items, it.nextCursor) }
        }
    }
    val managedTasks = remember(user.id) {
        SummaryPager<TaskDto>({ it.id }) { cursor ->
            calendarRepo.tasks(scope = "managed", cursor = cursor, limit = 20).let { SummaryPage(it.items, it.nextCursor) }
        }
    }
    // 成员详情的“最近日志”用不带日期的下属日志；日志列表页按所选日期另建分页器
    val recentJournals = remember(user.id) { journalPager(journalRepo, null) }
    var range by rememberSaveable(user.id) { mutableStateOf(JournalRange.ALL) }
    var pickedDay by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var pickingDay by rememberSaveable(user.id) { mutableStateOf(false) }
    val today = businessTodayKey(user.businessTimezone)
    val bounds = journalRangeBounds(range, today, pickedDay)
    val filteredJournals = remember(user.id, bounds) { if (bounds == null) recentJournals else journalPager(journalRepo, bounds) }

    var page by rememberSaveable(user.id) { mutableStateOf("contacts") }
    var memberId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var memberParent by rememberSaveable(user.id) { mutableStateOf("contacts") }
    var listParent by rememberSaveable(user.id) { mutableStateOf("contacts") }
    var listMemberId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var taskId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var journalId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var memberRefresh by remember(user.id) { mutableIntStateOf(0) }
    val uiStates = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val name: (String) -> String = { summaryPersonName(it, user, people.items) }

    LaunchedEffect(canManage) { if (canManage) people.ensureLoaded() }
    LaunchedEffect(page, canManage, filteredJournals) {
        if (!canManage) return@LaunchedEffect
        if (page == "member" || page == "tasks") managedTasks.ensureLoaded()
        if (page == "member") recentJournals.ensureLoaded()
        if (page == "journals") filteredJournals.ensureLoaded()
    }

    fun back() {
        when {
            taskId != null -> taskId = null
            journalId != null -> journalId = null
            page == "tasks" || page == "journals" -> page = listParent
            page == "member" -> { page = memberParent; memberId = null }
            page != "contacts" -> page = "contacts"
            else -> onClose()
        }
    }
    fun openMember(person: RecipientDto) {
        memberParent = page
        memberId = person.id
        page = "member"
    }
    fun openList(target: String, personId: String?) {
        listParent = page
        listMemberId = personId
        page = target
    }
    BackHandler { back() }
    val stateKey = when (page) {
        "member" -> "member:$memberId"
        "tasks" -> "tasks:$listMemberId"
        "journals" -> "journals:$listMemberId"
        else -> page
    }
    when {
        taskId != null -> taskDetail(taskId!!, { taskId = null }, { scope.launch { managedTasks.load(refresh = true) } })
        journalId != null -> journalDetail(journalId!!) { journalId = null }
        else -> uiStates.SaveableStateProvider(stateKey) {
            Column(Modifier.fillMaxSize()) {
                val title = when (page) {
                    "organization" -> "组织架构"; "member" -> "成员详情"
                    "tasks" -> "负责的任务"; "journals" -> "下属日志"; else -> "通讯录"
                }
                PandoraTopBar(title, onBack = { back() }) {
                    IconButton({
                        if (canManage) when (page) {
                            "organization", "contacts" -> scope.launch { people.load(refresh = true) }
                            "tasks" -> scope.launch { managedTasks.load(refresh = true) }
                            "journals" -> scope.launch { filteredJournals.load(refresh = true) }
                            "member" -> {
                                memberRefresh++
                                scope.launch { managedTasks.load(refresh = true) }
                                scope.launch { recentJournals.load(refresh = true) }
                            }
                        }
                    }) { Icon(Icons.Default.Refresh, "刷新", tint = PandoraTheme.colors.textSecondary) }
                }
                if (!canManage) Box(Modifier.padding(PandoraSpacing.space16)) {
                    SummaryPanel { Note("当前账号没有下属查看权限") }
                } else when (page) {
                    "organization" -> OrganizationContent(people, canManage, userRepo, { openMember(it) }, { scope.launch { people.load() } })
                    "tasks" -> SummaryPanel(Modifier.weight(1f).padding(PandoraSpacing.space16)) {
                        val records = listMemberId?.let { responsibleTasks(managedTasks.items, it) } ?: managedTasks.items
                        PanelHeading(listMemberId?.let { "${name(it)} · 负责的任务" } ?: "下属任务")
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                            items(records, key = { it.id }) { task -> SummaryTaskRow(task, name, userRepo) { taskId = task.id } }
                            item { CollectionFooter(managedTasks, records.isEmpty(), "暂无相关任务", filtered = listMemberId != null) { scope.launch { managedTasks.load() } } }
                        }
                    }
                    "journals" -> SummaryPanel(Modifier.weight(1f).padding(PandoraSpacing.space16)) {
                        val records = listMemberId?.let { memberJournals(filteredJournals.items, it) } ?: filteredJournals.items
                        PanelHeading(listMemberId?.let { "${name(it)} · 日志" } ?: "下属日志")
                        JournalRangeBar(range, pickedDay, bounds, { range = it }, { pickingDay = true })
                        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                            items(records, key = { it.id }) { journal -> JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id } }
                            item {
                                CollectionFooter(filteredJournals, records.isEmpty(), if (bounds == null) "暂无相关日志" else "所选日期范围内暂无日志",
                                    filtered = listMemberId != null) { scope.launch { filteredJournals.load() } }
                            }
                        }
                    }
                    "member" -> {
                        val member = people.items.firstOrNull { it.id == memberId }
                        if (member == null) Box(Modifier.padding(PandoraSpacing.space16)) { Note("成员信息不可用，请返回通讯录重新加载。") }
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
                                    MemberPhone(member.id, userRepo, memberRefresh)
                                }
                            }
                            item {
                                SummaryPanel {
                                    PanelHeading("负责的任务")
                                    TaskPreview(managedTasks, responsibleTasks(managedTasks.items, member.id), name, userRepo,
                                        { taskId = it }, { openList("tasks", member.id) }, { scope.launch { managedTasks.load() } }, filtered = true)
                                }
                            }
                            item {
                                SummaryPanel {
                                    PanelHeading("最近日志", { openList("journals", member.id) })
                                    val records = memberJournals(recentJournals.items, member.id)
                                    records.take(PreviewLimit).forEach { journal ->
                                        JournalRow(journal, name(journal.authorUserId), user.businessTimezone, userRepo) { journalId = journal.id }
                                    }
                                    CollectionFooter(recentJournals, records.isEmpty(), "暂无相关日志", filtered = true, showMore = false,
                                        partialEmptyText = "已加载范围暂无相关日志，可查看全部") { scope.launch { recentJournals.load() } }
                                }
                            }
                        }
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
                        item {
                            SummaryPanel {
                                EntryRow(Icons.Default.AccountTree, "组织架构", "按部门查看全部可见成员") { page = "organization" }
                                EntryRow(Icons.Default.Description, "下属日志", "按日期筛选下属提交的日志") { openList("journals", null) }
                            }
                        }
                        item {
                            SummaryPanel {
                                PanelHeading("直属下属")
                                val reports = directReports(people.items)
                                reports.forEach { person -> PersonRow(person, name(person.id), userRepo) { openMember(person) } }
                                CollectionFooter(people, reports.isEmpty(), "暂无直属下属", filtered = true,
                                    partialEmptyText = "已加载范围暂无直属下属，可继续加载") { scope.launch { people.load() } }
                            }
                        }
                    }
                }
            }
        }
    }
    if (pickingDay) DateWheelDialog(
        initial = parseIsoDate(pickedDay ?: today) ?: com.pandora6ix.app.mock.todayDate(),
        onDismiss = { pickingDay = false },
        onConfirm = { d ->
            pickingDay = false
            pickedDay = "%04d-%02d-%02d".format(d.year, d.month, d.day)
            range = JournalRange.DAY
        }
    )
}

private fun journalPager(journalRepo: JournalRepository, bounds: Pair<String, String>?) =
    SummaryPager<JournalDto>({ it.id }) { cursor ->
        journalRepo.journals(from = bounds?.first, to = bounds?.second, scope = "managed", cursor = cursor, limit = 20)
            .let { SummaryPage(it.items, it.nextCursor) }
    }

@Composable
private fun JournalRangeBar(
    range: JournalRange,
    pickedDay: String?,
    bounds: Pair<String, String>?,
    onRange: (JournalRange) -> Unit,
    onPickDay: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
            JournalRange.entries.forEach { option ->
                val label = if (option == JournalRange.DAY && range == JournalRange.DAY && pickedDay != null) pickedDay else option.label
                // 指定日期每次点按都重新选择日期
                PandoraChoiceChip(label, range == option, onClick = { if (option == JournalRange.DAY) onPickDay() else onRange(option) })
            }
        }
        Text(
            when {
                bounds == null -> "显示全部日期的下属日志"
                bounds.first == bounds.second -> "工作日期：${bounds.first}"
                else -> "工作日期：${bounds.first} 至 ${bounds.second}"
            },
            style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary
        )
    }
}

@Composable
private fun EntryRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().summaryEntry(onClick).padding(PandoraSpacing.space12), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
        Icon(icon, null, tint = PandoraTheme.colors.primary)
        Column(Modifier.weight(1f)) {
            Text(title, style = PandoraTypography.labelLarge)
            Text(subtitle, style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
        }
        Icon(Icons.Default.ChevronRight, null, tint = PandoraTheme.colors.textSecondary)
    }
}

@Composable
private fun PersonRow(person: RecipientDto, shownName: String, userRepo: UserRepository, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().summaryEntry(onClick).padding(PandoraSpacing.space12), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
        RemoteAvatar(person.id, shownName, PandoraDimensions.avatarList, 16.sp, userRepo)
        Column(Modifier.weight(1f)) {
            Text(shownName, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            val detail = listOfNotNull(person.jobTitle?.takeIf { it.isNotBlank() }, person.departmentName?.takeIf { it.isNotBlank() }).joinToString(" · ")
            if (detail.isNotEmpty()) Text(detail, style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Default.ChevronRight, "查看$shownName", tint = PandoraTheme.colors.textSecondary)
    }
}
