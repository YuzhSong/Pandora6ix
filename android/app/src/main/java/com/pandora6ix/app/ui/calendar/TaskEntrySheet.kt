package com.pandora6ix.app.ui.calendar

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.mock.*
import com.pandora6ix.app.ui.theme.*
import java.util.UUID

private fun DemoDate.key() = "$year-$month-$day"
private fun String.date() = split("-").map(String::toInt).let { DemoDate(it[0], it[1], it[2]) }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEntrySheet(initialDate: DemoDate, canCreateTasks: Boolean, onDismiss: () -> Unit, onSave: (WorkTask) -> Unit) {
    val context = LocalContext.current
    var title by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    var startKey by rememberSaveable { mutableStateOf(initialDate.key()) }
    var endKey by rememberSaveable { mutableStateOf(initialDate.key()) }
    var allDay by rememberSaveable { mutableStateOf(true) }
    var startMinute by rememberSaveable { mutableIntStateOf(540) }
    var endMinute by rememberSaveable { mutableIntStateOf(600) }
    var priority by rememberSaveable { mutableIntStateOf(3) }
    var recipients by rememberSaveable { mutableStateOf(listOf<String>()) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val dirty = title.isNotBlank() || note.isNotBlank() || recipients.isNotEmpty() || !allDay || priority != 3 || startKey != initialDate.key() || endKey != initialDate.key()
    val latestDirty by rememberUpdatedState(dirty)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = { value ->
        if (value == SheetValue.Hidden && latestDirty) { confirmExit = true; false } else true
    })
    val requestClose = { if (dirty) confirmExit = true else onDismiss() }
    val fieldColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .65f), unfocusedBorderColor = Ink.copy(alpha = .25f), focusedLabelColor = Ink, cursorColor = Ink)
    val buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
    val buttonBorder = BorderStroke(1.dp, Ink.copy(alpha = .25f))
    fun pickDate(key: String, update: (String) -> Unit) {
        val date = key.date()
        DatePickerDialog(context, { _, y, m, d -> update(DemoDate(y, m + 1, d).key()) }, date.year, date.month - 1, date.day).show()
    }
    fun pickTime(minute: Int, update: (Int) -> Unit) {
        TimePickerDialog(context, { _, h, m -> update(h * 60 + m) }, minute / 60, minute % 60, true).show()
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
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("全天任务", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    Switch(allDay, { allDay = it; error = null }, colors = SwitchDefaults.colors(checkedTrackColor = Ink))
                }
                Text("开始与结束", fontWeight = FontWeight.SemiBold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickDate(startKey) { startKey = it; error = null } }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, contentPadding = PaddingValues(8.dp)) { Text(startKey.date().let { "开始\n${it.year}/${it.month}/${it.day}" }, fontSize = 13.sp) }
                    OutlinedButton({ pickDate(endKey) { endKey = it; error = null } }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder, contentPadding = PaddingValues(8.dp)) { Text(endKey.date().let { "结束\n${it.year}/${it.month}/${it.day}" }, fontSize = 13.sp) }
                }
                if (!allDay) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ pickTime(startMinute) { startMinute = it; error = null } }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder) { Text("开始 ${minuteLabel(startMinute)}") }
                    OutlinedButton({ pickTime(endMinute) { endMinute = it; error = null } }, Modifier.weight(1f), colors = buttonColors, border = buttonBorder) { Text("结束 ${minuteLabel(endMinute)}") }
                }
                Text("重要程度 · $priority 星", fontWeight = FontWeight.SemiBold)
                Row { (1..5).forEach { level -> IconButton({ priority = level }, Modifier.size(40.dp)) { Icon(if (level <= priority) Icons.Default.Star else Icons.Default.StarBorder, "$level 星", tint = Ink) } } }
                Text("接收人（可多选）", fontWeight = FontWeight.SemiBold)
                Column(Modifier.fillMaxWidth().height(144.dp).verticalScroll(rememberScrollState())) {
                    MockData.managedMembers.forEach { member ->
                        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(member in recipients, { checked -> recipients = if (checked) recipients + member else recipients - member; error = null }, colors = CheckboxDefaults.colors(checkedColor = Ink))
                            Text(member, fontSize = 14.sp)
                        }
                    }
                }
                Text("仅保存为本地演示草稿，不会正式派发。审核入口后续设计。", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Button(onClick = {
                error = when {
                    !canCreateTasks -> "普通员工暂不能创建任务"
                    title.isBlank() -> "请填写任务名称"
                    recipients.isEmpty() -> "请至少选择一位接收人"
                    !validTaskInterval(startKey.date(), endKey.date(), if (allDay) null else startMinute, if (allDay) null else endMinute) -> "结束日期和时间必须晚于开始时间"
                    else -> null
                }
                if (error == null && canCreateTasks) onSave(WorkTask(UUID.randomUUID().toString(), title.trim(), startKey.date(), endKey.date(), "草稿（演示）", recipients.joinToString("、"), (priority - 1) % 4, priority = "★".repeat(priority), note = note.trim(), startMinute = if (allDay) null else startMinute, endMinute = if (allDay) null else endMinute, priorityLevel = priority))
            }, Modifier.fillMaxWidth().padding(vertical = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("保存任务（演示）") }
        }
    }
    if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false }, title = { Text("放弃未保存的任务？") }, text = { Text("关闭后，本次填写的内容会清空。") }, confirmButton = { TextButton({ confirmExit = false; onDismiss() }) { Text("放弃") } }, dismissButton = { TextButton({ confirmExit = false }) { Text("继续填写") } })
}
