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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.pandora6ix.app.data.*
import com.pandora6ix.app.mock.*
import com.pandora6ix.app.ui.theme.*
import java.util.UUID
import kotlinx.coroutines.launch

private fun DemoDate.key() = "$year-$month-$day"
private fun String.date() = split("-").map(String::toInt).let { DemoDate(it[0], it[1], it[2]) }

/** 服务端标签颜色（#RRGGBB）→ Compose Color；非法值回退墨色 */
internal fun tagHexColor(hex: String): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Ink)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TaskEntrySheet(initialDate: DemoDate, canCreatePersonal: Boolean, canCreateDelegated: Boolean, availableTags: List<TagDto>, taskRepo: TaskRepository, onDismiss: () -> Unit, onCreated: (TaskDto) -> Unit) {
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
    var startMinute by rememberSaveable { mutableIntStateOf(savedDraft?.startMinute ?: 540) }
    var endMinute by rememberSaveable { mutableIntStateOf(savedDraft?.endMinute ?: 600) }
    var priority by rememberSaveable { mutableIntStateOf(savedDraft?.priority ?: 3) }
    // 任务类型/接收人相关状态不跨弹层打开残留：优先用草稿恢复，其次按能力初始化
    var kind by remember { mutableStateOf(savedDraft?.kind ?: if (canCreateDelegated) "DELEGATED" else "PERSONAL") }
    var selectedRecipientIds by remember { mutableStateOf(savedDraft?.recipientIds ?: listOf()) }
    var candidates by remember { mutableStateOf<List<RecipientDto>>(emptyList()) }
    var recipientQuery by remember { mutableStateOf("") }
    var candidateOpen by remember { mutableStateOf(false) }
    var submitting by remember { mutableStateOf(false) }
    var createdTask by remember { mutableStateOf<TaskDto?>(null) }
    val idempotencyKey = remember { UUID.randomUUID().toString() }
    LaunchedEffect(candidateOpen, selectedRecipientIds) {
        if (candidateOpen) {
            withFrameNanos { }
            queryVisibility.bringIntoView()
        }
    }
    // @ 候选：管理范围内真实用户（服务端接口检索）
    LaunchedEffect(candidateOpen, recipientQuery, kind) {
        if (kind == "DELEGATED" && candidateOpen) {
            val keyword = recipientQuery.substringAfter("@").trim()
            runCatching { taskRepo.recipientCandidates(keyword.ifBlank { null }) }.getOrNull()?.let { candidates = it.items }
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
    val dirty = title.isNotBlank() || note.isNotBlank() || selectedRecipientIds.isNotEmpty() || tags.isNotEmpty() || !allDay || priority != 3 || startKey != initialDate.key() || endKey != initialDate.key()
    val latestDirty by rememberUpdatedState(dirty)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { value ->
        if (value == SheetValue.Hidden && latestDirty) { confirmExit = true; false } else true
    })
    val requestClose = { if (dirty) confirmExit = true else onDismiss() }
    val sheetScope = rememberCoroutineScope()
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .65f), unfocusedBorderColor = Ink.copy(alpha = .25f), focusedLabelColor = Ink, cursorColor = Ink)
    val buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
    val buttonBorder = BorderStroke(1.dp, Ink.copy(alpha = .25f))
    fun pickDate(target: String) { datePickTarget = target }
    fun pickTime(target: String) { timePickTarget = target }
    fun nameOf(id: String): String = candidates.firstOrNull { it.id == id }?.displayName?.ifBlank { null } ?: "成员 ${id.takeLast(4)}"
    /** 提交创建：POST /tasks（非幂等，携带 Idempotency-Key；失败重试复用同一键） */
    fun submit() {
        error = when {
            title.isBlank() -> "请填写任务名称"
            kind == "DELEGATED" && selectedRecipientIds.isEmpty() -> "请至少选择一位接收人"
            !validTaskInterval(startKey.date(), endKey.date(), if (allDay) null else startMinute, if (allDay) null else endMinute) -> "结束日期和时间必须晚于开始时间"
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
                OutlinedTextField(title, { title = it; error = null }, Modifier.fillMaxWidth(), label = { Text("任务名称") }, placeholder = { Text("我计划做…") }, singleLine = true, colors = fieldColors)
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().heightIn(min = 112.dp), label = { Text("备注 / 简介") }, colors = fieldColors, minLines = 3)
                if (canCreatePersonal && canCreateDelegated) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == "PERSONAL", onClick = { kind = "PERSONAL"; error = null }, label = { Text("个人任务") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CreamDeep, selectedLabelColor = Ink))
                    FilterChip(selected = kind == "DELEGATED", onClick = { kind = "DELEGATED"; error = null }, label = { Text("派发任务") }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CreamDeep, selectedLabelColor = Ink))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("全天任务", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(allDay, { allDay = it; error = null }, colors = SwitchDefaults.colors(checkedTrackColor = Ink))
                }
                Text("开始与结束", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickDate("start") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, contentPadding = PaddingValues(8.dp)) { Text(startKey.date().let { "开始\n${it.year}/${it.month}/${it.day}" }, fontSize = 13.sp) }
                    OutlinedButton({ pickDate("end") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, contentPadding = PaddingValues(8.dp)) { Text(endKey.date().let { "结束\n${it.year}/${it.month}/${it.day}" }, fontSize = 13.sp) }
                }
                if (!allDay) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickTime("start") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder) { Text("开始 ${minuteLabel(startMinute)}") }
                    OutlinedButton({ pickTime("end") }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder) { Text("结束 ${minuteLabel(endMinute)}") }
                }
                Text("重要程度 · $priority 星", fontWeight = FontWeight.SemiBold)
                Row { (1..5).forEach { level -> IconButton({ priority = level }, Modifier.size(40.dp)) { Icon(if (level <= priority) Icons.Default.Star else Icons.Default.StarBorder, "$level 星", tint = Ink) } } }
                Text("标签 · 可多选，先选的为主标签", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    tags.forEachIndexed { i, name ->
                        val tagColor = availableTags.firstOrNull { it.name == name }?.let { tagHexColor(it.colorHex) } ?: Ink
                        if (i == 0) {
                            Button({ tags = tags - name }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), colors = ButtonDefaults.buttonColors(containerColor = tagColor, contentColor = Ink)) { Text("$name · 主", fontSize = 13.sp) }
                        } else {
                            OutlinedButton({ tags = tags - name }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), border = BorderStroke(1.dp, tagColor), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Text(name, fontSize = 13.sp) }
                        }
                    }
                    OutlinedButton({ showTagPicker = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), border = BorderStroke(1.dp, Ink.copy(alpha = .25f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Icon(Icons.Default.Add, "选择标签", Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("选择标签", fontSize = 13.sp) }
                }
                if (kind == "DELEGATED") {
                Text("接收人（可多选，输入 @ 检索）", fontWeight = FontWeight.SemiBold)
                if (selectedRecipientIds.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    selectedRecipientIds.forEach { id -> FilterChip(selected = true, onClick = { selectedRecipientIds = selectedRecipientIds - id; error = null }, label = { Text(nameOf(id), fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(containerColor = Color.Transparent, labelColor = Ink, selectedContainerColor = CreamDeep, selectedLabelColor = Ink)) }
                }
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
                                if (candidates.isEmpty()) Text("无匹配成员", Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 13.sp, color = Ink.copy(alpha = .5f))
                                // 按部门分组展示，避免候选人过多时一屏铺平；点击部门头可折叠/展开
                                var collapsedDepts by remember { mutableStateOf(setOf<String>()) }
                                candidates.groupBy { it.organizationName?.takeIf { n -> n.isNotBlank() } ?: "未分配部门" }
                                    .forEach { (dept, members) ->
                                        val collapsed = dept in collapsedDepts
                                        Row(
                                            Modifier.fillMaxWidth().clickable { collapsedDepts = if (collapsed) collapsedDepts - dept else collapsedDepts + dept }.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(if (collapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess, if (collapsed) "展开" else "收起", Modifier.size(16.dp), tint = Ink.copy(alpha = .55f))
                                            Spacer(Modifier.width(4.dp))
                                            Text(dept, Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                            Text("${members.size} 人", fontSize = 11.sp, color = Ink.copy(alpha = .5f))
                                        }
                                        if (!collapsed) members.forEach { candidate ->
                                            val selected = candidate.id in selectedRecipientIds
                                            Row(Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable { selectedRecipientIds = if (selected) selectedRecipientIds - candidate.id else selectedRecipientIds + candidate.id; error = null }.padding(start = 24.dp, end = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(if (selected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank, if (selected) "已选" else "未选", tint = Ink)
                                                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                                                    Text(candidate.displayName.ifBlank { "成员 ${candidate.id.takeLast(4)}" }, fontSize = 14.sp)
                                                    candidate.jobTitle?.takeIf { it.isNotBlank() }?.let { Text(it, fontSize = 11.sp, color = Ink.copy(alpha = .5f)) }
                                                }
                                            }
                                        }
                                    }
                            }
                            TextButton({ candidateOpen = false }, Modifier.fillMaxWidth()) { Text("完成", fontSize = 13.sp, color = Ink) }
                        }
                    }
                }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Button(onClick = { submit() }, Modifier.fillMaxWidth().padding(vertical = 12.dp), enabled = !submitting, colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text(if (submitting) "提交中…" else "提交任务") }
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
                Text("可多选；先选的为主标签，决定任务在视图中的颜色。再次点击已选标签可取消。", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                Spacer(Modifier.height(6.dp))
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    availableTags.forEach { tag ->
                        val selected = tag.name in tags
                        val isPrimary = selected && tags.first() == tag.name
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { tags = if (selected) tags - tag.name else tags + tag.name }.padding(horizontal = 4.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(14.dp).clip(CircleShape).background(tagHexColor(tag.colorHex)))
                            Spacer(Modifier.width(10.dp))
                            Text(tag.name, Modifier.weight(1f), fontSize = 15.sp)
                            if (selected) {
                                Text(if (isPrimary) "主标签" else "次标签", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
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
