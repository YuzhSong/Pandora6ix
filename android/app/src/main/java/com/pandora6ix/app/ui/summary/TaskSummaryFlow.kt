package com.pandora6ix.app.ui.summary

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import com.pandora6ix.app.data.*
import com.pandora6ix.app.ui.components.*
import com.pandora6ix.app.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal val TaskScopes = listOf("mine" to "我的任务", "created" to "我创建的", "managed" to "下属任务")

/** 任务汇总：只有任务表格；下属与日志在通讯录。分页器按范围常驻，切换范围或进出详情不丢已加载的页。 */
@Composable
fun TaskSummaryFlow(
    user: UserDto,
    calendarRepo: CalendarRepository,
    taskDetail: @Composable (String, () -> Unit, () -> Unit) -> Unit,
    landscapeRequested: Boolean,
    compact: Boolean,
    onRotate: () -> Unit,
    onClose: () -> Unit
) {
    val businessTimezone = user.businessTimezone
    val canManage = user.capabilities.manage && user.capabilities.business
    val canRead = user.capabilities.business
    val taskPagers = remember(user.id) {
        TaskScopes.associate { (taskScope, _) -> taskScope to SummaryPager<TaskDto>({ it.id }) { cursor ->
            calendarRepo.tasks(scope = taskScope, cursor = cursor, limit = 20).let { SummaryPage(it.items, it.nextCursor) }
        } }
    }
    var taskScope by rememberSaveable(user.id) { mutableStateOf("mine") }
    var taskId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    val uiStates = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val selected = taskPagers.getValue(taskScope)
    val allowed = canRead && (taskScope != "managed" || canManage)
    val name: (String) -> String = { summaryPersonName(it, user, emptyList()) }
    val taskRefreshRevision = LocalTaskRefreshRevision.current
    LaunchedEffect(user.id, taskRefreshRevision) {
        if (taskRefreshRevision > 0 && canRead) taskPagers.values.filter { it.initialized }.forEach { pager ->
            snapshotFlow { pager.loading }.first { !it }
            pager.load(refresh = true)
        }
    }
    LaunchedEffect(taskScope, allowed) { if (allowed) selected.ensureLoaded() }

    val activity = LocalContext.current.findActivity()
    if (landscapeRequested && activity != null) {
        DisposableEffect(activity) {
            val previousOrientation = activity.requestedOrientation
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose { activity.requestedOrientation = previousOrientation }
        }
    }

    BackHandler { if (taskId != null) taskId = null else onClose() }
    if (taskId != null) {
        taskDetail(taskId!!, { taskId = null }) {
            taskPagers.values.filter { it.initialized }.forEach { pager -> scope.launch { pager.load(refresh = true) } }
        }
        return
    }
    TaskSummaryLayout(
        compact = compact, taskScope = taskScope, onScope = { taskScope = it }, onBack = onClose,
        onRotate = onRotate, rotationLocked = landscapeRequested
    ) { tableModifier ->
        if (!allowed) Note(if (!canRead) "当前账号没有业务任务查看权限" else "当前账号没有下属任务查看权限")
        // 每个范围各自保存表格的纵向/横向滚动位置
        else uiStates.SaveableStateProvider(taskScope) {
            TaskSummaryTable(selected, name, { taskId = it }, { scope.launch { selected.load() } }, tableModifier, businessTimezone)
        }
    }
}

@Composable
internal fun taskSummaryCompactWindow(): Boolean {
    val window = LocalConfiguration.current
    return window.screenWidthDp > window.screenHeightDp || window.screenHeightDp in 1..480
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
