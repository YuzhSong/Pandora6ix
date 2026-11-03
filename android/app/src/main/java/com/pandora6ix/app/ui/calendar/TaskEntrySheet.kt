package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.ui.components.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pandora6ix.app.data.*
import com.pandora6ix.app.mock.*
import com.pandora6ix.app.ui.theme.*
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private fun DemoDate.key() = "$year-$month-$day"

/** 星级文字含义：1 很低 · 2 一般 · 3 普通 · 4 重要 · 5 紧急 */
private fun priorityLabel(level: Int): String = listOf("很低", "一般", "普通", "重要", "紧急")[(level - 1).coerceIn(0, 4)]
private fun String.date() = split("-").map(String::toInt).let { DemoDate(it[0], it[1], it[2]) }

/** 服务端标签颜色（#RRGGBB）→ Compose Color；非法值回退墨色 */
internal fun tagHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(WarmColors.neutralContainer)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TaskEntrySheet(initialDate: DemoDate, canCreatePersonal: Boolean, canCreateDelegated: Boolean, availableTags: List<TagDto>, taskRepo: TaskRepository, businessTimezone: String? = null, currentUser: UserDto? = null, onDismiss: () -> Unit, onCreated: (TaskDto) -> Unit, editingTask: TaskDto? = null, newTaskDraft: TaskDraftDto? = null, draftKey: String = "task_draft.json") {
    val context = LocalContext.current
    val density = LocalDensity.current
    val candidateGap = with(density) { 8.dp.roundToPx() }
    val candidateInset = with(density) { 16.dp.roundToPx() }
    val candidatePosition = remember(candidateGap, candidateInset) { RecipientPopupPositionProvider(candidateGap, candidateInset) }
    var queryWidth by remember { mutableIntStateOf(0) }
    var queryTop by remember { mutableIntStateOf(0) }
    val queryVisibility = remember { BringIntoViewRequester() }
    // 草稿恢复：上次未提交的任务内容（缓存目录）
    val savedDraft = remember {
        if (editingTask != null) TaskDraftDto(editingTask.kind, editingTask.title, editingTask.description.orEmpty(),
            editingTask.timeKind == "ALL_DAY", editingTask.startDate, editingTask.endDate,
            parseHhMm(editingTask.startTime) ?: 0, parseHhMm(editingTask.endTime) ?: 60,
            editingTask.priorityLevel, editingTask.tags.sortedBy { if (it.id == editingTask.primaryTagId) 0 else 1 }.map { it.name }, editingTask.authorizedAssignments.map { it.recipientUserId }, companyWide = editingTask.companyWide)
        else
        AppCache.readDraft(context, draftKey)?.let { s -> runCatching { AppCache.json.decodeFromString<TaskDraftDto>(s) }.getOrNull() } ?: newTaskDraft
    }
    var title by rememberSaveable { mutableStateOf(savedDraft?.title ?: "") }
    var note by rememberSaveable { mutableStateOf(savedDraft?.note ?: "") }
    var startKey by rememberSaveable { mutableStateOf(savedDraft?.startKey ?: initialDate.key()) }
    var endKey by rememberSaveable { mutableStateOf(savedDraft?.endKey ?: initialDate.key()) }
    var allDay by rememberSaveable { mutableStateOf(savedDraft?.allDay ?: false) }
    var repeating by rememberSaveable { mutableStateOf(savedDraft?.recurrence != null) }
    var repeatFrequency by rememberSaveable { mutableStateOf(savedDraft?.recurrence?.frequency ?: "DAILY") }
    var repeatInterval by rememberSaveable { mutableStateOf((savedDraft?.recurrence?.interval ?: 1).toString()) }
    var customRepeat by rememberSaveable { mutableStateOf(savedDraft?.customRepeat == true || (savedDraft?.recurrence?.interval ?: 1) > 1) }
    var repeatWeekdays by rememberSaveable { mutableStateOf(savedDraft?.recurrence?.weekdays ?: listOf(1)) }
    val repeatRule = if (repeating) RecurrenceRule(repeatFrequency, if (customRepeat) repeatInterval.toIntOrNull() ?: 0 else 1,
        if (repeatFrequency == "WEEKLY") repeatWeekdays else emptyList()) else null
    val repeatPreview = repeatRule?.let { rule -> runCatching { recurrenceDates(startKey.date(), endKey.date(), rule) } }
    // 默认时间取公司业务时区的当前时刻，避免与现实时间不一致、也避免默认落在过去
    val nowMinute = remember(businessTimezone) { businessNowMinute(businessTimezone) }
    var startMinute by rememberSaveable { mutableIntStateOf(savedDraft?.startMinute ?: nowMinute.coerceAtMost(23 * 60 + 58)) }
    var endMinute by rememberSaveable { mutableIntStateOf(savedDraft?.endMinute ?: (nowMinute + 60).coerceAtMost(23 * 60 + 59)) }
    var priority by rememberSaveable { mutableIntStateOf(savedDraft?.priority ?: 3) }
    // 接收人规则：无派发权限的员工锁定为本人（灰色不可改）；有派发权限时可多选（本人 + 管理范围内成员）
    val canDelegate = canCreateDelegated
    val selfRecipient = remember(currentUser) {
        currentUser?.let { u ->
            RecipientDto(id = u.id, displayName = u.displayName.ifBlank { u.name.ifBlank { u.loginName } }, jobTitle = u.jobTitle, orgUnitId = u.orgUnitId)
        }
    }
    var selectedRecipientIds by remember {
        mutableStateOf(if (canDelegate) savedDraft?.recipientIds ?: listOf() else listOfNotNull(selfRecipient?.id))
    }
    // 任务类型由接收人推导，不再由按钮区分：仅本人且有个人任务能力 → 个人任务，否则派发任务
    var companyWide by rememberSaveable { mutableStateOf(savedDraft?.companyWide == true) }
    val kind = when {
        selectedRecipientIds.size == 1 && selectedRecipientIds.first() == selfRecipient?.id && canCreatePersonal -> "PERSONAL"
        else -> "DELEGATED"
    }
    var candidates by remember { mutableStateOf<List<RecipientDto>>(emptyList()) }
    val candidateNames = remember { mutableStateMapOf<String, String>().apply {
        editingTask?.authorizedAssignments?.forEach { assignment ->
            assignment.recipientName?.takeIf { it.isNotBlank() }?.let { put(assignment.recipientUserId, it) }
        }
    } }
    var namesLoading by remember { mutableStateOf(false) }
    var namesFailed by remember { mutableStateOf(false) }
    var namesRetry by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedRecipientIds, namesRetry) {
        if (!canDelegate || selectedRecipientIds.all { !candidateNames[it].isNullOrBlank() }) return@LaunchedEffect
        namesLoading = true; namesFailed = false
        try {
            var cursor: String? = null
            val seen = mutableSetOf<String>()
            do {
                val result = taskRepo.recipientCandidatePage(cursor = cursor, limit = 100)
                candidateNames.putAll(result.items.filter { it.displayName.isNotBlank() }.associate { it.id to it.displayName })
                cursor = result.nextCursor
                check(cursor == null || seen.add(cursor))
            } while (cursor != null && selectedRecipientIds.any { candidateNames[it].isNullOrBlank() })
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { namesFailed = true }
        finally { namesLoading = false }
    }
    // 本人姓名来自登录资料（displayName → name → loginName），无需等候选接口加载即可显示真实姓名
    selfRecipient?.takeIf { it.displayName.isNotBlank() }?.let { candidateNames.putIfAbsent(it.id, it.displayName) }
    var recipientQuery by remember { mutableStateOf("") }
    var candidateOpen by remember { mutableStateOf(false) }
    val candidateGroups = remember(candidates) { recipientOrganizationTree(candidates) }
    var collapsedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var candidateLoading by remember { mutableStateOf(false) }
    var candidateError by remember { mutableStateOf(false) }
    var candidateRetry by remember { mutableIntStateOf(0) }
    var submitting by remember { mutableStateOf(false) }
    var createdTask by remember { mutableStateOf<TaskDto?>(null) }
    val idempotencyKey = remember { UUID.randomUUID().toString() }
    val editKeys = remember { mutableMapOf<String, String>() }
    LaunchedEffect(candidateOpen, selectedRecipientIds) {
        if (candidateOpen) {
            withFrameNanos { }
            queryVisibility.bringIntoView()
        }
    }
    // 候选范围由后端授权；组织 ID 决定层级和折叠状态，名称只用于显示。
    LaunchedEffect(candidateOpen, recipientQuery, kind, candidateRetry) {
        if (canDelegate && candidateOpen) {
            val keyword = recipientQuery.substringAfter("@").trim()
            candidates = emptyList()
            candidateLoading = true
            candidateError = false
            try {
                val result = taskRepo.recipientCandidates(keyword.ifBlank { null })
                // 候选人固定包含本人（排最前），其余为后端按管理范围授权的成员
                val merged = if (selfRecipient != null && result.items.none { it.id == selfRecipient.id }) listOf(selfRecipient) + result.items else result.items
                candidates = merged
                candidateNames.putAll(merged.associate { it.id to it.displayName })
                collapsedGroups = if (keyword.isBlank()) {
                    recipientOrganizationTree(result.items).flatMap { it.groupKeys }.toSet()
                } else emptySet()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                candidateError = true
            } finally {
                candidateLoading = false
            }
        }
    }
    var tags by rememberSaveable { mutableStateOf(savedDraft?.tags ?: listOf<String>()) }
    // 草稿实时落盘（缓存目录）：提交成功或明确放弃时清除
    LaunchedEffect(kind, title, note, allDay, startKey, endKey, startMinute, endMinute, priority, tags, selectedRecipientIds, repeatRule, customRepeat, companyWide) {
        if (editingTask != null) return@LaunchedEffect
        val draft = TaskDraftDto(kind, title, note, allDay, startKey, endKey, startMinute, endMinute, priority, tags, selectedRecipientIds, repeatRule, customRepeat, companyWide)
        AppCache.writeDraft(context, draftKey, AppCache.json.encodeToString(TaskDraftDto.serializer(), draft))
    }
    var showTagPicker by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    // 时间/日期选择浮层目标：start | end | null
    var timePickTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var datePickTarget by rememberSaveable { mutableStateOf<String?>(null) }
    // 进入弹层时的状态快照：退出时仅在实际改动（相对进入时）才提示保存，草稿恢复的未修改内容不算改动
    val initialState = remember { listOf(kind, title, note, startKey, endKey, allDay, startMinute, endMinute, priority, tags, selectedRecipientIds, repeatRule, customRepeat, companyWide) }
    val dirty = listOf(kind, title, note, startKey, endKey, allDay, startMinute, endMinute, priority, tags, selectedRecipientIds, repeatRule, customRepeat, companyWide) != initialState
    val latestDirty by rememberUpdatedState(dirty)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { value ->
        if (value == SheetValue.Hidden && latestDirty) { confirmExit = true; false } else true
    })
    val requestClose = { if (dirty) confirmExit = true else onDismiss() }
    val sheetScope = rememberCoroutineScope()
    val fieldColors = pandoraTextFieldColors()
    // 日期/时间选择按钮：Outline 变体（透明底、中性 1dp 边框、12 角）
    val buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = PandoraTheme.colors.textPrimary)
    val buttonBorder = BorderStroke(PandoraDimensions.borderWidth, PandoraTheme.colors.inputBorder)
    var editVersions by remember { mutableStateOf<List<ScopeVersionDto>?>(null) }
    var editLoadError by remember { mutableStateOf(false) }
    var scopeOpen by remember { mutableStateOf(false) }
    LaunchedEffect(editingTask?.id) {
        if (editingTask != null) {
            try { editVersions = taskScopeVersions(taskRepo.scopeSnapshot(editingTask.id).items, null) }
            catch (e: Exception) { editLoadError = true }
        }
    }
    fun pickDate(target: String) { datePickTarget = target }
    fun pickTime(target: String) { timePickTarget = target }
    fun nameOf(id: String): String = candidateNames[id]?.ifBlank { null }
        ?: selfRecipient?.takeIf { it.id == id }?.displayName?.ifBlank { null }
        ?: if (namesLoading) "加载姓名…" else "姓名暂不可用"
    /** 提交创建：POST /tasks（非幂等，携带 Idempotency-Key；失败重试复用同一键） */
    fun submit(editScope: String? = null) {
        error = when {
            title.isBlank() -> "请填写任务名称"
            selectedRecipientIds.isEmpty() -> "请至少选择一位接收人"
            editingTask != null && editVersions == null -> if (editLoadError) "加载失败，请关闭后重试" else "正在加载任务"
            repeating && repeatPreview?.isFailure == true -> repeatPreview.exceptionOrNull()?.message ?: "重复规则无效"
            repeating && !allDay && endMinute <= startMinute -> "每次的结束时间须晚于开始时间（同一天）"
            repeating && repeatPreview?.getOrNull()?.any { !deadlineNotPast(it.key(), if (allDay) null else endMinute, allDay, businessTimezone) } == true -> "重复安排中存在已过截止时间的日期，请调整开始日期"
            !validTaskInterval(startKey.date(), endKey.date(), if (allDay) null else startMinute, if (allDay) null else endMinute) -> "结束日期和时间必须晚于开始时间"
            // 截止时间不能早于公司业务时区的当前时刻（服务端也按业务时区判定）
            editingTask == null && !deadlineNotPast(endKey, if (allDay) null else endMinute, allDay, businessTimezone) ->
                "截止时间不能早于当前时间（公司时间 ${businessNowLabel(businessTimezone)}）"
            else -> null
        }
        if (error != null) return
        if (editingTask?.seriesId != null && editScope == null) { scopeOpen = true; return }
        submitting = true; error = null
        sheetScope.launch {
            try {
                val tagIds = tags.mapNotNull { name -> availableTags.firstOrNull { it.name == name }?.id }
                if (editingTask != null) {
                    val command = TaskPatchDto(editingTask.version, title.trim(), note.trim().ifBlank { null }, priority,
                        if (allDay) "ALL_DAY" else "TIMED", startKey.date().let { "%04d-%02d-%02d".format(it.year,it.month,it.day) },
                        endKey.date().let { "%04d-%02d-%02d".format(it.year,it.month,it.day) },
                        if (allDay) null else minuteLabel(startMinute), if (allDay) null else minuteLabel(endMinute), tagIds, tagIds.firstOrNull(), selectedRecipientIds, kind, companyWide && kind == "DELEGATED")
                    val request = ScopedEditDto(editScope ?: "SINGLE", command, editVersions!!)
                    val updated = taskRepo.scopedEdit(editingTask.id, request, editKeys.getOrPut(request.toString()) { UUID.randomUUID().toString() })
                    onCreated(updated); onDismiss()
                    return@launch
                }
                createdTask = taskRepo.create(
                    TaskCreateDto(
                        kind = kind,
                        title = title.trim(),
                        description = note.trim().ifBlank { null },
                        priorityLevel = priority,
                        timeKind = if (allDay) "ALL_DAY" else "TIMED",
                        startDate = "%04d-%02d-%02d".format(startKey.date().year, startKey.date().month, startKey.date().day),
                        endDate = "%04d-%02d-%02d".format(endKey.date().year, endKey.date().month, endKey.date().day),
                        startTime = if (allDay) null else minuteLabel(startMinute),
                        endTime = if (allDay) null else minuteLabel(endMinute),
                        recipientIds = if (kind == "DELEGATED") selectedRecipientIds else null,
                        tagIds = tagIds.ifEmpty { null },
                        primaryTagId = tagIds.firstOrNull(),
                        recurrence = repeatRule,
                        companyWide = companyWide && kind == "DELEGATED"
                    ),
                    idempotencyKey
                )
                AppCache.deleteDraft(context, draftKey)
            } catch (e: ApiException) {
                error = e.friendlyMessage()
            } catch (e: Exception) {
                error = "网络异常，请重试"
            } finally {
                submitting = false
            }
        }
    }
    PandoraBottomSheet(onDismissRequest = requestClose, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).padding(horizontal = PandoraSpacing.space20)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (editingTask == null) "添加任务" else "编辑任务", style = PandoraTypography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(requestClose) { Icon(Icons.Default.Close, "关闭任务填写") }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                if (currentUser?.roles?.contains("FOUNDER") == true && kind == "DELEGATED") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("全公司可见", Modifier.weight(1f))
                        Switch(companyWide, { companyWide = it }, colors = com.pandora6ix.app.ui.theme.pandoraSwitchColors())
                    }
                }
                PandoraTextField(title, { title = it; error = null }, Modifier.fillMaxWidth(), label = { Text("任务名称（必填）") }, placeholder = { Text("我计划做…") }, singleLine = true, colors = fieldColors)
                PandoraTextField(note, { note = it }, Modifier.fillMaxWidth().heightIn(min = 112.dp), label = { Text("备注 / 简介（选填）") }, colors = fieldColors, minLines = 3)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("全天任务", Modifier.weight(1f), style = PandoraTypography.labelLarge)
                    Switch(allDay, { allDay = it; error = null }, colors = com.pandora6ix.app.ui.theme.pandoraSwitchColors())
                }
                if (editingTask == null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("重复安排", Modifier.weight(1f), style = PandoraTypography.labelLarge)
                    Switch(repeating, { repeating = it; error = null }, colors = com.pandora6ix.app.ui.theme.pandoraSwitchColors())
                }
                if (repeating) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        listOf("DAILY" to "每天", "WEEKLY" to "每周", "MONTHLY" to "每月").forEach { (value, label) ->
                            FilterChip(selected = !customRepeat && repeatFrequency == value, onClick = { customRepeat = false; repeatFrequency = value; error = null }, label = { Text(label) })
                        }
                        FilterChip(selected = customRepeat, onClick = { customRepeat = true; error = null }, label = { Text("自定义") })
                    }
                    if (customRepeat) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        listOf("DAILY" to "天", "WEEKLY" to "周", "MONTHLY" to "月").forEach { (value, label) ->
                            FilterChip(selected = repeatFrequency == value, onClick = { repeatFrequency = value; error = null }, label = { Text(label) })
                        }
                    }
                    PandoraTextField(repeatInterval, { if (it.length <= 3 && it.all(Char::isDigit)) repeatInterval = it },
                        Modifier.fillMaxWidth(), singleLine = true, colors = fieldColors,
                        label = { Text("自定义间隔：每 N ${when (repeatFrequency) { "DAILY" -> "天"; "WEEKLY" -> "周"; else -> "个月" }}（1–365）") })
                    }
                    if (repeatFrequency == "WEEKLY") FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
                        (1..7).forEach { day ->
                            FilterChip(selected = day in repeatWeekdays, onClick = { repeatWeekdays = if (day in repeatWeekdays) repeatWeekdays - day else repeatWeekdays + day },
                                label = { Text("周" + listOf("一", "二", "三", "四", "五", "六", "日")[day - 1]) })
                        }
                    }
                }
                Text(if (repeating) "重复生效日期范围（包含结束日）" else "连续时段的开始与结束", Modifier.fillMaxWidth(), style = PandoraTypography.labelLarge)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    OutlinedButton({ pickDate("start") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, shape = PandoraShapes.medium, contentPadding = PaddingValues(PandoraSpacing.space8)) { Text(startKey.date().let { "开始\n${it.year}/${it.month}/${it.day}" }, style = PandoraTypography.bodyMedium) }
                    OutlinedButton({ pickDate("end") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, shape = PandoraShapes.medium, contentPadding = PaddingValues(PandoraSpacing.space8)) { Text(endKey.date().let { "结束\n${it.year}/${it.month}/${it.day}" }, style = PandoraTypography.bodyMedium) }
                }
                if (!allDay) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    OutlinedButton({ pickTime("start") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, shape = PandoraShapes.medium) { Text("${if (repeating) "每次开始" else "开始"} ${minuteLabel(startMinute)}") }
                    OutlinedButton({ pickTime("end") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, shape = PandoraShapes.medium) { Text("${if (repeating) "每次结束" else "结束"} ${minuteLabel(endMinute)}") }
                }
                if (repeating) repeatPreview?.getOrNull()?.let { Text("共 ${it.size} 次", style = PandoraTypography.labelLarge) }
                // 星级文字含义：与审批规则一致——星级越高越优先处理
                Text("重要程度 · $priority 星 · ${priorityLabel(priority)}", style = PandoraTypography.labelLarge)
                Row { (1..5).forEach { level -> IconButton({ priority = level }) { Icon(if (level <= priority) Icons.Default.Star else Icons.Default.StarBorder, "$level 星", tint = if (level <= priority) PandoraTheme.colors.statusColors(priorityTone(priority)).content else PandoraTheme.colors.textTertiary) } } }
                Text("标签", style = PandoraTypography.titleSmall)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
                    // 已选标签：业务原色 + 中性边框，主标签前置并标注；点击移除
                    tags.forEachIndexed { i, name ->
                        PandoraTagChip(name, availableTags.firstOrNull { it.name == name }?.colorHex, Modifier.heightIn(min = PandoraDimensions.chipButtonHeight),
                            primary = i == 0, onClick = { tags = tags - name })
                    }
                    OutlinedButton({ showTagPicker = true }, contentPadding = PaddingValues(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8), border = BorderStroke(1.dp, PandoraTheme.colors.border), colors = ButtonDefaults.outlinedButtonColors(contentColor = PandoraTheme.colors.textPrimary)) { Icon(Icons.Default.Add, "选择标签", Modifier.size(16.dp)); Spacer(Modifier.width(PandoraSpacing.space4)); Text("选择标签", style = PandoraTypography.bodyMedium) }
                }
                run {
                Text("接收人", style = PandoraTypography.titleSmall)
                if (namesFailed) TextButton({ namesRetry++ }) { Text("姓名加载失败，重试", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
                if (selectedRecipientIds.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space2), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    selectedRecipientIds.forEach { id ->
                        if (canDelegate) {
                            FilterChip(selected = true, onClick = { selectedRecipientIds = selectedRecipientIds - id; error = null }, label = { Text(nameOf(id), style = PandoraTypography.bodyMedium) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent, labelColor = PandoraTheme.colors.textPrimary, selectedContainerColor = PandoraTheme.colors.primaryContainer, selectedLabelColor = PandoraTheme.colors.textPrimary))
                        } else {
                            // 无派发权限：接收人锁定为本人，浅蓝底黑字展示且不可更改
                            FilterChip(selected = true, onClick = { }, enabled = false, label = { Text(nameOf(id), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textPrimary) }, colors = FilterChipDefaults.filterChipColors(disabledContainerColor = PandoraTheme.colors.neutralContainer, disabledLabelColor = PandoraTheme.colors.textPrimary))
                        }
                    }
                }
                if (canDelegate) {
                Box(Modifier.bringIntoViewRequester(queryVisibility).onGloballyPositioned { coordinates ->
                    queryWidth = coordinates.size.width
                    queryTop = coordinates.boundsInWindow().top.toInt()
                }) {
                    PandoraTextField(recipientQuery, { recipientQuery = it; candidateOpen = it.contains("@") }, Modifier.fillMaxWidth(), placeholder = { Text("@ 检索接收人，勾选候选") }, singleLine = true, colors = fieldColors)
                    if (candidateOpen && queryWidth > 0) Popup(
                        popupPositionProvider = candidatePosition,
                        onDismissRequest = { candidateOpen = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        val maxCandidateHeight = with(density) { (queryTop - candidateGap - candidateInset).coerceAtLeast(0).toDp() }.coerceAtMost(280.dp)
                        Column(Modifier.width(with(density) { queryWidth.toDp() }).heightIn(max = maxCandidateHeight).clip(PandoraShapes.medium).background(PandoraTheme.colors.primaryContainer).border(1.dp, PandoraTheme.colors.border, PandoraShapes.medium).padding(vertical = PandoraSpacing.space4)) {
                            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                when {
                                    candidateLoading -> Text("正在加载成员…", Modifier.padding(PandoraSpacing.space12), style = PandoraTypography.bodyMedium)
                                    candidateError -> {
                                        Text("成员加载失败，请重试", Modifier.padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8), style = PandoraTypography.bodyMedium)
                                        TextButton({ candidateRetry++ }) { Text("重试", color = PandoraTheme.colors.textPrimary) }
                                    }
                                    candidates.isEmpty() -> Text("无匹配成员", Modifier.fillMaxWidth().padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textTertiary)
                                }
                                candidateGroups.forEach { group ->
                                    key(group.key) {
                                        RecipientGroupRows(
                                            group, collapsedGroups, selectedRecipientIds.toSet(),
                                            onToggleGroup = { groupKey ->
                                                collapsedGroups = if (groupKey in collapsedGroups) collapsedGroups - groupKey else collapsedGroups + groupKey
                                            },
                                            onTogglePerson = { candidate ->
                                                val selected = candidate.id in selectedRecipientIds
                                                selectedRecipientIds = if (selected) selectedRecipientIds - candidate.id else selectedRecipientIds + candidate.id
                                                error = null
                                                if (recipientQuery.isNotEmpty()) recipientQuery = ""
                                            }
                                        )
                                    }
                                }
                            }
                            TextButton({ candidateOpen = false }, Modifier.fillMaxWidth()) { Text("完成", style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textPrimary) }
                        }
                    }
                }
                }
                }
            }
            // 标签已改为可选：无可用标签不再阻断提交
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = PandoraTypography.bodyMedium) }
            PandoraButton(if (editingTask != null) "保存" else "提交审批", { submit() }, Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space12), loading = submitting)
        }
    }
    if (confirmExit) PandoraAlertDialog(onDismissRequest = { confirmExit = false; sheetScope.launch { sheetState.show() } }, title = { Text("放弃未保存的任务？") }, text = { Text("关闭后，本次填写的内容会清空。") }, confirmButton = { PandoraDialogConfirm("放弃", { confirmExit = false; AppCache.deleteDraft(context, draftKey); onDismiss() }, destructive = true) }, dismissButton = { TextButton({ confirmExit = false; sheetScope.launch { sheetState.show() } }) { Text("继续填写", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.textSecondary) } })
    timePickTarget?.let { target ->
        TimeWheelDialog(
            initialMinute = if (target == "start") startMinute else endMinute,
            onDismiss = { timePickTarget = null },
            onConfirm = { value ->
                if (target == "start") startMinute = value else endMinute = value
                error = null
                timePickTarget = null
            }
        )
    }
    datePickTarget?.let { target ->
        DateWheelDialog(
            initial = (if (target == "start") startKey else endKey).date(),
            onDismiss = { datePickTarget = null },
            onConfirm = { d ->
                if (target == "start") startKey = d.key() else endKey = d.key()
                error = null
                datePickTarget = null
            }
        )
    }
    if (showTagPicker) PandoraAlertDialog(
        onDismissRequest = { showTagPicker = false },
        title = { Text("选择标签") },
        text = {
            Column {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    availableTags.forEach { tag ->
                        val selected = tag.name in tags
                        val isPrimary = selected && tags.first() == tag.name
                        Row(
                            // 用勾选表示选择状态，操作区始终占位，避免选中后变色或撑宽弹窗。
                            Modifier.fillMaxWidth().clip(PandoraShapes.small).clickable { tags = if (selected) tags - tag.name else tags + tag.name }.padding(horizontal = PandoraSpacing.space4, vertical = PandoraSpacing.space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(PandoraDimensions.icon).clip(PandoraShapes.circle).background(tagHexColor(tag.colorHex)).border(PandoraDimensions.borderWidth, PandoraTheme.colors.border, PandoraShapes.circle))
                            Spacer(Modifier.width(PandoraSpacing.space12))
                            Text(tag.name, Modifier.weight(1f), style = PandoraTypography.bodyMedium)
                            Box(Modifier.width(72.dp).height(40.dp), contentAlignment = Alignment.Center) {
                                if (selected) {
                                    if (isPrimary) {
                                        Text("主标签", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                                    } else {
                                        TextButton(
                                            { tags = listOf(tag.name) + (tags - tag.name) },
                                            modifier = Modifier.fillMaxSize(),
                                            contentPadding = PaddingValues(0.dp),
                                            colors = ButtonDefaults.textButtonColors(contentColor = PandoraTheme.colors.textSecondary)
                                        ) { Text("设为主", style = PandoraTypography.caption) }
                                    }
                                }
                            }
                            Spacer(Modifier.width(PandoraSpacing.space8))
                            Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                                if (selected) Icon(Icons.Default.Check, "已选", Modifier.size(18.dp), tint = PandoraTheme.colors.textPrimary)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { PandoraDialogConfirm("完成", { showTagPicker = false }) }
    )
    if (createdTask != null) PandoraAlertDialog(
        onDismissRequest = { },
        title = { Text("已提交") },
        text = { Text("${createdTask?.occurrenceCount ?: 1} 次任务") },
        confirmButton = {
            TextButton({
                val created = createdTask
                createdTask = null
                onDismiss()
                if (created != null) onCreated(created)
            }) { Text("知道了", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
        }
    )
    if (scopeOpen && editingTask != null) com.pandora6ix.app.ui.task.ChangeScopeDialog(
        editingTask.occurrenceDate ?: editingTask.startDate, businessTimezone,
        onSelect = { scopeOpen = false; submit(it) }, onDismiss = { scopeOpen = false })
}

@Composable
private fun RecipientGroupRows(
    group: RecipientOrganizationGroup,
    collapsedGroups: Set<String>,
    selectedIds: Set<String>,
    onToggleGroup: (String) -> Unit,
    onTogglePerson: (RecipientDto) -> Unit
) {
    val collapsed = group.key in collapsedGroups
    Column {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 44.dp)
                .semantics { stateDescription = if (collapsed) "已折叠" else "已展开" }
                .clickable { onToggleGroup(group.key) }
                .padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, if (collapsed) "展开" else "收起", Modifier.size(16.dp), tint = PandoraTheme.colors.textTertiary)
            Spacer(Modifier.width(PandoraSpacing.space4))
            Text(group.name, Modifier.weight(1f), style = PandoraTypography.labelLarge)
            Text("${group.memberCount} 人", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
        }
        if (!collapsed) {
            group.members.forEach { candidate ->
                key(candidate.id) {
                    val selected = candidate.id in selectedIds
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { onTogglePerson(candidate) }
                            .padding(start = PandoraSpacing.space24, end = PandoraSpacing.space12, top = PandoraSpacing.space8, bottom = PandoraSpacing.space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, if (selected) "已选" else "未选", tint = PandoraTheme.colors.textPrimary)
                        Column(Modifier.padding(start = PandoraSpacing.space8).weight(1f)) {
                            Text(candidate.displayName.ifBlank { "成员 ${candidate.id.takeLast(4)}" }, style = PandoraTypography.bodyMedium)
                            candidate.jobTitle?.takeIf { it.isNotBlank() }?.let { Text(it, style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary) }
                        }
                    }
                }
            }
            Column(Modifier.padding(start = PandoraSpacing.space12)) {
                group.children.forEach { child ->
                    key(child.key) { RecipientGroupRows(child, collapsedGroups, selectedIds, onToggleGroup, onTogglePerson) }
                }
            }
        }
    }
}

/** 时间选择：左列小时 00–23、右列分钟 00–59，上下滚动或点击选择 */
@Composable
private fun TimeWheelDialog(initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var hour by remember { mutableStateOf(initialMinute / 60) }
    var minute by remember { mutableStateOf(initialMinute % 60) }
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("时", style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textTertiary)
                    WheelColumn((0..23).map { "%02d".format(it) }, hour, { hour = it })
                }
                Text(":", Modifier.padding(top = PandoraSpacing.space20), style = PandoraTypography.titleLarge, color = PandoraTheme.colors.textTertiary)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("分", style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textTertiary)
                    WheelColumn((0..59).map { "%02d".format(it) }, minute, { minute = it })
                }
            }
        },
        confirmButton = { PandoraDialogConfirm("确定", { onConfirm(hour * 60 + minute) }) },
        dismissButton = { PandoraDialogDismiss("取消", onDismiss) }
    )
}
