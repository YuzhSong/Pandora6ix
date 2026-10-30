package com.pandora6ix.app.ui.task

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.pandora6ix.app.data.*
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun SeriesStatusEditor(task: TaskDto, repo: TaskRepository, status: String, timezone: String? = null,
    note: String? = null, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val owner = SessionStore(LocalContext.current).ownerId
    val scope = rememberCoroutineScope()
    var versions by remember { mutableStateOf<List<ScopeVersionDto>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val key = remember { UUID.randomUUID().toString() }
    LaunchedEffect(task.id) {
        try {
            versions = taskScopeVersions(repo.scopeSnapshot(task.id).items, owner).map {
                if (it.taskId == task.id) {
                    val assignment = task.authorizedAssignments.firstOrNull { a -> a.recipientUserId == owner }
                    it.copy(taskVersion = task.version, assignmentId = assignment?.id, assignmentVersion = assignment?.version)
                } else it
            }
        } catch (e: Exception) { error = (e as? ApiException)?.friendlyMessage() ?: "加载失败" }
    }
    when {
        error != null -> AlertDialog(onDismissRequest = onDismiss, title = { Text("未能保存") }, text = { Text(error!!) }, confirmButton = { TextButton(onDismiss) { Text("关闭") } })
        busy || versions == null -> AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, title = { Text(if (busy) "保存中…" else "加载中…") }, confirmButton = {})
        else -> ChangeScopeDialog(task.occurrenceDate ?: task.startDate, timezone, onDismiss = onDismiss, onSelect = { choice ->
            busy = true
            scope.launch {
                try { repo.scopedStatus(task.id, ScopedStatusDto(choice, status, note, versions!!), key); onSaved() }
                catch (e: Exception) { error = (e as? ApiException)?.friendlyMessage() ?: "保存失败" }
                finally { busy = false }
            }
        })
    }
}
