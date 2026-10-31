package com.pandora6ix.app.ui.task

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.data.ApiException
import com.pandora6ix.app.data.AssignmentDto
import com.pandora6ix.app.data.CompletionRequestDto
import com.pandora6ix.app.data.SessionExpiredException
import com.pandora6ix.app.data.TaskDto
import com.pandora6ix.app.data.TaskRepository
import com.pandora6ix.app.data.executionStatusLabel
import com.pandora6ix.app.ui.theme.CoralDark
import com.pandora6ix.app.ui.theme.CreamDeep
import com.pandora6ix.app.ui.theme.Ink
import com.pandora6ix.app.ui.theme.Mint
import com.pandora6ix.app.ui.theme.MintDeep
import com.pandora6ix.app.ui.theme.WarmCard
import com.pandora6ix.app.ui.theme.WarmOrange
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 任务操作面板（4.3 进度 + 4.4 完成/验收），全部动作由 assignment.allowedActions 驱动：
 * - UPDATE_PROGRESS：滑杆记录进度（expectedVersion = assignment.version；100 仍为 ACTIVE）
 * - COMPLETE_PERSONAL：个人自完成（POST /tasks/{id}/complete，expectedAssignmentVersion）
 * - REQUEST_COMPLETION：派发接收人申请验收（completion-requests，执行关系进入 AWAITING_ACCEPTANCE）
 * - DECIDE_COMPLETION：创建者对该执行关系的待验收集合 ACCEPT|REJECT（expectedVersion = request.version）
 * - 任一动作 409 后重读任务详情并换新 Idempotency-Key；成功后刷新日历数据
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressSheet(task: TaskDto, taskRepo: TaskRepository, onDismiss: () -> Unit, onChanged: () -> Unit) {
    var current by remember { mutableStateOf(task) }
    var refreshing by remember { mutableStateOf(false) }
    val keys = remember { mutableStateMapOf<String, String>() }
    val uiScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var repeatStatus by remember { mutableStateOf<String?>(null) }
    var repeatNote by remember { mutableStateOf<String?>(null) }
    var repeatSaved by remember { mutableStateOf<(() -> Unit)?>(null) }
    var repeatCancelled by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun keyFor(action: String, id: String): String = keys.getOrPut("$action:$id") { UUID.randomUUID().toString() }

    fun refresh() {
        refreshing = true
        uiScope.launch {
            try {
                current = taskRepo.task(current.id)
                onChanged()
            } catch (_: SessionExpiredException) {
            } catch (_: Exception) { }
            refreshing = false
        }
    }

    if (repeatStatus != null) SeriesStatusEditor(current, taskRepo, repeatStatus!!, note = repeatNote,
        onDismiss = { repeatStatus = null; repeatCancelled?.invoke() },
        onSaved = { repeatStatus = null; repeatSaved?.invoke(); refresh() })
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = Color.White) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("任务操作", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(current.title, fontSize = 13.sp, color = Ink.copy(alpha = .6f), maxLines = 1)
                }
                if (refreshing) CircularProgressIndicator(Modifier.height(22.dp), color = WarmOrange)
                IconButton(onDismiss) { Icon(Icons.Default.Close, "关闭任务操作") }
            }
            if (current.authorizedAssignments.isEmpty()) {
                Text("当前没有可见的执行关系。", color = Ink.copy(alpha = .55f), fontSize = 14.sp)
            }
            current.authorizedAssignments.forEachIndexed { index, assignment ->
                AssignmentActions(
                    label = if (current.authorizedAssignments.size > 1) "执行关系 ${index + 1}" else "我的执行关系",
                    assignment = assignment,
                    pendingRequests = current.completionRequests.filter { it.assignmentId == assignment.id && it.decision == "PENDING" },
                    onProgress = { percent, note, onDone, onError -> uiScope.launch {
                        try {
                            taskRepo.recordProgress(assignment.id, assignment.version, percent, note.ifBlank { null }, keyFor("progress", assignment.id))
                            keys["progress:${assignment.id}"] = UUID.randomUUID().toString()
                            onChanged(); onDone(); refresh()
                        } catch (e: SessionExpiredException) {
                        } catch (e: ApiException) {
                            onError(e.friendlyMessage())
                            if (e.httpStatus == 409) { keys["progress:${assignment.id}"] = UUID.randomUUID().toString(); refresh() }
                        } catch (e: Exception) { onError("操作失败，请稍后重试") }
                    } },
                    onComplete = { onDone, onError -> if (current.seriesId != null) {
                        repeatNote = null; repeatSaved = { onChanged(); onDone() }; repeatCancelled = { onError("") }; repeatStatus = "COMPLETED"
                    } else uiScope.launch {
                        try {
                            taskRepo.completePersonalTask(current.id, assignment.version, keyFor("complete", assignment.id))
                            keys["complete:${assignment.id}"] = UUID.randomUUID().toString()
                            onChanged(); onDone(); refresh()
                        } catch (e: SessionExpiredException) {
                        } catch (e: ApiException) {
                            onError(e.friendlyMessage())
                            if (e.httpStatus == 409) { keys["complete:${assignment.id}"] = UUID.randomUUID().toString(); refresh() }
                        } catch (e: Exception) { onError("操作失败，请稍后重试") }
                    } },
                    onRequest = { note, onDone, onError -> if (current.seriesId != null) {
                        repeatNote = note; repeatSaved = { onChanged(); onDone() }; repeatCancelled = { onError("") }; repeatStatus = "COMPLETED"
                    } else uiScope.launch {
                        try {
                            taskRepo.requestCompletion(assignment.id, assignment.version, note.ifBlank { null }, keyFor("request", assignment.id))
                            keys["request:${assignment.id}"] = UUID.randomUUID().toString()
                            onChanged(); onDone(); refresh()
                        } catch (e: SessionExpiredException) {
                        } catch (e: ApiException) {
                            onError(e.friendlyMessage())
                            if (e.httpStatus == 409) { keys["request:${assignment.id}"] = UUID.randomUUID().toString(); refresh() }
                        } catch (e: Exception) { onError("操作失败，请稍后重试") }
                    } },
                    onDecide = { request, accept, comment, onDone, onError -> uiScope.launch {
                        try {
                            taskRepo.decideCompletion(request.id, if (accept) "ACCEPT" else "REJECT", request.version, comment.ifBlank { null }, keyFor("decide", request.id))
                            keys["decide:${request.id}"] = UUID.randomUUID().toString()
                            onChanged(); onDone(); refresh()
                        } catch (e: SessionExpiredException) {
                        } catch (e: ApiException) {
                            onError(e.friendlyMessage())
                            if (e.httpStatus == 409) { keys["decide:${request.id}"] = UUID.randomUUID().toString(); refresh() }
                        } catch (e: Exception) { onError("操作失败，请稍后重试") }
                    } }
                )
            }
            if (current.progressRecords.isNotEmpty()) {
                Text("最近进度", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    current.progressRecords.sortedByDescending { it.recordedAt }.take(5).forEach { record ->
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CreamDeep).padding(10.dp)) {
                            Text("${record.progressPercent}%${record.note?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}", fontSize = 13.sp)
                            record.recordedAt?.let { Text(it.substring(0, minOf(16, it.length)).replace("T", " "), fontSize = 11.sp, color = Ink.copy(alpha = .55f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AssignmentActions(
    label: String,
    assignment: AssignmentDto,
    pendingRequests: List<CompletionRequestDto>,
    onProgress: (percent: Int, note: String, onDone: () -> Unit, onError: (String) -> Unit) -> Unit,
    onComplete: (onDone: () -> Unit, onError: (String) -> Unit) -> Unit,
    onRequest: (note: String, onDone: () -> Unit, onError: (String) -> Unit) -> Unit,
    onDecide: (request: CompletionRequestDto, accept: Boolean, comment: String, onDone: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    val actions = assignment.allowedActions
    var percent by remember(assignment.id, assignment.version) { mutableStateOf(assignment.progressPercent.coerceIn(0, 100)) }
    var note by remember(assignment.id) { mutableStateOf("") }
    var error by remember(assignment.id, assignment.version) { mutableStateOf<String?>(null) }
    var busy by remember(assignment.id) { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var rejectRequest by remember { mutableStateOf<CompletionRequestDto?>(null) }
    var doneMessage by remember { mutableStateOf<String?>(null) }

    fun done(action: String, accept: Boolean = true) {
        busy = false; confirm = null; rejectRequest = null; note = ""
        doneMessage = when (action) {
            "complete" -> "任务已完成"
            "request" -> "已提交验收申请，等待创建者确认"
            "decide" -> if (accept) "已接受完成" else "已驳回完成申请，接收人可继续执行"
            else -> "已记录"
        }
    }
    fun fail(msg: String) { error = msg.takeIf { it.isNotBlank() }; busy = false; confirm = null; rejectRequest = null }

    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Mint.copy(alpha = .45f)).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(executionStatusLabel(assignment.executionStatus), fontSize = 12.sp, color = Ink.copy(alpha = .6f))
        }
        LinearProgressIndicator(progress = { assignment.progressPercent.coerceIn(0, 100) / 100f }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)), color = MintDeep, trackColor = WarmCard)
        Text("当前 ${assignment.progressPercent}%", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
        error?.let { Text(it, color = CoralDark, fontSize = 13.sp) }

        if (actions.contains("UPDATE_PROGRESS")) {
            Slider(value = percent.toFloat(), onValueChange = { percent = it.toInt() }, valueRange = 0f..100f, steps = 19, enabled = !busy)
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                Modifier.fillMaxWidth().heightIn(min = 64.dp),
                label = { Text("备注（可选）") },
                minLines = 1,
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .7f), unfocusedBorderColor = Ink.copy(alpha = .3f), focusedLabelColor = Ink, cursorColor = Ink)
            )
            Button({ busy = true; onProgress(percent, note, { done("progress") }, ::fail) }, Modifier.fillMaxWidth(), enabled = !busy) {
                Text(if (percent >= 100) "记录 100%（仍为进行中）" else "记录 $percent%")
            }
            if (percent >= 100) Text("进度 100% 不计为完成；完成需按验收流程确认。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
        }

        if (actions.contains("COMPLETE_PERSONAL")) {
            Button({ confirm = "complete" }, Modifier.fillMaxWidth(), enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = MintDeep, contentColor = Color.White)) {
                Text("完成任务")
            }
            Text("确认后本人执行关系标记为已完成，计入任务统计。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
        }

        if (actions.contains("REQUEST_COMPLETION")) {
            Button({ confirm = "request" }, Modifier.fillMaxWidth(), enabled = !busy) {
                Text("申请验收")
            }
            Text("提交后创建者将收到完成确认申请，确认前任务保持进行中。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
        }

        if (actions.contains("DECIDE_COMPLETION") && pendingRequests.isNotEmpty()) {
            Text("待验收申请", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            pendingRequests.forEach { request ->
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CreamDeep).padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    request.submittedNote?.takeIf { it.isNotBlank() }?.let { Text("留言：$it", fontSize = 13.sp) }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button({ busy = true; onDecide(request, true, "", { done("decide", true) }, ::fail) }, Modifier.weight(1f), enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = MintDeep, contentColor = Color.White)) { Text("接受完成") }
                        OutlinedButton({ rejectRequest = request }, Modifier.weight(1f), enabled = !busy, colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDark)) { Text("驳回") }
                    }
                }
            }
        }

        if (actions.isEmpty()) {
            Text("该执行关系当前无可执行的操作。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
        }
    }

    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { if (!busy) confirm = null },
            title = { Text(if (action == "complete") "确认完成任务？" else "提交验收申请？") },
            confirmButton = {
                if (!busy) TextButton({
                    confirm = null
                    if (action == "complete") { busy = true; onComplete({ done("complete") }, ::fail) }
                    else { busy = true; onRequest(note, { done("request") }, ::fail) }
                }) { Text("确认") }
            },
            dismissButton = { if (!busy) TextButton({ confirm = null }) { Text("取消") } }
        )
    }
    rejectRequest?.let { request ->
        AlertDialog(
            onDismissRequest = { if (!busy) rejectRequest = null },
            title = { Text("驳回完成申请？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("驳回后接收人可继续执行并重新提交。")
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("驳回理由（可选）") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .7f), unfocusedBorderColor = Ink.copy(alpha = .3f), focusedLabelColor = Ink, cursorColor = Ink)
                    )
                }
            },
            confirmButton = {
                if (!busy) TextButton({ busy = true; onDecide(request, false, note, { done("decide", false) }, ::fail) }) { Text("驳回", color = CoralDark) }
                else CircularProgressIndicator(Modifier.height(22.dp), color = WarmOrange)
            },
            dismissButton = { if (!busy) TextButton({ rejectRequest = null }) { Text("取消") } }
        )
    }
    doneMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("操作完成") },
            text = { Text(message) },
            confirmButton = { TextButton({ doneMessage = null }) { Text("好的") } }
        )
    }
}
