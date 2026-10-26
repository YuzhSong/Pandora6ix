package com.pandora6ix.app.ui.calendar

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
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Ink)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TaskEntrySheet(initialDate: DemoDate, canCreatePersonal: Boolean, canCreateDelegated: Boolean, availableTags: List<TagDto>, taskRepo: TaskRepository, businessTimezone: String? = null, currentUser: UserDto? = null, onDismiss: () -> Unit, onCreated: (TaskDto) -> Unit) {
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
        AppCache.readDraft(context, "task_draft.json")?.let { s -> runCatching { AppCache.json.decodeFromString<TaskDraftDto>(s) }.getOrNull() }
    }
    var title by rememberSaveable { mutableStateOf(savedDraft?.title ?: "") }
    var note by rememberSaveable { mutableStateOf(savedDraft?.note ?: "") }
    var startKey by rememberSaveable { mutableStateOf(savedDraft?.startKey ?: initialDate.key()) }
    var endKey by rememberSaveable { mutableStateOf(savedDraft?.endKey ?: initialDate.key()) }
    var allDay by rememberSaveable { mutableStateOf(savedDraft?.allDay ?: false) }
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
    val kind = when {
        selectedRecipientIds.size == 1 && selectedRecipientIds.first() == selfRecipient?.id && canCreatePersonal -> "PERSONAL"
        else -> "DELEGATED"
    }
    var candidates by remember { mutableStateOf<List<RecipientDto>>(emptyList()) }
    val candidateNames = remember { mutableStateMapOf<String, String>() }
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
    LaunchedEffect(kind, title, note, allDay, startKey, endKey, startMinute, endMinute, priority, tags, selectedRecipientIds) {
        val draft = TaskDraftDto(kind, title, note, allDay, startKey, endKey, startMinute, endMinute, priority, tags, selectedRecipientIds)
        AppCache.writeDraft(context, "task_draft.json", AppCache.json.encodeToString(TaskDraftDto.serializer(), draft))
    }
    var showTagPicker by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    // 时间/日期选择浮层目标：start | end | null
    var timePickTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var datePickTarget by rememberSaveable { mutableStateOf<String?>(null) }
    // 进入弹层时的状态快照：退出时仅在实际改动（相对进入时）才提示保存，草稿恢复的未修改内容不算改动
    val initialState = remember { listOf(kind, title, note, startKey, endKey, allDay, startMinute, endMinute, priority, tags, selectedRecipientIds) }
    val dirty = listOf(kind, title, note, startKey, endKey, allDay, startMinute, endMinute, priority, tags, selectedRecipientIds) != initialState
    val latestDirty by rememberUpdatedState(dirty)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { value ->
        if (value == SheetValue.Hidden && latestDirty) { confirmExit = true; false } else true
    })
    val requestClose = { if (dirty) confirmExit = true else onDismiss() }
    val sheetScope = rememberCoroutineScope()
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .65f), unfocusedBorderColor = Ink.copy(alpha = .25f), focusedLabelColor = Ink, cursorColor = Ink)
    val buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
    val buttonBorder = BorderStroke(1.dp, HairLine)
    fun pickDate(target: String) { datePickTarget = target }
    fun pickTime(target: String) { timePickTarget = target }
    fun nameOf(id: String): String = candidateNames[id]?.ifBlank { null }
        ?: selfRecipient?.takeIf { it.id == id }?.displayName?.ifBlank { null }
        ?: "成员 ${id.takeLast(4)}"
    /** 提交创建：POST /tasks（非幂等，携带 Idempotency-Key；失败重试复用同一键） */
    fun submit() {
        error = when {
            title.isBlank() -> "请填写任务名称"
            selectedRecipientIds.isEmpty() -> "请至少选择一位接收人"
            !validTaskInterval(startKey.date(), endKey.date(), if (allDay) null else startMinute, if (allDay) null else endMinute) -> "结束日期和时间必须晚于开始时间"
            // 截止时间不能早于公司业务时区的当前时刻（服务端也按业务时区判定）
            !deadlineNotPast(endKey, if (allDay) null else endMinute, allDay, businessTimezone) ->
                "截止时间不能早于当前时间（公司时间 ${businessNowLabel(businessTimezone)}）"
            else -> null
        }
        if (error != null) return
        submitting = true; error = null
        sheetScope.launch {
            try {
                val tagIds = tags.mapNotNull { name -> availableTags.firstOrNull { it.name == name }?.id }
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
                        primaryTagId = tagIds.firstOrNull()
                    ),
                    idempotencyKey
                )
                AppCache.deleteDraft(context, "task_draft.json")
            } catch (e: ApiException) {
                error = e.friendlyMessage()
            } catch (e: Exception) {
                error = "网络异常，请重试"
            } finally {
                submitting = false
            }
        }
    }
    ModalBottomSheet(onDismissRequest = requestClose, sheetState = sheetState, containerColor = Cream) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.88f).padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("添加任务", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(requestClose) { Icon(Icons.Default.Close, "关闭任务填写") }
            }
            Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it; error = null }, Modifier.fillMaxWidth(), label = { Text("任务名称（必填）") }, placeholder = { Text("我计划做…") }, singleLine = true, colors = fieldColors)
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().heightIn(min = 112.dp), label = { Text("备注 / 简介（选填）") }, colors = fieldColors, minLines = 3)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("全天任务", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(allDay, { allDay = it; error = null }, colors = com.pandora6ix.app.ui.theme.pandoraSwitchColors())
                }
                Text("开始与结束", Modifier.fillMaxWidth(), fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickDate("start") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, contentPadding = PaddingValues(8.dp)) { Text(startKey.date().let { "开始\n${it.year}/${it.month}/${it.day}" }, fontSize = 13.sp) }
                    OutlinedButton({ pickDate("end") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, contentPadding = PaddingValues(8.dp)) { Text(endKey.date().let { "结束\n${it.year}/${it.month}/${it.day}" }, fontSize = 13.sp) }
                }
                if (!allDay) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickTime("start") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder) { Text("开始 ${minuteLabel(startMinute)}") }
                    OutlinedButton({ pickTime("end") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder) { Text("结束 ${minuteLabel(endMinute)}") }
                }
                // 星级文字含义：与审批规则一致——星级越高越优先处理
                Text("重要程度 · $priority 星 · ${priorityLabel(priority)}", fontWeight = FontWeight.SemiBold)
                Row { (1..5).forEach { level -> IconButton({ priority = level }, Modifier.size(40.dp)) { Icon(if (level <= priority) Icons.Default.Star else Icons.Default.StarBorder, "$level 星", tint = Ink) } } }
                Text("标签 · 可选，可多选；主标签决定任务颜色，未选标签的任务在视图中以中性色展示", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    tags.forEachIndexed { i, name ->
                        val tagColor = availableTags.firstOrNull { it.name == name }?.let { tagHexColor(it.colorHex) } ?: Ink
                        if (i == 0) {
                            Button({ tags = tags - name }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = tagColor, contentColor = Ink)) { Text("$name · 主", fontSize = 13.sp) }
                        } else {
                            OutlinedButton({ tags = tags - name }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), border = BorderStroke(1.dp, tagColor), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Text(name, fontSize = 13.sp) }
                        }
                    }
                    OutlinedButton({ showTagPicker = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), border = BorderStroke(1.dp, HairLine), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Icon(Icons.Default.Add, "选择标签", Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("选择标签", fontSize = 13.sp) }
                }
                Text(if (canDelegate) "接收人（可多选，输入 @ 检索）" else "接收人", fontWeight = FontWeight.SemiBold)
                if (selectedRecipientIds.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedRecipientIds.forEach { id ->
                        if (canDelegate) {
                            FilterChip(selected = true, onClick = { selectedRecipientIds = selectedRecipientIds - id; error = null }, label = { Text(nameOf(id), fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent, labelColor = Ink, selectedContainerColor = CreamDeep, selectedLabelColor = Ink))
                        } else {
                            // 无派发权限：接收人锁定为本人，浅蓝底黑字展示且不可更改
                            FilterChip(selected = true, onClick = { }, enabled = false, label = { Text(nameOf(id), fontSize = 13.sp, color = Ink) }, colors = FilterChipDefaults.filterChipColors(disabledContainerColor = Color(0xFFD6EAF8), disabledLabelColor = Ink))
                        }
                    }
                }
                if (canDelegate) {
                Box(Modifier.bringIntoViewRequester(queryVisibility).onGloballyPositioned { coordinates ->
                    queryWidth = coordinates.size.width
                    queryTop = coordinates.boundsInWindow().top.toInt()
                }) {
                    OutlinedTextField(recipientQuery, { recipientQuery = it; candidateOpen = it.contains("@") }, Modifier.fillMaxWidth(), placeholder = { Text("@ 检索接收人，勾选候选") }, singleLine = true, colors = fieldColors)
                    if (candidateOpen && queryWidth > 0) Popup(
                        popupPositionProvider = candidatePosition,
                        onDismissRequest = { candidateOpen = false },
                        properties = PopupProperties(focusable = false)
                    ) {
                        val maxCandidateHeight = with(density) { (queryTop - candidateGap - candidateInset).coerceAtLeast(0).toDp() }.coerceAtMost(280.dp)
                        Column(Modifier.width(with(density) { queryWidth.toDp() }).heightIn(max = maxCandidateHeight).clip(RoundedCornerShape(12.dp)).background(CreamDeep).border(1.dp, Ink.copy(alpha = .12f), RoundedCornerShape(12.dp)).padding(vertical = 4.dp)) {
                            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                                when {
                                    candidateLoading -> Text("正在加载成员…", Modifier.padding(12.dp), fontSize = 13.sp)
                                    candidateError -> {
                                        Text("成员加载失败，请重试", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp)
                                        TextButton({ candidateRetry++ }) { Text("重试", color = Ink) }
                                    }
                                    candidates.isEmpty() -> Text("无匹配成员", Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, color = Ink.copy(alpha = .5f))
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
                            TextButton({ candidateOpen = false }, Modifier.fillMaxWidth()) { Text("完成", fontSize = 13.sp, color = Ink) }
                        }
                    }
                }
                }
            }
            // 标签已改为可选：无可用标签不再阻断提交
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Button(onClick = { submit() }, Modifier.fillMaxWidth().padding(vertical = 12.dp), enabled = !submitting, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton)) { Text(if (submitting) "提交中…" else "提交审批") }
            Text("提交后进入直属上级审批，批准前任务不会出现在日历中", Modifier.fillMaxWidth(), fontSize = 11.sp, color = Ink.copy(alpha = .55f))
        }
    }
    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false; sheetScope.launch { sheetState.show() } }, title = { Text("放弃未保存的任务？") }, text = { Text("关闭后，本次填写的内容会清空。") }, confirmButton = { TextButton({ confirmExit = false; AppCache.deleteDraft(context, "task_draft.json"); onDismiss() }) { Text("放弃") } }, dismissButton = { TextButton({ confirmExit = false; sheetScope.launch { sheetState.show() } }) { Text("继续填写") } })
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
    if (showTagPicker) AlertDialog(
        onDismissRequest = { showTagPicker = false },
        title = { Text("选择标签") },
        text = {
            Column {
                Text("可多选；点“设为主”可随时更换主标签（决定任务在视图中的颜色）。再次点击已选标签可取消。", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                Spacer(Modifier.height(6.dp))
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    availableTags.forEach { tag ->
                        val selected = tag.name in tags
                        val isPrimary = selected && tags.first() == tag.name
                        Row(
                            // 选中行整行高亮，勾选状态一目了然
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (selected) CreamDeep else Color.Transparent).clickable { tags = if (selected) tags - tag.name else tags + tag.name }.padding(horizontal = 4.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(tagHexColor(tag.colorHex)))
                            Spacer(Modifier.width(10.dp))
                            Text(tag.name, Modifier.weight(1f), fontSize = 15.sp)
                            if (selected) {
                                if (isPrimary) {
                                    Text("主标签", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                                } else {
                                    TextButton({ tags = listOf(tag.name) + (tags - tag.name) }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("设为主", fontSize = 12.sp, color = WarmOrange) }
                                }
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.Default.Check, "已选", Modifier.size(18.dp), tint = Ink)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ showTagPicker = false }) { Text("完成") } }
    )
    if (createdTask != null) AlertDialog(
        onDismissRequest = { },
        title = { Text("已提交") },
        text = { Text("任务已提交，等待直属上级审批。审批通过后，接收人才能看到正式任务。") },
        confirmButton = {
            TextButton({
                val created = createdTask
                createdTask = null
                onDismiss()
                if (created != null) onCreated(created)
            }) { Text("知道了", color = Ink, fontWeight = FontWeight.Bold) }
        }
    )
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, if (collapsed) "展开" else "收起", Modifier.size(16.dp), tint = Ink.copy(alpha = .55f))
            Spacer(Modifier.width(4.dp))
            Text(group.name, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("${group.memberCount} 人", fontSize = 11.sp, color = Ink.copy(alpha = .5f))
        }
        if (!collapsed) {
            group.members.forEach { candidate ->
                key(candidate.id) {
                    val selected = candidate.id in selectedIds
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { onTogglePerson(candidate) }
                            .padding(start = 24.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, if (selected) "已选" else "未选", tint = Ink)
                        Column(Modifier.padding(start = 8.dp).weight(1f)) {
                            Text(candidate.displayName.ifBlank { "成员 ${candidate.id.takeLast(4)}" }, fontSize = 14.sp)
                            candidate.jobTitle?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 11.sp, color = Ink.copy(alpha = .5f)) }
                        }
                    }
                }
            }
            Column(Modifier.padding(start = 12.dp)) {
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择时间", fontWeight = FontWeight.Bold) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("时", fontSize = 13.sp, color = Ink.copy(alpha = .6f))
                    WheelColumn((0..23).map { "%02d".format(it) }, hour, { hour = it })
                }
                Text(":", Modifier.padding(top = 20.dp), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink.copy(alpha = .55f))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("分", fontSize = 13.sp, color = Ink.copy(alpha = .6f))
                    WheelColumn((0..59).map { "%02d".format(it) }, minute, { minute = it })
                }
            }
        },
        confirmButton = { TextButton({ onConfirm(hour * 60 + minute) }) { Text("确定", color = Ink, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Ink.copy(alpha = .6f)) } }
    )
}
