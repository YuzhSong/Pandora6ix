package com.pandora6ix.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import org.json.JSONObject
import cn.jpush.android.api.JPushInterface
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.pandora6ix.app.data.*
import com.pandora6ix.app.mock.*
import com.pandora6ix.app.ui.calendar.*
import com.pandora6ix.app.ui.review.ReviewScreen
import com.pandora6ix.app.ui.task.ProgressSheet
import com.pandora6ix.app.ui.tags.TagManageScreen
import com.pandora6ix.app.ui.logs.JournalDatePickerDialog
import com.pandora6ix.app.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.File
import java.util.UUID

/** 默认头像色板（暖色系 8 色，选中即在圆形上显示姓名首字） */
private val AvatarPalette = listOf(
    0xFFF2A65A, 0xFFE8837B, 0xFF7BC5A8, 0xFF7FA8E0,
    0xFFB79AE0, 0xFFE0A0C0, 0xFF8FB573, 0xFFD9B36C
).map { Color(it) }

/** 圆形头像：照片（本地文件，降采样解码）或默认色板 + 姓名首字 */
@Composable
private fun AvatarView(avatarType: String, avatarColorIndex: Int, avatarPath: String, name: String, size: Dp) {
    Box(
        Modifier.size(size).clip(CircleShape).background(AvatarPalette[avatarColorIndex.coerceIn(0, AvatarPalette.size - 1)]),
        contentAlignment = Alignment.Center
    ) {
        val photo = avatarType == "photo" && avatarPath.isNotBlank() && File(avatarPath).exists()
        val bitmap = remember(avatarPath) { if (photo) decodeAvatar(avatarPath) else null }
        if (photo && bitmap != null) {
            Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(name.take(1).ifBlank { "我" }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = (size.value / 2.6f).sp)
        }
    }
}

private fun decodeAvatar(path: String, target: Int = 512) = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= target) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
}.getOrNull()

/** 查询媒体库图片（系统按授权范围自动过滤：限制访问时仅返回用户选中的照片），按添加时间倒序 */
private fun queryGalleryImages(context: Context, limit: Int = 500): List<Uri> = runCatching {
    val collection = if (Build.VERSION.SDK_INT >= 29) MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val uris = mutableListOf<Uri>()
    context.contentResolver.query(collection, arrayOf(MediaStore.Images.Media._ID), null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC")?.use { c ->
        val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (c.moveToNext() && uris.size < limit) uris.add(Uri.withAppendedPath(collection, c.getLong(idCol).toString()))
    }
    uris
}.getOrDefault(emptyList())

private fun decodeUriThumbnail(context: Context, uri: Uri, target: Int = 192) = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
    context.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
}.getOrNull()

class MainActivity : ComponentActivity() {
    private val notificationOpenEvents = Channel<String>(Channel.BUFFERED)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.JPUSH_APP_KEY.isNotBlank()) {
            JPushInterface.setDebugMode(BuildConfig.DEBUG)
            JPushInterface.init(this)
        }
        capturePushIntent(intent)
        val session = SessionStore(this)
        val api = ApiClient(session)
        val auth = AuthRepository(api, session)
        setContent { PandoraApp(session, api, auth, notificationOpenEvents.receiveAsFlow()) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        capturePushIntent(intent)
    }

    private fun capturePushIntent(intent: android.content.Intent?) {
        val direct = intent?.getStringExtra("notificationId")
        val encoded = intent?.getStringExtra("cn.jpush.android.EXTRA")
        val notificationId = direct ?: runCatching { JSONObject(encoded ?: "{}").optString("notificationId") }.getOrNull()
        if (!notificationId.isNullOrBlank()) notificationOpenEvents.trySend(notificationId)
    }
}

private enum class Route { HOME, VIEW, LOGS, AI, ME }

/** 任务操作面板覆盖的动作集合（进度/自完成/申请验收/验收决定），用于详情页入口判定 */
// 详情页"记录进度"入口的动作集：仅实际执行人可见；
// 发起人的验收决策（DECIDE_COMPLETION）不触发该入口，避免发起人在视图页看到与己无关的"进度记录"
private val PROGRESS_ENTRY_ACTIONS = setOf("UPDATE_PROGRESS", "COMPLETE_PERSONAL", "REQUEST_COMPLETION")
private data class NavItem(val route: Route, val label: String, val icon: ImageVector)
/** emptyText：该面板为空时的提示（四个面板语义不同，分别给出「暂无任务」「暂无日志」等） */
private data class HomePanelDef(val title: String, val items: List<String>, val emptyText: String)

/** 导图面板分页状态：items 为已加载条目；nextCursor 非空表示还有更多可加载 */
private class HomePanelPager<T> {
    val items = mutableStateListOf<T>()
    var nextCursor by mutableStateOf<String?>(null)
    var moreUrl by mutableStateOf<String?>(null)
    var loadingMore by mutableStateOf(false)
}
private data class HomeDetail(
    val section: String,
    val index: Int,
    val text: String,
    val accent: Color,
    val kind: String? = null,   // COMPANY_ITEM / TASK / WORK_ENTRY：按稳定 ID 读取全文
    val id: String? = null
)

/** 提醒轮询间隔（App 前台；无后台调度，退到后台或进程被杀后不再比对） */
private const val REMINDER_POLL_MILLIS = 60L * 1000L
/** 截止时间提前量预设值（分钟）：1 / 2 / 6 / 12 / 24 小时 */
private val REMINDER_LEAD_OPTIONS = listOf(60, 120, 360, 720, 1440)

/** 提前量展示：1440 → "24 小时"，90 → "1 小时 30 分钟"，45 → "45 分钟" */
private fun leadLabel(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest 分钟"
        rest == 0 -> "$hours 小时"
        else -> "$hours 小时 $rest 分钟"
    }
}

/** 底部导航图标：待审批数 > 0 时在“我的”图标上挂数字角标（99+ 封顶） */
@Composable
private fun MeNavIcon(item: NavItem, badgeCount: Int) {
    if (item.route == Route.ME && badgeCount > 0) {
        androidx.compose.material3.BadgedBox(badge = { androidx.compose.material3.Badge { Text(if (badgeCount >= 99) "99+" else "$badgeCount") } }) {
            Icon(item.icon, item.label)
        }
    } else {
        Icon(item.icon, item.label)
    }
}

/** 白色圆角卡片边框：主题背景与卡片色（WarmCard）过于接近时（如暖米）加细边框以免融入背景，其余主题返回 null 无边框 */
@Composable
private fun cardBorderIfBlended(): BorderStroke? {
    val bg = LocalAppTheme.current.background
    return if (kotlin.math.abs(bg.luminance() - WarmCard.luminance()) < 0.02f)
        BorderStroke(1.dp, Ink.copy(alpha = 0.14f)) else null
}

@Composable
fun PandoraApp(session: SessionStore, api: ApiClient, auth: AuthRepository, notificationOpenEvents: Flow<String>) {
    PandoraTheme {
        var route by rememberSaveable { mutableStateOf(Route.HOME.name) }
        var modeName by rememberSaveable { mutableStateOf(CalendarMode.WEEK.name) }
        var dateKey by rememberSaveable { mutableStateOf(todayDate().let { "${it.year}-${it.month}-${it.day}" }) }
        var detail by rememberSaveable { mutableStateOf<String?>(null) }
        var homeDetail by remember { mutableStateOf<HomeDetail?>(null) }
        // 日志详情：由日志卡片点开，按 journalId 读取整篇
        var journalDetailId by remember { mutableStateOf<String?>(null) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var showReviews by rememberSaveable { mutableStateOf(false) }
        // 标签管理覆盖层（我的页进入，入口仅对有权限角色显示）
        var showTagManage by rememberSaveable { mutableStateOf(false) }
        // 进度记录浮层：从任务详情进入，显示当前任务的执行关系
        var progressTaskId by remember { mutableStateOf<String?>(null) }
        var fontScale by rememberSaveable { mutableStateOf(1f) }
        var themeKey by rememberSaveable { mutableStateOf("warm") }
        var currentUser by remember { mutableStateOf<UserDto?>(null) }
        var authChecked by remember { mutableStateOf(false) }
        // 标签增改删权限：与服务端一致（仅 FOUNDER/DEPARTMENT_MANAGER）；未来接入 capabilities 后再细分
        val tagManager = currentUser?.capabilities?.createTags == true
        val theme = appThemes.firstOrNull { it.key == themeKey } ?: appThemes.first()
        // 视图页任务与企业标签目录来自真实接口（登录后按模式/日期加载）
        val taskItems = remember { mutableStateListOf<TaskItem>() }
        // 保留服务端原始 TaskDto（执行关系/allowedActions/进度），供详情与进度记录使用
        val taskDtoById = remember { mutableStateMapOf<String, TaskDto>() }
        val serverTags = remember { mutableStateListOf<TagDto>() }
        val date = dateKey.split("-").map(String::toInt).let { DemoDate(it[0], it[1], it[2]) }
        val current = Route.valueOf(route)
        val nav = listOf(
            NavItem(Route.HOME, "导图", Icons.Default.GridView),
            NavItem(Route.VIEW, "视图", Icons.Default.CalendarMonth),
            NavItem(Route.LOGS, "日志", Icons.Default.NoteAlt),
            NavItem(Route.AI, "AI地图", Icons.Default.Psychology),
            NavItem(Route.ME, "我的", Icons.Default.PersonOutline)
        )
        val baseDensity = LocalDensity.current
        val scaledDensity = remember(baseDensity, fontScale) { Density(baseDensity.density, baseDensity.fontScale * fontScale) }
        val dashboardRepo = remember { DashboardRepository(api) }
        // 通知提醒：偏好本地持久化（按账号隔离），提醒以页内弹窗为主、系统通知为辅
        val appContext = LocalContext.current
        val notifyPrefs = remember(currentUser?.id) { NotificationPrefs(appContext, currentUser?.id) }
        val pushDeviceRepo = remember { PushDeviceRepository(appContext, api) }
        val notificationRepo = remember { NotificationRepository(api) }
        var notifyMaster by remember(currentUser?.id) { mutableStateOf(notifyPrefs.master) }
        var notifyNewTask by remember(currentUser?.id) { mutableStateOf(notifyPrefs.newTask) }
        var notifyDeadline by remember(currentUser?.id) { mutableStateOf(notifyPrefs.deadline) }
        var notifyStatus by remember(currentUser?.id) { mutableStateOf(notifyPrefs.statusChange) }
        var notifyLeadMinutes by remember(currentUser?.id) { mutableIntStateOf(notifyPrefs.leadMinutes) }
        var notificationPermissionRevision by rememberSaveable { mutableIntStateOf(0) }
        val reminderQueue = remember(currentUser?.id, notifyMaster) { mutableStateListOf<ReminderEvent>() }
        // Android 13+ 需运行时申请通知权限；提醒默认开启，首次登录后询问一次，被拒也不影响页内弹窗
        val notifyPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            notifyPrefs.permissionRequested = true
            notificationPermissionRevision++
        }
        fun requestNotifyPermission() {
            if (NotificationHelper.canPostNotifications(appContext)) {
                notificationPermissionRevision++
                return
            }
            if (NotificationHelper.needsRuntimePermission()) notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // 启动恢复会话：已有 token 时用 GET /me 校验；失败（含 401）回到登录页
        LaunchedEffect(Unit) {
            currentUser = if (session.token != null) runCatching { auth.me() }.getOrNull() else null
            authChecked = true
        }
        // 任意带 token 请求返回 401：清理会话并回登录（切换到主线程更新状态）
        var openedNotificationId by rememberSaveable { mutableStateOf<String?>(null) }
        var notificationOpenSequence by rememberSaveable { mutableIntStateOf(0) }
        LaunchedEffect(notificationOpenEvents) {
            notificationOpenEvents.collect { id ->
                openedNotificationId = id
                notificationOpenSequence++
                route = Route.HOME.name
                detail = null
                homeDetail = null
            }
        }
        DisposableEffect(appContext) {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context, intent: android.content.Intent) { notificationPermissionRevision++ }
            }
            ContextCompat.registerReceiver(appContext, receiver, android.content.IntentFilter(PandoraPushReceiver.REGISTRATION_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
            onDispose { appContext.unregisterReceiver(receiver) }
        }
        LaunchedEffect(currentUser?.id, session.token, notifyMaster, notificationPermissionRevision) {
            if (currentUser == null) return@LaunchedEffect
            if (!notifyMaster || !NotificationHelper.canPostNotifications(appContext)) {
                runCatching { pushDeviceRepo.deactivate() }
                return@LaunchedEffect
            }
            if (BuildConfig.JPUSH_APP_KEY.isBlank()) return@LaunchedEffect
            repeat(30) {
                val registrationId = runCatching {
                    withContext(Dispatchers.IO) { JPushInterface.getRegistrationID(appContext) }
                }.getOrNull()
                if (!registrationId.isNullOrBlank()) {
                    try {
                        pushDeviceRepo.register(registrationId)
                        return@LaunchedEffect
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) { /* Retry a transient registration failure. */ }
                }
                kotlinx.coroutines.delay(2_000)
            }
        }
        DisposableEffect(api) {
            val main = Handler(Looper.getMainLooper())
            api.onSessionExpired = { main.post { currentUser = null; showSettings = false } }
            onDispose { api.onSessionExpired = null }
        }
        val calendarRepo = remember { CalendarRepository(api) }
        val journalRepo = remember { JournalRepository(api) }
        val selectionRepo = remember { SelectionRepository(api) }
        val taskRepo = remember { TaskRepository(api) }
        val userRepo = remember { UserRepository(api) }
        val tagRepo = remember { TagRepository(api) }
        val appScope = rememberCoroutineScope()
        fun refreshTags() {
            appScope.launch {
                runCatching { calendarRepo.tags() }.getOrNull()?.let { list ->
                    serverTags.clear(); serverTags.addAll(list)
                }
            }
        }
        // 待我审批数量：审批中心入口角标 + 底部导航“我的”红点（仅审批能力用户加载；超过一页按 99+ 显示）
        var pendingReviewCount by remember(currentUser?.id, currentUser?.capabilities?.reviewTasks) { mutableStateOf(0) }
        // 登录后、切换页签、关闭审批中心（可能已批准/驳回）时刷新角标
        LaunchedEffect(currentUser?.id, authChecked, currentUser?.capabilities?.reviewTasks, current, showReviews) {
            pendingReviewCount = 0
            if (currentUser != null && authChecked && currentUser?.capabilities?.reviewTasks == true) {
                try {
                    val page = taskRepo.reviews("pending", limit = 100)
                    coroutineContext.ensureActive()
                    pendingReviewCount = if (page.nextCursor != null) 99 else page.items.size
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    pendingReviewCount = 0
                }
            }
        }
        // 登录后加载企业标签目录与本人统计
        var statistics by remember { mutableStateOf<StatisticsDto?>(null) }
        LaunchedEffect(currentUser?.id) {
            taskItems.clear()
            taskDtoById.clear()
            serverTags.clear()
            statistics = null
            progressTaskId = null
            if (currentUser == null) return@LaunchedEffect
            runCatching { calendarRepo.tags() }.getOrNull()?.let { list ->
                serverTags.clear(); serverTags.addAll(list)
            }
            runCatching { calendarRepo.statistics() }.getOrNull()?.let { statistics = it }
        }
        // 提醒默认开启：首次登录后申请一次系统通知权限（Android 13+），被拒只失去系统通知，页内弹窗照常
        LaunchedEffect(currentUser?.id, notifyMaster) {
            if (currentUser == null || !notifyMaster || notifyPrefs.permissionRequested) return@LaunchedEffect
            notifyPrefs.permissionRequested = true
            requestNotifyPermission()
        }
        // 提醒轮询：登录后按固定间隔比对本人任务快照，产出弹窗（+ 有权限时发系统通知）
        LaunchedEffect(currentUser?.id, notifyMaster, notifyNewTask, notifyDeadline, notifyStatus, notifyLeadMinutes) {
            // 权限只在用户主动开启提醒时申请（见设置页），进入软件不打扰
            if (currentUser == null || !notifyMaster) return@LaunchedEffect
            val reminderOwner = currentUser!!.id
            NotificationHelper.createChannel(appContext)
            while (true) {
                val events = runCatching {
                    collectReminders(calendarRepo, notifyPrefs, currentUser?.businessTimezone)
                }.getOrElse {
                    if (it is kotlinx.coroutines.CancellationException) throw it
                    emptyList()
                }
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                if (currentUser?.id != reminderOwner || !notifyMaster) return@LaunchedEffect
                events.forEach { event ->
                    NotificationHelper.notify(appContext, NotificationHelper.idOf(event.key), event.title, event.message)
                    reminderQueue.add(event)
                }
                kotlinx.coroutines.delay(REMINDER_POLL_MILLIS)
            }
        }
        // 视图页：按当前模式与日期加载真实任务；日/周/月走 /calendar，年视图按 /tasks 全年区间查询
        var calLoading by remember { mutableStateOf(false) }
        var calError by remember { mutableStateOf<String?>(null) }
        // 非阻断提示（如权限自动切换），重载后仍保留，用户手动操作后清除
        var calNotice by remember { mutableStateOf<String?>(null) }
        var calRetry by remember { mutableStateOf(0) }
        // 所有视图任务范围：mine=我的任务 / managed=我管理的 / created=我创建的
        var taskScope by rememberSaveable { mutableStateOf("mine") }
        val calendarAnchor = calendarQueryDate(CalendarMode.valueOf(modeName), date)
        LaunchedEffect(current, modeName, calendarAnchor, currentUser?.id, calRetry, taskScope) {
            if (current != Route.VIEW || currentUser == null) return@LaunchedEffect
            calLoading = true; calError = null
            val anchor = "%04d-%02d-%02d".format(calendarAnchor.year, calendarAnchor.month, calendarAnchor.day)
            try {
                val loaded = when (CalendarMode.valueOf(modeName)) {
                    CalendarMode.DAY -> calendarRepo.calendar("day", anchor, scope = taskScope).tasks
                    CalendarMode.WEEK -> calendarRepo.calendar("week", anchor, scope = taskScope).tasks
                    CalendarMode.MONTH -> calendarRepo.calendar("month", anchor, scope = taskScope).tasks
                    CalendarMode.YEAR -> calendarRepo.tasks(scope = taskScope, from = "%04d-01-01".format(date.year), to = "%04d-12-31".format(date.year)).items
                }
                coroutineContext.ensureActive()
                taskItems.clear(); taskItems.addAll(loaded.map { it.toTaskItem() })
                taskDtoById.clear(); taskDtoById.putAll(loaded.associateBy { it.id })
                calNotice = null
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: Exception) {
                // 加载失败显示空视图与重试入口，不保留过期内容
                val api = e as? ApiException
                // 无权限查看当前范围（如"我管理的"）：给出具体提示并切回"我的任务"，重试无意义
                if (api?.httpStatus == 403 && taskScope == "managed") {
                    calNotice = "当前账号没有查看「我管理的」任务的权限，已切回「我的任务」"
                    taskScope = "mine"
                } else {
                    calError = api?.friendlyMessage() ?: "加载失败，请重试"
                }
                taskItems.clear(); taskDtoById.clear()
            } finally {
                if (coroutineContext[kotlinx.coroutines.Job]?.isActive == true) calLoading = false
            }
        }
        // 创建入口由 /me.capabilities 驱动：个人/派发能力分别控制任务类型；具体命令仍由服务端校验
        val canCreatePersonal = currentUser?.capabilities?.createPersonalTasks == true
        val canCreateDelegated = currentUser?.capabilities?.createDelegatedTasks == true
        // 审批中心入口：审核能力，或任何会提交任务申请（个人/派发）的用户都可查看自己的申请
        val canReviewTasks = currentUser?.capabilities?.reviewTasks == true
        val showReviewCenter = canReviewTasks || canCreatePersonal || canCreateDelegated

        when {
            // 启动会话检查中的过渡页
            !authChecked -> Box(Modifier.fillMaxSize().background(theme.background), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GridView, null, Modifier.size(64.dp), tint = WarmOrange)
                    Spacer(Modifier.height(12.dp))
                    Text("正在恢复会话…", color = Ink.copy(alpha = .6f))
                }
            }
            // 未登录：登录页，禁止进入保护页
            currentUser == null -> LoginScreen(auth) { user ->
                currentUser = user
                route = Route.HOME.name; detail = null; homeDetail = null; showSettings = false; showReviews = false
            }
            else -> CompositionLocalProvider(LocalAppTheme provides theme, LocalDensity provides scaledDensity) {
        Scaffold(containerColor = theme.background, bottomBar = { NavigationBar(containerColor = theme.backgroundDeep) { nav.forEach { item -> NavigationBarItem(selected = current == item.route, onClick = { route = item.route.name; detail = null; homeDetail = null; journalDetailId = null; showSettings = false; showReviews = false; showTagManage = false; progressTaskId = null }, icon = { MeNavIcon(item, pendingReviewCount) }, label = { Text(item.label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = WarmOrange, selectedTextColor = WarmOrange, indicatorColor = theme.backgroundDeep)) } } }) { padding ->
            Surface(Modifier.padding(padding).fillMaxSize(), color = theme.background) {
                when {
                    homeDetail != null -> HomeItemDetailScreen(dashboardRepo, homeDetail!!) { homeDetail = null }
                    journalDetailId != null -> JournalDetailScreen(journalRepo, journalDetailId!!, currentUser?.businessTimezone) { journalDetailId = null }
                    showSettings -> SettingsScreen(
                        fontScale = fontScale,
                        onFontScaleChange = { fontScale = it },
                        notificationsEnabled = notifyMaster,
                        onNotificationsChange = { enabled ->
                            notifyMaster = enabled
                            notifyPrefs.master = enabled
                            if (enabled) requestNotifyPermission()
                        },
                        newTaskEnabled = notifyNewTask,
                        onNewTaskChange = { notifyNewTask = it; notifyPrefs.newTask = it },
                        deadlineEnabled = notifyDeadline,
                        onDeadlineChange = { notifyDeadline = it; notifyPrefs.deadline = it },
                        statusChangeEnabled = notifyStatus,
                        onStatusChangeChange = { notifyStatus = it; notifyPrefs.statusChange = it },
                        leadMinutes = notifyLeadMinutes,
                        onLeadMinutesChange = { notifyLeadMinutes = it; notifyPrefs.leadMinutes = it },
                        themeKey = themeKey,
                        onThemeChange = { themeKey = it },
                        auth = auth,
                        onLoggedOut = { currentUser = null },
                        onClose = { showSettings = false }
                    )
                    showReviews -> ReviewScreen(taskRepo, serverTags, currentUser?.businessTimezone, userRepo, canReview = canReviewTasks) { showReviews = false }
                    showTagManage -> TagManageScreen(
                        tagRepo,
                        // 与服务端一致的权限：仅 FOUNDER/DEPARTMENT_MANAGER 可建/改/删；无权限者根本进不了此页
                        TagPermissions(canCreate = tagManager, canUpdate = tagManager, canDelete = tagManager),
                        reloadTags = { refreshTags() }
                    ) { showTagManage = false }
                    detail != null -> {
                        // 列表接口的视图会剥除任务说明/进度记录/完成申请（full=false），打开详情时按 ID 拉取完整数据
                        val detailId = detail!!
                        LaunchedEffect(detailId, currentUser?.id, calRetry) {
                            try {
                                val full = taskRepo.task(detailId)
                                coroutineContext.ensureActive()
                                taskDtoById[full.id] = full
                            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                                throw cancelled
                            } catch (_: Exception) { /* Existing detail remains available for retry. */ }
                        }
                        val dto = taskDtoById[detailId]
                        DetailScreen(detailId, dto, serverTags, taskRepo, onBack = { detail = null }, onChanged = { calRetry++ })
                    }
                    else -> AnimatedContent(current, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { target ->
                        when (target) {
                            Route.HOME -> HomeScreen(dashboardRepo, selectionRepo, notificationRepo, openedNotificationId, notificationOpenSequence, onOpenHome = { homeDetail = it }, onOpenReviews = { showReviews = true })
                            Route.VIEW -> CalendarScreen(CalendarMode.valueOf(modeName), date, taskItems, canCreatePersonal, canCreateDelegated, serverTags, taskRepo, { modeName = it.name }, { dateKey = "${it.year}-${it.month}-${it.day}" }, { calRetry++ }, { detail = it.id }, taskScope = taskScope, onTaskScopeChange = { taskScope = it; calNotice = null }, currentUser = currentUser, canReviewTasks = canReviewTasks, businessTimezone = currentUser?.businessTimezone, loading = calLoading, error = calError, onRetry = { calRetry++ }, notice = calNotice)
                            Route.LOGS -> LogsScreen(journalRepo, serverTags.toList(), currentUser?.businessTimezone) { journalDetailId = it }
                            Route.AI -> AiMapScreen()
                            Route.ME -> ProfileScreen(currentUser!!, statistics, notifyMaster, showReviewCenter, pendingReviewCount, tagManager, userRepo, onOpenTagManage = { showTagManage = true }, onOpenReviews = { showReviews = true }, onOpenSettings = { showSettings = true })
                        }
                    }
                }
                // 提醒弹窗：逐条展示，关闭后显示下一条
                reminderQueue.firstOrNull()?.let { event ->
                    ReminderDialog(event) { if (reminderQueue.isNotEmpty()) reminderQueue.removeAt(0) }
                }
            }
        }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PageHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = { Column { Text(title, fontWeight = FontWeight.Bold); subtitle?.let { Text(it, fontSize = 12.sp, color = Ink.copy(alpha = .6f)) } } }, navigationIcon = { if (onBack != null) IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
}

/** 企业登录页：POST /auth/login；错误密码/网络异常分别提示，成功后进入应用 */
@Composable private fun LoginScreen(auth: AuthRepository, onLoggedIn: (UserDto) -> Unit) {
    var domain by rememberSaveable { mutableStateOf("") }
    var loginName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Cream).verticalScroll(rememberScrollState()).padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(72.dp))
        Icon(Icons.Default.GridView, null, Modifier.size(64.dp), tint = WarmOrange)
        Spacer(Modifier.height(12.dp))
        Text("Pandora", fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("智能掌上日志系统", fontSize = 14.sp, color = Ink.copy(alpha = .6f))
        Spacer(Modifier.height(36.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            OutlinedTextField(domain, { domain = it; error = null }, Modifier.fillMaxWidth(), label = { Text("企业域") }, singleLine = true)
            OutlinedTextField(loginName, { loginName = it; error = null }, Modifier.fillMaxWidth(), label = { Text("登录名") }, singleLine = true)
            OutlinedTextField(password, { password = it; error = null }, Modifier.fillMaxWidth(), label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            Button({
                if (domain.isBlank() || loginName.isBlank() || password.isBlank()) {
                    error = "请填写企业域、登录名和密码"
                } else {
                    loading = true; error = null
                    scope.launch {
                        try {
                            onLoggedIn(auth.login(domain, loginName, password))
                        } catch (e: ApiException) {
                            error = e.friendlyMessage()
                        } catch (e: Exception) {
                            error = "网络异常，请检查网络后重试"
                        } finally {
                            loading = false
                        }
                    }
                }
            }, Modifier.fillMaxWidth().height(50.dp), enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton), shape = RoundedCornerShape(14.dp)) { Text(if (loading) "登录中…" else "登录", fontSize = 16.sp) }
        }
    }
}

@Composable private fun HomeScreen(dashboardRepo: DashboardRepository, selectionRepo: SelectionRepository, notificationRepo: NotificationRepository, openedNotificationId: String?, notificationOpenSequence: Int, onOpenHome: (HomeDetail) -> Unit, onOpenReviews: () -> Unit) {
    var expandedPanel by rememberSaveable { mutableStateOf<String?>(null) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(notificationOpenSequence) {
        if (notificationOpenSequence > 0) notifications = true
    }
    var editingPersonal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf<DashboardDto?>(null) }
    var dashLoading by remember { mutableStateOf(true) }
    var dashError by remember { mutableStateOf<String?>(null) }
    val companyPager = remember { HomePanelPager<CompanyItemDto>() }
    val taskPager = remember { HomePanelPager<TaskDto>() }
    val entryPager = remember { HomePanelPager<WorkEntryDto>() }
    fun loadDashboard() {
        dashLoading = true; dashError = null
        scope.launch {
            try {
                val d = dashboardRepo.dashboard()
                dashboard = d
                companyPager.items.clear(); companyPager.items.addAll(d.companyImportantItems.items)
                companyPager.nextCursor = d.companyImportantItems.nextCursor
                companyPager.moreUrl = d.companyImportantItems.moreUrl
                taskPager.items.clear(); taskPager.items.addAll(d.companyTasks.items)
                taskPager.nextCursor = d.companyTasks.nextCursor
                taskPager.moreUrl = d.companyTasks.moreUrl
                entryPager.items.clear(); entryPager.items.addAll(d.workEntries.items)
                entryPager.nextCursor = d.workEntries.nextCursor
                entryPager.moreUrl = d.workEntries.moreUrl
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理，回到登录页
            } catch (e: Exception) {
                dashError = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
            } finally {
                dashLoading = false
            }
        }
    }
    fun <T> loadMore(pager: HomePanelPager<T>, defaultMoreUrl: String, fetch: suspend (String) -> Pair<List<T>, String?>) {
        val cursor = pager.nextCursor ?: return
        if (pager.loadingMore) return
        pager.loadingMore = true
        scope.launch {
            try {
                val (items, next) = fetch(cursor)
                pager.items.addAll(items)
                pager.nextCursor = next
            } catch (e: SessionExpiredException) {
            } catch (_: Exception) {
                // 翻页失败：保留已加载内容，用户可再次点击重试
            } finally {
                pager.loadingMore = false
            }
        }
    }
    fun loadMoreCompany() = loadMore(companyPager, "/api/v1/company-important-items") { cursor ->
        val page = dashboardRepo.more(companyPager.moreUrl ?: "/api/v1/company-important-items", cursor, CompanyItemPageDto.serializer())
        page.items to page.nextCursor
    }
    fun loadMoreTasks() = loadMore(taskPager, "/api/v1/dashboard/task-items") { cursor ->
        val page = dashboardRepo.more(taskPager.moreUrl ?: "/api/v1/dashboard/task-items", cursor, TaskPageDto.serializer())
        page.items to page.nextCursor
    }
    fun loadMoreEntries() = loadMore(entryPager, "/api/v1/me/important-entry-candidates") { cursor ->
        val page = dashboardRepo.more(entryPager.moreUrl ?: "/api/v1/me/important-entry-candidates", cursor, WorkEntryPageDto.serializer())
        page.items to page.nextCursor
    }
    LaunchedEffect(Unit) { loadDashboard() }
    val d = dashboard
    val panels = listOf(
        HomePanelDef("公司十大重要事项", companyPager.items.map { it.title }, "暂无公司重要事项"),
        HomePanelDef("公司十大派发任务", taskPager.items.map { it.title }, "暂无任务"),
        HomePanelDef("个人十大重要事项", d?.personalImportantEntries?.entries?.map { it.content } ?: emptyList(), "暂无个人重要事项"),
        HomePanelDef("个人日志", entryPager.items.map { it.content }, "暂无日志")
    )
    val panelAccent = LocalAppTheme.current.accent
    fun openItem(panelIndex: Int, index: Int, text: String) {
        val kind: String?
        val id: String?
        when (panelIndex) {
            0 -> { kind = "COMPANY_ITEM"; id = companyPager.items.getOrNull(index)?.id }
            1 -> { kind = "TASK"; id = taskPager.items.getOrNull(index)?.id }
            2 -> { kind = "WORK_ENTRY"; id = d?.personalImportantEntries?.entries?.getOrNull(index)?.id }
            3 -> { kind = "WORK_ENTRY"; id = entryPager.items.getOrNull(index)?.id }
            else -> { kind = null; id = null }
        }
        // 进入详情前收起展开浮层，返回后不会叠着一层
        expandedPanel = null
        onOpenHome(HomeDetail(panels[panelIndex].title, index, text, panelAccent, kind, id))
    }
    Box(Modifier.fillMaxSize().background(LocalAppTheme.current.dashboard)) {
      Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(todayDate().shortLabelWithWeekday(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { notifications = true }) { Surface(shape = RoundedCornerShape(50), color = CreamDeep) { Icon(Icons.Default.Email, "邮箱", Modifier.padding(9.dp), tint = Ink) } }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                dashError != null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(dashError!!, color = Ink.copy(alpha = .7f))
                    Button({ loadDashboard() }, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton), shape = RoundedCornerShape(12.dp)) { Text("重试") }
                }
                d == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = WarmOrange, strokeWidth = 3.dp)
                    Text("正在加载…", color = Ink.copy(alpha = .6f))
                }
                else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomePanel(panels[0], Modifier.weight(1f).fillMaxHeight(), loading = dashLoading, error = dashError, onRetry = { loadDashboard() }, onExpand = { expandedPanel = panels[0].title }, onItemClick = { i, t -> openItem(0, i, t) })
                HomePanel(panels[1], Modifier.weight(1f).fillMaxHeight(), loading = dashLoading, error = dashError, onRetry = { loadDashboard() }, onExpand = { expandedPanel = panels[1].title }, onItemClick = { i, t -> openItem(1, i, t) })
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomePanel(
                    panels[2],
                    Modifier.weight(1f).fillMaxHeight(),
                    loading = dashLoading,
                    error = dashError,
                    onRetry = { loadDashboard() },
                    trailing = {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(WarmOrange.copy(alpha = .35f)).clickable { editingPersonal = true },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Edit, "编辑个人事项", Modifier.size(15.dp), tint = Ink) }
                    },
                    onExpand = { expandedPanel = panels[2].title },
                    onItemClick = { i, t -> openItem(2, i, t) }
                )
                HomePanel(panels[3], Modifier.weight(1f).fillMaxHeight(), loading = dashLoading, error = dashError, onRetry = { loadDashboard() }, onExpand = { expandedPanel = panels[3].title }, onItemClick = { i, t -> openItem(3, i, t) })
            }
                }
            }
        }
      }
      val selected = panels.firstOrNull { it.title == expandedPanel }
      if (selected != null) {
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).clickable { expandedPanel = null })
          Card(Modifier.align(Alignment.Center).fillMaxWidth(.9f).fillMaxHeight(.82f).clickable { }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WarmCard), elevation = CardDefaults.cardElevation(10.dp)) {
              Column(Modifier.fillMaxSize().padding(20.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Box(Modifier.size(10.dp).clip(CircleShape).background(WarmOrange))
                      Spacer(Modifier.width(10.dp))
                      Text(selected.title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                      IconButton({ expandedPanel = null }) { Icon(Icons.Default.Close, "关闭") }
                  }
                  Text("共 ${selected.items.size} 条", color = Ink.copy(alpha = .55f), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 12.dp))
                  Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                      val list = selected.items
                      // 展开后的每一条也可点击进入该事项详情（与面板内点击一致）
                      val panelIndex = panels.indexOfFirst { it.title == selected.title }
                      list.forEachIndexed { i, item ->
                          Row(
                              Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                  .clickable { if (panelIndex >= 0) openItem(panelIndex, i, item) }
                                  .padding(horizontal = 4.dp, vertical = 10.dp),
                              verticalAlignment = Alignment.CenterVertically
                          ) {
                              Box(Modifier.size(30.dp).clip(CircleShape).background(WarmOrange.copy(alpha = .25f)), contentAlignment = Alignment.Center) {
                                  Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                              }
                              Spacer(Modifier.width(12.dp))
                              Text(item, fontSize = 16.sp, lineHeight = 23.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                          }
                          if (i < list.lastIndex) DashedDivider(WarmOrange.copy(alpha = .5f))
                      }
                      // 分页：nextCursor 非空时提供“加载更多”（个人重要事项面板为当前选择，无分页）
                      val pagerState = when (selected.title) {
                          panels[0].title -> companyPager.nextCursor to companyPager.loadingMore
                          panels[1].title -> taskPager.nextCursor to taskPager.loadingMore
                          panels[3].title -> entryPager.nextCursor to entryPager.loadingMore
                          else -> null to false
                      }
                      if (pagerState.first != null) {
                          TextButton({
                              when (selected.title) {
                                  panels[0].title -> loadMoreCompany()
                                  panels[1].title -> loadMoreTasks()
                                  panels[3].title -> loadMoreEntries()
                              }
                          }, Modifier.fillMaxWidth().padding(top = 6.dp)) {
                              Text(if (pagerState.second) "加载中…" else "加载更多", color = WarmOrange, fontWeight = FontWeight.Bold)
                          }
                      }
                  }
              }
          }
      }
      if (editingPersonal) {
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).clickable { editingPersonal = false })
          PersonalImportantEditor(repo = selectionRepo, onClose = { editingPersonal = false }, onSaved = { loadDashboard() })
      }
      if (notifications) {
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).clickable { notifications = false; expandedPanel = null })
          NotificationDrawer(notificationRepo, openedNotificationId, onClose = { notifications = false }, onOpenReviews = { notifications = false; onOpenReviews() })
      }
    }
}

@Composable private fun HomePanel(
    panel: HomePanelDef,
    modifier: Modifier,
    accent: Color = WarmOrange,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
    onExpand: () -> Unit,
    onItemClick: (index: Int, text: String) -> Unit
) {
    val list = panel.items.take(10)
    Card(
        modifier.border(2.dp, accent, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    panel.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                trailing()
            }
            HorizontalDivider(Modifier.padding(top = 7.dp, bottom = 4.dp), color = accent.copy(alpha = .65f))
            // 空 / 加载中 / 加载失败分别给出对应提示，而不是一片空白
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                when {
                    loading && list.isEmpty() -> Column(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("加载中…", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                    }
                    error != null && list.isEmpty() -> Column(
                        Modifier.fillMaxWidth().padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("加载失败", fontSize = 13.sp, color = Ink.copy(alpha = .7f))
                        Text("点击重试", fontSize = 12.sp, color = accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp).clickable(onClick = onRetry))
                    }
                    list.isEmpty() -> Column(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(panel.emptyText, fontSize = 13.sp, color = Ink.copy(alpha = .6f))
                    }
                    else -> list.forEachIndexed { i, text ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                .clickable { onItemClick(i, text) }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${i + 1}. $text",
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (i < list.lastIndex) DashedDivider(accent)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onExpand)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("展开查看 →", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable private fun DashedDivider(color: Color) {
    Spacer(
        Modifier.fillMaxWidth().height(4.dp).drawBehind {
            drawLine(color, Offset(size.width * .25f, size.height / 2), Offset(size.width * .75f, size.height / 2), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 4.dp.toPx())))
        }
    )
}

@Composable private fun PersonalImportantEditor(repo: SelectionRepository, onClose: () -> Unit, onSaved: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableStateOf(0) }
    var selected by remember { mutableStateOf<List<WorkEntryDto>>(emptyList()) }
    var candidates by remember { mutableStateOf<List<WorkEntryDto>>(emptyList()) }
    var expectedVersion by remember { mutableStateOf(0L) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    // 排序模式：隐藏候选区、长按正式事项拖动排序；「取消」恢复原顺序、「完成」应用新顺序
    var sorting by remember { mutableStateOf(false) }
    var showSortHint by remember { mutableStateOf(false) }
    var orderBeforeSort by remember { mutableStateOf<List<WorkEntryDto>>(emptyList()) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val rowHeightPx = with(density) { 52.dp.toPx() }
    val scope = rememberCoroutineScope()
    val accent = WarmOrange
    fun load() {
        loading = true; loadError = null
        scope.launch {
            try {
                val sel = repo.selection()
                selected = sel.entries
                expectedVersion = sel.version
                candidates = repo.candidates().items
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: Exception) {
                loadError = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
            } finally {
                loading = false
            }
        }
    }
    LaunchedEffect(retry) { load() }
    val selectedIds = selected.map { it.entryId.ifBlank { it.id } }
    // 已勾选的候选不再出现在候选区：打钩后即移入正式事项
    val visibleCandidates = candidates.filter { (it.entryId.ifBlank { it.id }) !in selectedIds }
    fun startSorting() {
        orderBeforeSort = selected
        draggingIndex = null
        dragOffset = 0f
        sorting = true
    }
    fun cancelSorting() {
        selected = orderBeforeSort
        sorting = false
        draggingIndex = null
        dragOffset = 0f
    }
    fun toggle(entry: WorkEntryDto) {
        val id = entry.entryId.ifBlank { entry.id }
        if (id in selectedIds) selected = selected.filter { it.entryId.ifBlank { it.id } != id }
        else if (selected.size < 10) selected = selected + entry
    }
    fun save() {
        saving = true; saveError = null
        scope.launch {
            try {
                val result = repo.save(selectedIds, expectedVersion)
                expectedVersion = result.version
                selected = result.entries
                onSaved()
                onClose()
            } catch (e: SessionExpiredException) {
            } catch (e: ApiException) {
                saveError = e.friendlyMessage()
                // 版本冲突：重新读取最新选择，不覆盖他人修改
                if (e.code == "VERSION_CONFLICT") {
                    runCatching { repo.selection() }.getOrNull()?.let { sel ->
                        expectedVersion = sel.version
                        selected = sel.entries
                    }
                }
            } catch (e: Exception) {
                saveError = "网络异常，请重试"
            } finally {
                saving = false
            }
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Card(
            Modifier.fillMaxWidth(.92f).fillMaxHeight(.88f).clickable { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("编辑 · 个人十大重要事项", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClose) { Icon(Icons.Default.Close, "关闭") }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    loading -> Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = accent, strokeWidth = 3.dp)
                        Spacer(Modifier.height(10.dp))
                        Text("正在加载…", color = Ink.copy(alpha = .6f), fontSize = 13.sp)
                    }
                    loadError != null -> Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(loadError!!, color = Ink.copy(alpha = .7f))
                        Button({ retry++ }, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton), shape = RoundedCornerShape(12.dp)) { Text("重试") }
                    }
                    else -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        Text("正式事项", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(4.dp))
                        selected.forEachIndexed { i, item ->
                            val dragging = draggingIndex == i
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (dragging) accent.copy(alpha = .18f) else Color.Transparent)
                                    // 排序模式下长按即可上下拖动换位
                                    .then(
                                        if (sorting) Modifier.pointerInput(i) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = { draggingIndex = i; dragOffset = 0f },
                                                onDragEnd = { draggingIndex = null; dragOffset = 0f },
                                                onDragCancel = { draggingIndex = null; dragOffset = 0f },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    dragOffset += dragAmount.y
                                                    var from = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                    while (dragOffset >= rowHeightPx && from < selected.lastIndex) {
                                                        selected = selected.toMutableList().apply { add(from + 1, removeAt(from)) }
                                                        dragOffset -= rowHeightPx
                                                        draggingIndex = from + 1
                                                        from += 1
                                                    }
                                                    from = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                                    while (dragOffset <= -rowHeightPx && from > 0) {
                                                        selected = selected.toMutableList().apply { add(from - 1, removeAt(from)) }
                                                        dragOffset += rowHeightPx
                                                        draggingIndex = from - 1
                                                        from -= 1
                                                    }
                                                }
                                            )
                                        } else Modifier
                                    )
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(accent.copy(alpha = .3f)), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.content, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                // 排序中不提供移出，避免与拖动冲突
                                if (!sorting) IconButton({ toggle(item) }, Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Close, "移出", Modifier.size(18.dp), tint = CoralDark)
                                }
                            }
                            if (i < selected.lastIndex) DashedDivider(accent)
                        }
                        // 排序模式下不显示候选区，只专注于正式事项排序
                        if (!sorting) {
                            // 分割线：上为正式事项，下为候选事项
                            Spacer(Modifier.height(14.dp))
                            DashedDivider(accent)
                            Spacer(Modifier.height(10.dp))
                            Text("候选事项", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.height(4.dp))
                        }
                        if (!sorting) visibleCandidates.forEach { entry ->
                            val canAdd = selected.size < 10
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 勾选圆形：打钩即加入正式事项，并随即从候选区移除
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .background(if (canAdd) accent.copy(alpha = .12f) else Color.Transparent)
                                        .border(1.5.dp, if (canAdd) accent else HairLine, CircleShape)
                                        .clickable(enabled = canAdd) { toggle(entry) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Add, "加入正式事项", Modifier.size(16.dp), tint = if (canAdd) Ink else Ink.copy(alpha = .3f))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    entry.content,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(8.dp))
                // 左下：排序 / 取消；右下：保存 / 完成
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (sorting) {
                        TextButton(
                            { cancelSorting() },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, accent)
                        ) { Text("取消", color = Ink.copy(alpha = .65f)) }
                    } else {
                        TextButton(
                            { showSortHint = true },
                            enabled = selected.size > 1,
                            shape = RoundedCornerShape(12.dp),
                            // 描边让它看起来像按钮，而不是一段可点的文字
                            border = BorderStroke(1.dp, accent.copy(alpha = if (selected.size > 1) .9f else .3f))
                        ) {
                            Text("排序", color = if (selected.size > 1) Ink else Ink.copy(alpha = .35f))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        { if (sorting) { sorting = false; draggingIndex = null; dragOffset = 0f } else save() },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink),
                        shape = RoundedCornerShape(12.dp)
                    ) { Text(if (sorting) "完成" else if (saving) "保存中…" else "保存", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
    if (showSortHint) AlertDialog(
        onDismissRequest = { showSortHint = false },
        title = { Text("排序", fontWeight = FontWeight.Bold) },
        text = { Text("长按正式事项即可自由排序。", color = Ink.copy(alpha = .8f)) },
        confirmButton = { TextButton({ showSortHint = false; startSorting() }) { Text("确定", color = Ink, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton({ showSortHint = false }) { Text("取消", color = Ink.copy(alpha = .6f)) } }
    )
}

@Composable private fun HomeItemDetailScreen(repo: DashboardRepository, detail: HomeDetail, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    var content by remember(detail.id) { mutableStateOf<String?>(null) }
    var loading by remember(detail.id) { mutableStateOf(detail.id != null) }
    var error by remember(detail.id) { mutableStateOf<String?>(null) }
    var attempt by remember(detail.id) { mutableStateOf(0) }
    LaunchedEffect(detail.id, attempt) {
        val id = detail.id ?: return@LaunchedEffect
        loading = true; error = null
        try {
            content = when (detail.kind) {
                // 公司重要事项全文
                "COMPANY_ITEM" -> repo.companyItem(id).let { it.content.ifBlank { it.title } }
                // 任务详情：标题 + 说明（说明全文仅在详情中提供）
                "TASK" -> repo.task(id).let { t -> buildString {
                    append(t.title)
                    t.description?.takeIf { it.isNotBlank() }?.let { append("\n\n"); append(it) }
                } }
                // 单条工作条目全文（entryId 不是 journalId）
                else -> repo.workEntry(id).content
            }
        } catch (e: SessionExpiredException) {
            // 401 已由全局回调处理
        } catch (e: Exception) {
            error = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
        } finally {
            loading = false
        }
    }
    Column(Modifier.fillMaxSize().background(LocalAppTheme.current.dashboard)) {
        PageHeader(detail.section, onBack = onBack)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(20.dp)
        ) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = WarmCard),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Box(
                        Modifier.align(Alignment.CenterHorizontally).size(48.dp).clip(CircleShape).background(detail.accent.copy(alpha = .3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${detail.index + 1}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                    }
                    when {
                        loading -> Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CircularProgressIndicator(color = WarmOrange, strokeWidth = 3.dp)
                            Text("正在加载全文…", color = Ink.copy(alpha = .6f))
                        }
                        error != null -> Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            Button({ attempt++ }, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton), shape = RoundedCornerShape(12.dp)) { Text("重试") }
                        }
                        else -> Text(content ?: detail.text, fontSize = 19.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}


/** 编辑中的一条工作条目：entryId 非空表示修改已有条目，空表示新增（发布时生成 clientEntryKey） */
private data class EntryDraft(val entryId: String?, val content: String)

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun LogsScreen(repo: JournalRepository, availableTags: List<TagDto>, businessTimezone: String?, onOpen: (String) -> Unit) {
    var expandedLogs by rememberSaveable { mutableStateOf(false) }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    // null = 全部日期；否则为具体业务日期（iso），避免用展示文案做筛选键
    var historyFilter by rememberSaveable { mutableStateOf<String?>(null) }
    // 日期选择器着色用：全部有日志的日期
    var loggedDates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var journals by remember { mutableStateOf<List<JournalDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadRetry by remember { mutableStateOf(0) }
    var writing by rememberSaveable { mutableStateOf(false) }
    var editingJournal by remember { mutableStateOf<JournalDto?>(null) }
    var entryDrafts by remember { mutableStateOf(listOf(EntryDraft(null, ""))) }
    var publishing by remember { mutableStateOf(false) }
    var publishError by remember { mutableStateOf<String?>(null) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    // 编辑器业务日期（最近三日可选）；删除确认目标与错误
    var editorDateKey by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showTagPicker by rememberSaveable { mutableStateOf(false) }
    var deletingJournal by remember { mutableStateOf<JournalDto?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val mutedColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .7f), unfocusedBorderColor = Ink.copy(alpha = .3f), focusedLabelColor = Ink, cursorColor = Ink)
    // 日报草稿（缓存目录）：上次编辑未发布的内容
    val context = LocalContext.current
    val savedJournalDraft = remember {
        AppCache.readDraft(context, "journal_draft.json")?.let { s -> runCatching { AppCache.json.decodeFromString<JournalDraftDto>(s) }.getOrNull() }
    }
    // 编辑器打开期间草稿实时落盘；发布成功或放弃时清除
    LaunchedEffect(writing, editorDateKey, entryDrafts, tags) {
        if (writing) AppCache.writeDraft(
            context, "journal_draft.json",
            AppCache.json.encodeToString(
                JournalDraftDto.serializer(),
                JournalDraftDto(editorDateKey, entryDrafts.map { JournalDraftEntryDto(it.entryId, it.content) }, tags.mapNotNull { n -> availableTags.firstOrNull { it.name == n }?.id })
            )
        )
    }
    fun businessDate(): DemoDate = com.pandora6ix.app.data.journalBusinessDate(businessTimezone)
    fun todayIso(): String { val t = businessDate(); return "%04d-%02d-%02d".format(t.year, t.month, t.day) }
    fun isoOf(d: DemoDate): String = "%04d-%02d-%02d".format(d.year, d.month, d.day)
    fun threeDays(): List<DemoDate> { val t = businessDate(); return listOf(t, t.plusDays(-1), t.plusDays(-2)) }
    fun loadJournals() {
        loading = true; loadError = null
        scope.launch {
            try {
                val t = businessDate()
                val start = t.plusDays(-30)
                val from = "%04d-%02d-%02d".format(start.year, start.month, start.day)
                val to = "%04d-%02d-%02d".format(t.year, t.month, t.day)
                journals = repo.journals(from = from, to = to).items.sortedByDescending { it.workDate }
                // 日期选择器需要全部历史日期（列表只取最近 31 天）
                runCatching { repo.dates().items.toSet() }.getOrNull()?.let { loggedDates = it }
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: Exception) {
                loadError = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
            } finally {
                loading = false
            }
        }
    }
    LaunchedEffect(loadRetry) { loadJournals() }
    // 日历可选 31 天以外的历史日期：列表里没有该日期时按日期单独取一次
    LaunchedEffect(historyFilter) {
        val key = historyFilter ?: return@LaunchedEffect
        if (journals.any { it.workDate == key }) return@LaunchedEffect
        runCatching { repo.journals(date = key).items }.getOrNull()?.let { extra ->
            if (extra.isNotEmpty()) journals = (journals + extra).distinctBy { it.id }
        }
    }
    val todayJournal = journals.firstOrNull { it.workDate == todayIso() }
    fun dateLabel(iso: String): String = parseIsoDate(iso)?.shortLabel() ?: iso
    // 按整篇日志出卡片：日期/标签/完成的工作与编辑删除在同一张卡片内，不再与上方重复
    val filteredJournals = historyFilter?.let { key -> journals.filter { it.workDate == key } } ?: journals
    fun openEditor(dateKey: String) {
        if (businessTimezone.isNullOrBlank()) { publishError = "请管理员先配置公司业务时区"; return }
        val draft = savedJournalDraft?.takeIf { it.dateKey == dateKey && it.dateKey in threeDays().map(::isoOf) && (it.dateKey == todayIso() || journals.none { j -> j.workDate == it.dateKey }) }
        editorDateKey = draft?.dateKey ?: dateKey
        // 编辑窗口按创建时间（allowedActions 由后端按 published_at 判定），而不是 work_date
        val existing = journals.firstOrNull { it.workDate == editorDateKey }
        if (existing != null && "EDIT" !in existing.allowedActions) { publishError = "该日志不是当天创建，已不能修改"; return }
        editingJournal = journals.firstOrNull { it.workDate == editorDateKey }
        entryDrafts = draft?.entries?.map { EntryDraft(it.entryId, it.content) }
            ?: editingJournal?.entries?.sortedBy { it.displayOrder }?.map { EntryDraft(it.entryId, it.content) }
            ?: listOf(EntryDraft(null, ""))
        tags = if (draft != null) draft.tagIds.mapNotNull { id -> availableTags.firstOrNull { it.id == id }?.name }
        else editingJournal?.tags?.map { it.name } ?: emptyList()
        publishError = null
        writing = true
    }
    fun switchEditorDate(dateKey: String) {
        if (dateKey == editorDateKey) return
        val existing = journals.firstOrNull { it.workDate == dateKey }
        if (existing != null && "EDIT" !in existing.allowedActions) { publishError = "该日期日志不是当天创建，已不能修改"; return }
        editorDateKey = dateKey
        editingJournal = journals.firstOrNull { it.workDate == dateKey }
        entryDrafts = editingJournal?.entries?.sortedBy { it.displayOrder }?.map { EntryDraft(it.entryId, it.content) } ?: listOf(EntryDraft(null, ""))
        tags = editingJournal?.tags?.map { it.name } ?: emptyList()
        publishError = null
    }
    fun closeEditor() {
        if (entryDrafts.any { it.content.isNotBlank() }) confirmExit = true else writing = false
    }
    fun publish() {
        if (editorDateKey !in threeDays().map(::isoOf) || (editingJournal != null && "EDIT" !in editingJournal!!.allowedActions)) { publishError = "只能补交昨天和前天的日志；日志仅在创建当天可修改"; return }
        val valid = entryDrafts.filter { it.content.isNotBlank() }
        // 从草稿列表移除的既有条目 = 删除（PATCH deleteEntryIds）；不在原条目上追加新条目代替删改
        val deleteIds = editingJournal?.entries
            ?.mapNotNull { e -> e.entryId.ifBlank { null } }
            ?.filter { id -> entryDrafts.none { it.entryId == id } }
            ?: emptyList()
        if (valid.isEmpty() && deleteIds.isEmpty()) { publishError = "请至少填写一条工作内容"; return }
        if (valid.isEmpty() && editingJournal != null && deleteIds.size == editingJournal!!.entries.size) {
            publishError = "如需删除整篇日志，请使用日志卡片的删除按钮"; return
        }
        // 标签必选：未选标签不允许发布日志
        if (tags.isEmpty()) { publishError = "请至少选择一个标签"; return }
        publishing = true; publishError = null
        scope.launch {
            try {
                val ej = editingJournal
                // 标签（可多选，先选的为主标签）：名称映射为服务端标签 ID
                val tagIdList = tags.mapNotNull { n -> availableTags.firstOrNull { it.name == n }?.id }
                if (ej == null) {
                    try {
                        // 新建一篇（所选日期）：每条携带客户端稳定键
                        repo.create(
                            editorDateKey,
                            valid.map { EntryCreateDto(clientEntryKey = UUID.randomUUID().toString(), content = it.content.trim()) },
                            tagIdList.ifEmpty { null },
                            tagIdList.firstOrNull()
                        )
                    } catch (e: ApiException) {
                        if (e.code == "DAILY_JOURNAL_EXISTS" && e.existingJournalId != null) {
                            // 同日已有日报（如其他设备创建）：并入同一篇继续补充，不新增第二篇
                            val existing = repo.journal(e.existingJournalId!!)
                            if (existing.workDate != todayIso()) { loadJournals(); throw e }
                            repo.patch(
                                existing.journalId.ifBlank { existing.id },
                                JournalPatchDto(
                                    expectedVersion = existing.version,
                                    upsertEntries = valid.map { EntryUpsertDto(clientEntryKey = UUID.randomUUID().toString(), content = it.content.trim()) },
                                    tagIds = tagIdList,
                                    primaryTagId = tagIdList.firstOrNull()
                                )
                            )
                        } else throw e
                    }
                } else {
                    // 编辑：已有条目带 entryId（原条目修改），新增条目带 clientEntryKey；移除的条目走 deleteEntryIds
                    val upserts = valid.map { d ->
                        EntryUpsertDto(
                            entryId = d.entryId,
                            clientEntryKey = if (d.entryId == null) UUID.randomUUID().toString() else null,
                            content = d.content.trim()
                        )
                    }
                    repo.patch(
                        ej.journalId.ifBlank { ej.id },
                        JournalPatchDto(expectedVersion = ej.version, upsertEntries = upserts, deleteEntryIds = deleteIds.ifEmpty { null }, tagIds = tagIdList, primaryTagId = tagIdList.firstOrNull())
                    )
                }
                writing = false
                AppCache.deleteDraft(context, "journal_draft.json")
                loadJournals()
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: ApiException) {
                publishError = e.friendlyMessage()
                // 版本冲突：重新读取最新一篇，保留用户新增（未发布）的条目，已有条目以服务端为准
                if (e.code == "VERSION_CONFLICT") {
                    val jid = editingJournal?.journalId?.ifBlank { editingJournal?.id } ?: editingJournal?.id
                    if (jid != null) {
                        runCatching { repo.journal(jid) }.getOrNull()?.let { fresh ->
                            editingJournal = fresh
                            entryDrafts = fresh.entries.sortedBy { it.displayOrder }.map { EntryDraft(it.entryId, it.content) } +
                                    entryDrafts.filter { it.entryId == null && it.content.isNotBlank() }
                        }
                    }
                }
            } catch (e: Exception) {
                publishError = "网络异常，请重试"
            } finally {
                publishing = false
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            // 标题行与视图页「周视图」同高（44dp 行、20sp 加粗），下方保留原来的间距
            Row(Modifier.fillMaxWidth().height(44.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("日志", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                // 主操作与视图页「回到今年」同为实心主题按钮
                item { Button({ openEditor(todayIso()) }, Modifier.fillMaxWidth(), enabled = !publishing, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(if (todayJournal != null) "补充今日工作" else "记录今日工作") } }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button({ filterOpen = true }, contentPadding = PaddingValues(horizontal = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = CreamDeep, contentColor = Ink), shape = RoundedCornerShape(12.dp)) {
                            Text(historyFilter?.let { dateLabel(it) } ?: "全部日期")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                }
                if (loading && journals.isEmpty()) item {
                    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = WarmOrange, strokeWidth = 3.dp)
                        Spacer(Modifier.width(12.dp))
                        Text("正在加载…", color = Ink.copy(alpha = .6f))
                    }
                }
                if (loadError != null && journals.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(loadError!!, color = Ink.copy(alpha = .7f))
                        Button({ loadRetry++ }, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton), shape = RoundedCornerShape(12.dp)) { Text("重试") }
                    }
                }
                items(if (expandedLogs) filteredJournals else filteredJournals.take(5)) { j ->
                    JournalCard(
                        journal = j,
                        businessTimezone = businessTimezone,
                        enabled = !publishing,
                        onEdit = { openEditor(j.workDate) },
                        onDelete = { deleteError = null; deletingJournal = j },
                        onOpen = { onOpen(j.journalId.ifBlank { j.id }) }
                    )
                }
                if (filteredJournals.size > 5) item { TextButton({ expandedLogs = !expandedLogs }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.textButtonColors(containerColor = CreamDeep, contentColor = Ink)) { Text(if (expandedLogs) "收起" else "展开") } }
                if (!loading && journals.isNotEmpty() && filteredJournals.isEmpty()) item { Text("所选日期暂无日志", color = Ink.copy(alpha = .6f)) }
                if (!loading && journals.isEmpty() && loadError == null) item { Text("最近 30 天暂无日志，点击上方按钮记录今日工作。", color = Ink.copy(alpha = .6f)) }
            }
        }
        if (filterOpen) JournalDatePickerDialog(
            loggedDates = loggedDates,
            today = todayIso(),
            selected = historyFilter,
            onSelect = { picked -> historyFilter = picked; filterOpen = false; expandedLogs = false },
            onDismiss = { filterOpen = false }
        )
        if (writing) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)).clickable { closeEditor() })
        if (writing) {
            androidx.activity.compose.BackHandler { closeEditor() }
            val imeVisible = WindowInsets.isImeVisible
            // 底部对齐 + 导航栏/键盘避让：弹窗直接落在键盘（或导航栏）上方，不再留出一段空白
            Box(Modifier.fillMaxSize()) {
            Card(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().fillMaxWidth(.9f).fillMaxHeight(if (imeVisible) .92f else .9f).border(2.dp, CreamDeep, RoundedCornerShape(24.dp)).clickable { }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WarmCard)) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(if (editingJournal != null) "编辑日志" else "记录日志", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ closeEditor() }) { Icon(Icons.Default.Close, "关闭") } }
                    Text("日志在创建当天内可修改或删除（按公司业务时区）", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                    if (imeVisible) {
                        Text(parseIsoDate(editorDateKey)?.shortLabelWithWeekday() ?: "", Modifier.padding(top = 2.dp, bottom = 8.dp), color = Ink.copy(alpha = .6f), fontSize = 13.sp)
                    } else {
                        Text("· 日志日期", fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            threeDays().forEach { d ->
                                val key = isoOf(d)
                                val selected = editorDateKey == key
                                Text(
                                    d.shortLabel(),
                                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (selected) WarmOrange else CreamDeep).clickable { switchEditorDate(key) }.padding(vertical = 8.dp),
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Ink else Ink.copy(alpha = .65f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        val editorEditable = editingJournal == null || "EDIT" in editingJournal!!.allowedActions
                        if (!editorEditable) Text("补交日志已提交，不能再次修改。今日的日志可在今日内修改。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
                        Text("· 标签（可多选，先选的为主）", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                        if (tags.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tags.forEach { name ->
                                val tagColor = availableTags.firstOrNull { it.name == name }?.let { tagHexColor(it.colorHex) } ?: CreamDeep
                                FilterChip(selected = true, onClick = { tags = tags - name }, label = { Text(name, fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(containerColor = tagColor, labelColor = Ink, selectedContainerColor = tagColor, selectedLabelColor = Ink))
                            }
                        }
                        OutlinedButton({ showTagPicker = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), border = BorderStroke(1.dp, HairLine), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Icon(Icons.Default.Add, "选择标签", Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("选择标签", fontSize = 13.sp) }
                    }
                    // 「完成的工作」标题放在键盘分支之外：输入时也保留，避免这一节看起来"消失/显示不全"
                    Text("· 完成的工作", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if (imeVisible) 4.dp else 12.dp, bottom = 6.dp))
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        entryDrafts.forEachIndexed { i, draft ->
                            Row(verticalAlignment = Alignment.Top) {
                                OutlinedTextField(draft.content, { value -> entryDrafts = entryDrafts.toMutableList().also { it[i] = draft.copy(content = value) } }, Modifier.weight(1f), placeholder = { Text(if (editingJournal == null) "第 ${i + 1} 条：记录完成的工作" else "第 ${i + 1} 条") }, colors = mutedColors, minLines = 2)
                                if (editingJournal == null && entryDrafts.size > 1) IconButton({ entryDrafts = entryDrafts.filterIndexed { idx, _ -> idx != i } }, Modifier.size(40.dp)) { Icon(Icons.Default.Close, "删除此条", Modifier.size(16.dp)) }
                            }
                        }
                    }
                    publishError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton({ closeEditor() }, Modifier.weight(1f), enabled = !publishing, colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Text("取消") }
                        Button(
                            { publish() },
                            Modifier.weight(1f),
                            enabled = !publishing && (entryDrafts.any { it.content.isNotBlank() } || (editingJournal?.entries?.any { e -> entryDrafts.none { it.entryId == e.entryId } } == true)) && (editingJournal == null || "EDIT" in editingJournal!!.allowedActions),
                            colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (publishing) "发布中…" else "发布") }
                    }
                }
            }
            }
        }
        if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false }, modifier = Modifier.border(2.dp, CreamDeep, RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), containerColor = Color.White, titleContentColor = Ink, textContentColor = Ink.copy(alpha = .75f), title = { Text("放弃未保存内容？", fontWeight = FontWeight.Bold) }, text = { Text("退出后当前填写内容会被清空。") }, confirmButton = { TextButton({ confirmExit = false; entryDrafts = listOf(EntryDraft(null, "")); AppCache.deleteDraft(context, "journal_draft.json"); writing = false }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) { Text("放弃并退出") } }, dismissButton = { TextButton({ confirmExit = false }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) { Text("继续编辑") } })
        if (showTagPicker) AlertDialog(
        onDismissRequest = { showTagPicker = false },
        title = { Text("选择标签") },
        text = {
            Column {
                Text("可多选；先选的为主标签。再次点击已选标签可取消。", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                Spacer(Modifier.height(6.dp))
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    if (availableTags.isEmpty()) Text("暂无标签，可在「我的 → 标签管理」中创建。", fontSize = 13.sp, color = Ink.copy(alpha = .5f))
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
    deletingJournal?.let { j ->
            AlertDialog(
                onDismissRequest = { if (!publishing) deletingJournal = null },
                title = { Text("删除这篇日志？", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${dateLabel(j.workDate)} 的 ${j.entries.size} 条工作记录将被删除，且不可恢复。")
                        deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
                    }
                },
                confirmButton = {
                    if (publishing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = WarmOrange)
                    else TextButton({
                        publishing = true
                        scope.launch {
                            try {
                                repo.delete(j.journalId.ifBlank { j.id })
                                deletingJournal = null
                                loadJournals()
                            } catch (e: SessionExpiredException) {
                                // 401 已由全局回调处理
                            } catch (e: ApiException) {
                                deleteError = e.friendlyMessage()
                            } catch (e: Exception) {
                                deleteError = "网络异常，请重试"
                            } finally {
                                publishing = false
                            }
                        }
                    }, colors = ButtonDefaults.textButtonColors(contentColor = CoralDark)) { Text("删除") }
                },
                dismissButton = { if (!publishing) TextButton({ deletingJournal = null }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) { Text("取消") } }
            )
        }
    }
}

/** 业务日期（YYYY-MM-DD）→ 展示文案 */
private fun isoLabel(iso: String): String = parseIsoDate(iso)?.shortLabel() ?: iso

/** 标签胶囊：底色为服务端 colorHex 的真实颜色；换行铺满，保证全部标签都看得见 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagChips(tags: List<TagDto>, primaryTagId: String?) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        tags.forEach { t ->
            Text(
                if (t.id == primaryTagId) "${t.name} · 主" else t.name,
                fontSize = 11.sp,
                color = Ink,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tagHexColor(t.colorHex)).padding(horizontal = 7.dp, vertical = 3.dp)
            )
        }
    }
}

/**
 * 日志卡片：一篇日志一张 —— 日期与创建时间、标签、完成的工作，以及编辑/删除入口。
 * 编辑与删除放在这张卡片里，不再另设「最近三日」区块，避免同一日期信息重复出现。
 */
@Composable
private fun JournalCard(
    journal: JournalDto,
    businessTimezone: String?,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    val editable = "EDIT" in journal.allowedActions
    // publishedAt 是 UTC 时间戳，按公司业务时区换算，保证与现实时间一致
    val created = businessTimeLabel(journal.publishedAt, businessTimezone)
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .8f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(isoLabel(journal.workDate), fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (created.isNotBlank()) Text(created, fontSize = 12.sp, color = Ink.copy(alpha = .6f))
            }
            if (journal.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                TagChips(journal.tags, journal.primaryTagId)
            }
            Spacer(Modifier.height(8.dp))
            journal.entries.sortedBy { it.displayOrder }.forEachIndexed { i, entry ->
                Text(
                    entry.content,
                    fontSize = 13.sp,
                    color = Ink.copy(alpha = .8f),
                    lineHeight = 20.sp,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (i < journal.entries.lastIndex) Spacer(Modifier.height(4.dp))
            }
            if (editable) {
                // 编辑/删除居中放在工作内容下方
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onEdit, enabled = enabled, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("编辑", color = Ink) }
                    TextButton(onDelete, enabled = enabled, contentPadding = PaddingValues(horizontal = 14.dp)) { Text("删除", color = CoralDark) }
                }
            }
        }
    }
}

/** 日志详情分区：小标题 + 主题卡片 */
@Composable
private fun JournalSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text("· $title", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(6.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = WarmCard)
        ) {
            Column(Modifier.fillMaxWidth().padding(14.dp), content = content)
        }
    }
}

/** 日志详情：创建时间 / 标签 / 完成的工作 三个分区 */
@Composable
private fun JournalDetailScreen(repo: JournalRepository, journalId: String, businessTimezone: String?, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    var journal by remember(journalId) { mutableStateOf<JournalDto?>(null) }
    var loading by remember(journalId) { mutableStateOf(true) }
    var error by remember(journalId) { mutableStateOf<String?>(null) }
    var attempt by remember(journalId) { mutableStateOf(0) }
    LaunchedEffect(journalId, attempt) {
        loading = true; error = null
        try {
            journal = repo.journal(journalId)
        } catch (e: SessionExpiredException) {
            // 401 已由全局回调处理
        } catch (e: Exception) {
            error = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
        } finally {
            loading = false
        }
    }
    val j = journal
    Column(Modifier.fillMaxSize().background(LocalAppTheme.current.dashboard)) {
        // 抬头与视图页「周视图」同高（44dp 行、20sp 加粗）
        Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack, Modifier.size(40.dp)) { Icon(Icons.Default.ArrowBack, "返回") }
            Text("日志详情", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarmOrange, strokeWidth = 3.dp)
            }
            error != null -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error!!, color = Ink.copy(alpha = .7f))
                Spacer(Modifier.height(10.dp))
                Button({ attempt++ }, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton), shape = RoundedCornerShape(12.dp)) { Text("重试") }
            }
            j == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("日志不存在或已被删除。", color = Ink.copy(alpha = .7f))
            }
            else -> Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JournalSection("创建时间") {
                    val created = businessTimeLabel(j.publishedAt, businessTimezone)
                    Text(
                        if (created.isNotBlank()) "${isoLabel(j.workDate)} · $created" else isoLabel(j.workDate),
                        fontSize = 15.sp
                    )
                }
                if (j.tags.isNotEmpty()) JournalSection("标签") { TagChips(j.tags, j.primaryTagId) }
                JournalSection("完成的工作") {
                    val entries = j.entries.sortedBy { it.displayOrder }
                    entries.forEachIndexed { i, entry ->
                        Text(entry.content, fontSize = 14.sp, lineHeight = 22.sp)
                        if (i < entries.lastIndex) Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}

@Composable private fun BoxScope.NotificationDrawer(repository: NotificationRepository, requestedId: String?, onClose: () -> Unit, onOpenReviews: () -> Unit) {
    var records by remember { mutableStateOf<List<NotificationDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(repository) {
        try {
            records = repository.list()
        } catch (_: Exception) {
            loadError = "消息加载失败，请稍后重试"
        } finally {
            loading = false
        }
    }
    LaunchedEffect(requestedId, records) {
        val target = records.firstOrNull { it.id == requestedId && it.readAt == null } ?: return@LaunchedEffect
        if (runCatching { repository.markRead(target.id) }.isSuccess)
            records = records.map { if (it.id == target.id) it.copy(readAt = "read") else it }
    }
    Card(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.75f).fillMaxHeight(), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = WarmCard), elevation = CardDefaults.cardElevation(14.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("消息", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClose) { Icon(Icons.Default.Close, "关闭") }
            }
            when {
                loading -> Text("正在加载消息…", color = Ink.copy(alpha = .6f), modifier = Modifier.padding(vertical = 16.dp))
                loadError != null -> Text(loadError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = 16.dp))
                records.isEmpty() -> Text("暂无消息", color = Ink.copy(alpha = .6f), modifier = Modifier.padding(vertical = 16.dp))
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(records, key = { it.id }) { item ->
                        Card(colors = CardDefaults.cardColors(containerColor = if (item.id == requestedId) CreamDeep else Cream), shape = RoundedCornerShape(8.dp)) {
                            Column(Modifier.fillMaxWidth().clickable {
                                if (item.readAt == null) scope.launch {
                                    if (runCatching { repository.markRead(item.id) }.isSuccess)
                                        records = records.map { if (it.id == item.id) it.copy(readAt = "read") else it }
                                }
                            }.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    if (item.readAt == null) Text("未读", color = WarmOrange, fontSize = 12.sp)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(item.body, color = Ink.copy(alpha = .72f), fontSize = 13.sp)
                                Text(item.createdAt.replace('T', ' ').take(16), color = Ink.copy(alpha = .48f), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                                if (item.title == "审批申请") TextButton(onClick = onOpenReviews, contentPadding = PaddingValues(0.dp)) { Text("查看待审批", color = WarmOrange) }
                            }
                        }
                    }
                }
            }
        }
    }
}
@Composable private fun AiMapScreen() { Column(Modifier.fillMaxSize().padding(16.dp)) { PageHeader("AI地图"); Spacer(Modifier.height(48.dp)); Icon(Icons.Default.Info, null, Modifier.size(70.dp).align(Alignment.CenterHorizontally), tint = Coral); Text("AI 地图", Modifier.fillMaxWidth().padding(top = 18.dp), textAlign = TextAlign.Center, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("功能规划中", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, color = Ink.copy(alpha = .6f)) } }
@Composable private fun ProfileScreen(user: UserDto, statistics: StatisticsDto?, notificationsEnabled: Boolean, showReviewCenter: Boolean, pendingReviewCount: Int, canManageTags: Boolean, userRepo: UserRepository, onOpenTagManage: () -> Unit, onOpenReviews: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val avatarScope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("profile_avatar_" + SessionStore(context).ownerId, Context.MODE_PRIVATE) }
    var avatarType by remember { mutableStateOf(prefs.getString("avatarType", "color") ?: "color") }
    var avatarColorIndex by remember { mutableStateOf(prefs.getInt("avatarColor", 0)) }
    var avatarPath by remember { mutableStateOf(prefs.getString("avatarPath", "") ?: "") }
    var showAvatarMenu by remember { mutableStateOf(false) }
    var showAvatarPalette by remember { mutableStateOf(false) }
    fun saveAvatar(type: String, colorIndex: Int = avatarColorIndex, path: String = avatarPath) {
        avatarType = type; avatarColorIndex = colorIndex; avatarPath = path
        prefs.edit().putString("avatarType", type).putInt("avatarColor", colorIndex).putString("avatarPath", path).apply()
    }
    fun uploadAvatarFile(uri: Uri?) {
        if (uri == null) return
        avatarScope.launch {
            val bitmap = withContext(Dispatchers.IO) { decodeUriThumbnail(context, uri, 256) }
            if (bitmap == null) {
                Toast.makeText(context, "头像上传失败，请重试", Toast.LENGTH_SHORT).show()
                return@launch
            }
            runCatching { userRepo.uploadAvatar(user.id, bitmap) }
                .onFailure { Toast.makeText(context, "头像上传失败，本地已生效", Toast.LENGTH_SHORT).show() }
        }
    }
    fun importImage(uri: Uri?) {
        if (uri == null) return
        runCatching {
            val dir = File(context.filesDir, "avatars").apply { mkdirs() }
            val dest = File(dir, "picked_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)!!.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
            saveAvatar("photo", path = dest.absolutePath)
            // 同步到服务端：审批详情里的请求人/审批人/执行人才能显示真实头像
            uploadAvatarFile(uri)
        }.onFailure { Toast.makeText(context, "导入失败，请重试", Toast.LENGTH_SHORT).show() }
    }
    // 相册浏览：授权后自建媒体库网格（系统按授权范围自动过滤——限制访问=仅所选照片，全部=整个相册）
    var showGallery by remember { mutableStateOf(false) }
    var galleryUris by remember { mutableStateOf(listOf<Uri>()) }
    fun openGalleryBrowser() {
        galleryUris = queryGalleryImages(context)
        showGallery = true
    }
    // 相册权限：Android 14 系统弹窗自带"全部/仅所选照片/不允许"三档；13 走 READ_MEDIA_IMAGES；更早走存储权限
    val galleryPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val allowed = grants[Manifest.permission.READ_MEDIA_IMAGES] == true
                || grants[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true
                || grants[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        if (allowed) openGalleryBrowser()
        else Toast.makeText(context, "未授予相册权限，可在系统设置中重新开启", Toast.LENGTH_SHORT).show()
    }
    fun photoPerms(): Array<String> = (if (Build.VERSION.SDK_INT >= 33) buildList {
        add(Manifest.permission.READ_MEDIA_IMAGES)
        if (Build.VERSION.SDK_INT >= 34) add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
    } else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)).toTypedArray()
    fun pickFromGallery() { galleryPermission.launch(photoPerms()) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraFile?.let { saveAvatar("photo", path = it.absolutePath) }
    }
    fun capturePhoto() {
        runCatching {
            val dir = File(context.filesDir, "avatars").apply { mkdirs() }
            val dest = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dest)
            cameraFile = dest
            takePicture.launch(uri)
        }.onFailure { Toast.makeText(context, "无法启动相机", Toast.LENGTH_SHORT).show() }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) capturePhoto() else Toast.makeText(context, "未授予相机权限，可在系统设置中重新开启", Toast.LENGTH_SHORT).show()
    }
    val displayName = user.displayName.ifBlank { user.name }.ifBlank { "未命名" }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageHeader("我的")
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(20.dp), border = cardBorderIfBlended()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clickable { showAvatarMenu = true }) {
                        AvatarView(avatarType, avatarColorIndex, avatarPath, displayName, 64.dp)
                        Box(Modifier.size(20.dp).align(Alignment.BottomEnd).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PhotoCamera, "更换头像", Modifier.size(12.dp), tint = Color.White)
                        }
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
                        Text(user.jobTitle ?: "未设置职位", color = Ink.copy(alpha = .7f))
                    }
                }
            }
            if (showAvatarMenu) androidx.compose.ui.window.Dialog(onDismissRequest = { showAvatarMenu = false }) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        Text("更换头像", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
                        TextButton({ showAvatarMenu = false; pickFromGallery() }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp)) { Text("从相册选择", color = Ink, fontSize = 16.sp) }
                        TextButton({ showAvatarMenu = false; cameraPermission.launch(Manifest.permission.CAMERA) }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp)) { Text("拍照", color = Ink, fontSize = 16.sp) }
                        TextButton({ showAvatarMenu = false; showAvatarPalette = true }, Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp)) { Text("使用默认头像", color = Ink, fontSize = 16.sp) }
                        TextButton({ showAvatarMenu = false }, Modifier.align(Alignment.End), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 1.dp)) { Text("取消", fontSize = 13.sp, color = Ink.copy(alpha = .6f)) }
                    }
                }
            }
            if (showAvatarPalette) AlertDialog(
                onDismissRequest = { showAvatarPalette = false },
                title = { Text("默认头像", fontWeight = FontWeight.Bold) },
                text = {
                    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(200.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(AvatarPalette.size) { i ->
                            Box(
                                Modifier.size(52.dp).clip(CircleShape).background(AvatarPalette[i])
                                    .border(2.dp, if (avatarType == "color" && avatarColorIndex == i) Ink else Color.Transparent, CircleShape)
                                    .clickable { saveAvatar("color", colorIndex = i); showAvatarPalette = false },
                                contentAlignment = Alignment.Center
                            ) { Text(displayName.take(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
                        }
                    }
                },
                confirmButton = { TextButton({ showAvatarPalette = false }) { Text("完成") } }
            )
            if (showGallery) AlertDialog(
                onDismissRequest = { showGallery = false },
                title = { Text("选择头像照片", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (galleryUris.isEmpty()) "当前没有可访问的照片。" else "仅显示已授权可访问的照片；点击照片设为头像。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
                        if (galleryUris.isEmpty()) {
                            TextButton({ galleryPermission.launch(photoPerms()) }, Modifier.fillMaxWidth()) { Text("重新授权 / 管理可访问的照片", color = WarmOrange) }
                        } else {
                            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(380.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(galleryUris.size) { i ->
                                    val uri = galleryUris[i]
                                    val bmp = remember(uri) { decodeUriThumbnail(context, uri) }
                                    Box(Modifier.size(96.dp).clip(RoundedCornerShape(10.dp)).background(CreamDeep).clickable { importImage(uri); showGallery = false }, contentAlignment = Alignment.Center) {
                                        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        else CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = WarmOrange)
                                    }
                                }
                            }
                            TextButton({ galleryPermission.launch(photoPerms()) }, Modifier.fillMaxWidth()) { Text("管理可访问的照片（增删）", color = WarmOrange) }
                        }
                    }
                },
                confirmButton = { TextButton({ showGallery = false }) { Text("关闭") } }
            )
            statistics?.let { st ->
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(20.dp), border = cardBorderIfBlended()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("任务统计", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        ProfileInfoRow("任务总数", "${st.total}")
                        ProfileInfoRow("已完成", "${st.completed}")
                        ProfileInfoRow("未完成", "${st.uncompleted}")
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(20.dp), border = cardBorderIfBlended()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("组织信息", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    ProfileInfoRow("所属企业", user.enterprise?.name ?: "未分配")
                    DashedDivider(HairLine)
                    ProfileInfoRow("所属部门", user.department ?: user.organization?.name ?: "未分配")
                    DashedDivider(HairLine)
                    // 角色显示中文；未分配直属上级时明确说明（数据来源 /me.supervisorUserId）
                    ProfileInfoRow("当前角色", user.roles.joinToString("、") { roleLabel(it) }.ifBlank { "未分配" })
                    ProfileInfoRow("直属上级", user.supervisorName?.takeIf { it.isNotBlank() } ?: "未配置（请联系管理员设置）")
                }
            }
            SettingsCard {
                // 仅 FOUNDER/DEPARTMENT_MANAGER 可见标签管理入口
                if (canManageTags) {
                    SettingsRow(icon = Icons.Default.Label, title = "标签管理", onClick = onOpenTagManage) {
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Ink.copy(alpha = .4f))
                    }
                    DashedDivider(HairLine)
                }
                if (showReviewCenter) {
                    SettingsRow(icon = Icons.Default.FactCheck, title = "审批中心", onClick = onOpenReviews) {
                        if (pendingReviewCount > 0) {
                            Text(
                                if (pendingReviewCount >= 99) "99+" else "$pendingReviewCount",
                                fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold,
                                modifier = Modifier.clip(CircleShape).background(CoralDark).padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Ink.copy(alpha = .4f))
                    }
                    DashedDivider(HairLine)
                }
                SettingsRow(icon = Icons.Default.Tune, title = "全部设置", onClick = onOpenSettings) {
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Ink.copy(alpha = .4f))
                }
            }
            Text("关于 Pandora", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Pandora v0.1.0\n任务、审批与日志说明可在设置与各功能页查看；使用问题请联系管理员或通过管理团队反馈。", color = Ink.copy(alpha = .6f), lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable private fun ProfileInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(label, Modifier.width(72.dp), color = Ink.copy(alpha = .6f), fontSize = 14.sp)
        Text(value, Modifier.weight(1f), fontSize = 14.sp)
    }
}

/** 角色枚举中文化：与后端角色枚举一一对应（ADMIN/FOUNDER/DEPARTMENT_MANAGER/TEAM_LEAD/EMPLOYEE） */
private fun roleLabel(role: String): String = when (role.uppercase()) {
    "EMPLOYEE" -> "员工"
    "TEAM_LEAD" -> "组长"
    "DEPARTMENT_MANAGER" -> "部门主管"
    "FOUNDER" -> "创始人"
    "ADMIN" -> "管理员"
    else -> role
}
@Composable private fun DetailCard(title: String, body: String, color: Color, maxLines: Int = 2, onClick: () -> Unit = {}) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .78f)), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, fontSize = 13.sp, color = Ink.copy(alpha = .75f), maxLines = maxLines, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun DetailScreen(title: String, dto: TaskDto?, availableTags: List<TagDto>, taskRepo: TaskRepository, onBack: () -> Unit, onChanged: () -> Unit) {
    val isLog = title.startsWith("日志 · ")
    val logContent = if (isLog) title.substringAfter("§", "") else null
    val displayTitle = dto?.title ?: if (isLog) title.substringBefore("§") else title
    val item = dto?.toTaskItem()
    androidx.activity.compose.BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    // 任务说明弹窗：状态标签（已完成/未完成）+ 阶段性内容记录（不做百分比）
    var showDescDialog by remember { mutableStateOf(false) }
    var stageNote by remember { mutableStateOf("") }
    var stageBusy by remember { mutableStateOf(false) }
    var stageError by remember { mutableStateOf<String?>(null) }
    val stageKeys = remember { mutableStateMapOf<String, String>() }
    fun stageKey(action: String, id: String): String = stageKeys.getOrPut("$action:$id") { UUID.randomUUID().toString() }
    fun markCompleted(assignment: AssignmentDto) {
        stageBusy = true; stageError = null
        scope.launch {
            try {
                when {
                    "COMPLETE_PERSONAL" in assignment.allowedActions ->
                        taskRepo.completePersonalTask(dto!!.id, assignment.version, stageKey("complete", assignment.id))
                    "REQUEST_COMPLETION" in assignment.allowedActions ->
                        taskRepo.requestCompletion(assignment.id, assignment.version, stageNote.trim().ifBlank { null }, stageKey("request", assignment.id))
                    else -> { stageError = "当前账号无权变更该任务状态"; stageBusy = false; return@launch }
                }
                stageNote = ""
                onChanged(); stageBusy = false
            } catch (e: ApiException) {
                stageError = e.friendlyMessage()
                if (e.httpStatus == 409) stageKeys["complete:${assignment.id}"] = UUID.randomUUID().toString()
                stageBusy = false
            } catch (e: Exception) {
                stageError = "操作失败，请稍后重试"; stageBusy = false
            }
        }
    }
    fun recordStage(assignment: AssignmentDto) {
        if (stageNote.isBlank()) { stageError = "请输入阶段性内容"; return }
        stageBusy = true; stageError = null
        scope.launch {
            try {
                // 服务端进度接口按百分比提交：这里不向用户暴露百分比，
                // 已完成按 100 提交，其余保持执行关系当前值，仅追加阶段性文字记录
                val percent = if (assignment.executionStatus == "COMPLETED") 100 else assignment.progressPercent
                taskRepo.recordProgress(assignment.id, assignment.version, percent, stageNote.trim(), stageKey("progress", assignment.id))
                stageNote = ""
                onChanged(); stageBusy = false
            } catch (e: ApiException) {
                stageError = e.friendlyMessage()
                if (e.httpStatus == 409) stageKeys["progress:${assignment.id}"] = UUID.randomUUID().toString()
                stageBusy = false
            } catch (e: Exception) {
                stageError = "操作失败，请稍后重试"; stageBusy = false
            }
        }
    }
    Column(Modifier.fillMaxSize()) {
        PageHeader(displayTitle, null, onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 20.dp)) {
            if (dto != null && dto.tags.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    dto.tags.forEach { tag ->
                        val tagColor = runCatching { Color(android.graphics.Color.parseColor(tag.colorHex)) }.getOrDefault(Ink.copy(alpha = .18f))
                        Text(if (tag.id == dto.primaryTagId) "${tag.name} · 主" else tag.name, Modifier.clip(RoundedCornerShape(10.dp)).background(tagColor).padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, color = Ink)
                    }
                }
                Spacer(Modifier.height(22.dp))
            }
            if (!isLog) { Text("详情", fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)) }
            if (dto != null && item != null) {
                // 任务说明可点开：弹窗内切换任务状态标签并记录阶段性内容
                DetailCard("任务说明 · 点击查看与更新", dto.description?.takeIf { it.isNotBlank() } ?: "暂无备注", Color(0xFFE2E2E2), maxLines = 8) { showDescDialog = true }
                Spacer(Modifier.height(10.dp))
                DetailCard("时间节点", "${item.start.year}/${item.start.month}/${item.start.day}—${item.end.year}/${item.end.month}/${item.end.day}\n${taskTimeLabel(item)}", Color(0xFFE2E2E2))
                Spacer(Modifier.height(10.dp))
                DetailCard("类型与状态", "${kindLabelOf(dto.kind)} · ${reviewStatusLabel(dto.reviewStatus)}", Color(0xFFE2E2E2))
                Spacer(Modifier.height(10.dp))
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E2E2).copy(alpha = .78f)), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("重要程度", fontWeight = FontWeight.Bold)
                        val stars = dto.priorityLevel.coerceIn(1, 5)
                        val meta = priorityMeta(stars)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("★".repeat(stars) + "☆".repeat(5 - stars), color = meta.first, fontSize = 13.sp)
                            Text(meta.second, fontSize = 13.sp, color = Ink.copy(alpha = .75f))
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text("执行情况", fontWeight = FontWeight.Bold)
                if (dto.authorizedAssignments.isEmpty()) Text("暂无可见执行关系", color = Ink.copy(alpha = .6f), lineHeight = 22.sp)
                dto.authorizedAssignments.forEachIndexed { i, a ->
                    Text("${if (dto.authorizedAssignments.size > 1) "执行关系 ${i + 1} · " else ""}${executionStatusLabel(a.executionStatus)}", lineHeight = 22.sp)
                }
            } else if (isLog) {
                Text(logContent ?: "", color = Ink.copy(alpha = .75f), fontSize = 16.sp, lineHeight = 26.sp)
            } else {
                Text("任务不存在或已被移除。", color = Ink.copy(alpha = .75f), lineHeight = 24.sp)
            }
        }
    }
    // 任务说明弹窗：状态标签（已完成/未完成）+ 阶段性内容输入框（不做百分比），已有记录直接展示
    if (showDescDialog && dto != null && item != null) {
        val assignment = dto.authorizedAssignments.firstOrNull()
        val completed = assignment?.executionStatus == "COMPLETED" || (assignment?.progressPercent ?: 0) >= 100
        AlertDialog(
            onDismissRequest = { if (!stageBusy) showDescDialog = false },
            title = { Text("任务说明与进度", fontWeight = FontWeight.Bold) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(dto.description?.takeIf { it.isNotBlank() } ?: "暂无备注", fontSize = 14.sp, lineHeight = 21.sp, color = Ink.copy(alpha = .85f))
                    Spacer(Modifier.height(14.dp))
                    Text("任务状态", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = completed,
                            onClick = { if (!completed && assignment != null) markCompleted(assignment) },
                            label = { Text("已完成") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Mint, selectedLabelColor = Ink)
                        )
                        FilterChip(
                            selected = !completed,
                            onClick = { if (completed) stageError = "已完成状态不可回退；如需调整请联系任务创建者" },
                            label = { Text("未完成") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CreamDeep, selectedLabelColor = Ink)
                        )
                    }
                    if (assignment == null) Text("你没有该任务的执行关系，状态仅供查看。", fontSize = 12.sp, color = Ink.copy(alpha = .55f), modifier = Modifier.padding(top = 6.dp))
                    Spacer(Modifier.height(14.dp))
                    Text("阶段性内容记录", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        stageNote,
                        { stageNote = it; stageError = null },
                        Modifier.fillMaxWidth(),
                        placeholder = { Text("记录目前已完成的阶段性内容…") },
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .65f), unfocusedBorderColor = Ink.copy(alpha = .25f), cursorColor = Ink)
                    )
                    if (assignment != null && "UPDATE_PROGRESS" in assignment.allowedActions) {
                        TextButton({ recordStage(assignment) }, enabled = !stageBusy && stageNote.isNotBlank()) { Text("保存记录", color = WarmOrange, fontWeight = FontWeight.Bold) }
                    } else if (assignment != null) {
                        Text("当前账号无记录进度的权限。", fontSize = 12.sp, color = Ink.copy(alpha = .5f), modifier = Modifier.padding(top = 4.dp))
                    }
                    stageError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                    if (stageBusy) Text("处理中…", fontSize = 12.sp, color = Ink.copy(alpha = .6f), modifier = Modifier.padding(top = 4.dp))
                    val records = dto.progressRecords.filter { !it.note.isNullOrBlank() }.sortedByDescending { it.recordedAt }.take(5)
                    if (records.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text("已有记录", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            records.forEach { r ->
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CreamDeep).padding(10.dp)) {
                                    Text(r.note!!, fontSize = 13.sp, lineHeight = 19.sp)
                                    r.recordedAt?.let { Text(it.substring(0, minOf(16, it.length)).replace("T", " "), fontSize = 11.sp, color = Ink.copy(alpha = .55f)) }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton({ if (!stageBusy) showDescDialog = false }) { Text("关闭", color = Ink) } }
        )
    }
}

@Composable
private fun SettingsScreen(
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    newTaskEnabled: Boolean,
    onNewTaskChange: (Boolean) -> Unit,
    deadlineEnabled: Boolean,
    onDeadlineChange: (Boolean) -> Unit,
    statusChangeEnabled: Boolean,
    onStatusChangeChange: (Boolean) -> Unit,
    leadMinutes: Int,
    onLeadMinutesChange: (Int) -> Unit,
    themeKey: String,
    onThemeChange: (String) -> Unit,
    auth: AuthRepository,
    onLoggedOut: () -> Unit,
    onClose: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    val scope = rememberCoroutineScope()
    var showFontDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showLeadDialog by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    // 真实缓存清点：草稿（任务/日报未发布内容）+ 被替换的旧头像副本
    val context = LocalContext.current
    // 系统通知权限状态（Android 13+）：应用内开关开启 ≠ 系统已授权，需如实提示
    var notifGranted by remember {
        mutableStateOf(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
    }
    val notifPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notifGranted = granted
        if (granted) onNotificationsChange(true) else toast = "未授予系统通知权限，提醒不会展示"
    }
    val toggleMaster: () -> Unit = {
        if (!notificationsEnabled && Build.VERSION.SDK_INT >= 33 && !notifGranted) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onNotificationsChange(!notificationsEnabled)
        }
    }
    val avatarPrefs = remember { context.getSharedPreferences("profile_avatar_" + SessionStore(context).ownerId, Context.MODE_PRIVATE) }
    var cacheVersion by remember { mutableStateOf(0) }
    val cacheItems = remember(cacheVersion) { AppCache.collect(context, avatarPrefs.getString("avatarPath", null)) }
    val totalCache = cacheItems.sumOf { it.size }
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(1800)
            toast = null
        }
    }

    Column(Modifier.fillMaxSize().background(LocalAppTheme.current.dashboard)) {
        PageHeader("设置", onBack = onClose)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp)) {
                SettingsSectionLabel("通用")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.TextFields,
                        title = "字号大小",
                        onClick = { showFontDialog = true }
                    ) {
                        Text(fontScaleLabel(fontScale), color = Ink.copy(alpha = 0.55f), fontSize = 13.sp)
                    }
                    DashedDivider(HairLine)
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "修改登录密码",
                        onClick = { showPasswordDialog = true }
                    )
                    DashedDivider(HairLine)
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "切换主题",
                        onClick = { showThemeDialog = true }
                    ) {
                        Text(appThemes.firstOrNull { it.key == themeKey }?.label ?: "暖米", color = Ink.copy(alpha = 0.55f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                // 通知提醒独立成一级分区：主开关 + 三个同级开关（提前量是截止时间临近的子项）
                SettingsSectionLabel("通知提醒")
                Text(
                    "这里设置的是应用内提醒与系统通知：站内消息暂未开放，审批记录请到「我的 → 审批中心」查看。",
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), fontSize = 12.sp, color = Ink.copy(alpha = .55f), lineHeight = 17.sp
                )
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "通知提醒",
                        subtitle = "默认关闭，开启后按下方分项提醒",
                        onClick = { toggleMaster() }
                    ) {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { toggleMaster() },
                            colors = pandoraSwitchColors()
                        )
                    }
                    // 系统未授权时如实提示并提供直达系统设置的入口
                    if (notificationsEnabled && !notifGranted) {
                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        SettingsRow(
                            icon = Icons.Default.Warning,
                            title = "系统未授权通知",
                            subtitle = "请在系统设置中允许通知，否则提醒不会展示",
                            onClick = { context.startActivity(intent) }
                        ) {
                            Text("去设置", color = WarmOrange, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        DashedDivider(HairLine)
                    }
                    // 总控关闭则全部静默：不展示分项
                    if (notificationsEnabled) {
                        DashedDivider(HairLine)
                        SettingsRow(
                            icon = Icons.Default.Assignment,
                            title = "新任务提醒",
                            subtitle = "你被派发了新任务，或个人任务批准生效时提醒",
                            onClick = { onNewTaskChange(!newTaskEnabled) }
                        ) {
                            ReminderSwitch(newTaskEnabled, onNewTaskChange)
                        }
                        DashedDivider(HairLine)
                        SettingsRow(
                            icon = Icons.Default.Schedule,
                            title = "截止时间临近",
                            subtitle = "任务截止前 ${leadLabel(leadMinutes)}提醒；已完成任务不再提醒",
                            onClick = { onDeadlineChange(!deadlineEnabled) }
                        ) {
                            ReminderSwitch(deadlineEnabled, onDeadlineChange)
                        }
                        // 提前提醒时间是「截止时间临近」的子项：缩进展示
                        if (deadlineEnabled) {
                            SubValueRow(
                                icon = Icons.Default.Timelapse,
                                title = "提前提醒时间",
                                subtitle = "截止前多久提醒，可自定义到分钟",
                                value = leadLabel(leadMinutes),
                                onClick = { showLeadDialog = true }
                            )
                        }
                        DashedDivider(HairLine)
                        SettingsRow(
                            icon = Icons.Default.SyncAlt,
                            title = "任务状态变化",
                            subtitle = "他人更新了你参与任务的进度或验收结果时提醒",
                            onClick = { onStatusChangeChange(!statusChangeEnabled) }
                        ) {
                            ReminderSwitch(statusChangeEnabled, onStatusChangeChange)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                SettingsSectionLabel("数据")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.CleaningServices,
                        title = "清除缓存",
                        onClick = { showClearCacheDialog = true }
                    ) {
                        Text(AppCache.formatSize(totalCache), color = Ink.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(18.dp))
                SettingsSectionLabel("关于")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "关于我们",
                        onClick = { showAboutDialog = true }
                    ) {
                        Text("v0.1.0", color = Ink.copy(alpha = 0.5f), fontSize = 13.sp)
                    }
                    DashedDivider(HairLine)
                    SettingsRow(icon = Icons.Default.Logout, title = "退出登录", isDestructive = true, onClick = { showLogoutDialog = true })
                }
            }
            if (toast != null) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Ink.copy(alpha = 0.88f)),
                        elevation = CardDefaults.cardElevation(6.dp)
                    ) {
                        Text(toast!!, color = Cream, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                    }
                }
            }
        }
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("选择字号大小") },
            text = {
                Column {
                    val options = listOf(0.9f to "小", 1.0f to "标准", 1.15f to "大")
                    options.forEach { (v, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onFontScaleChange(v); showFontDialog = false }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fontScale == v,
                                onClick = { onFontScaleChange(v); showFontDialog = false },
                                colors = RadioButtonDefaults.colors(selectedColor = WarmOrange)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = { TextButton({ showFontDialog = false }) { Text("关闭", color = Ink) } }
        )
    }
    if (showLeadDialog) {
        // 预设之外可自定义到分钟：小时 + 分钟，范围 1 分钟 – 72 小时
        var hourText by remember { mutableStateOf((leadMinutes / 60).toString()) }
        var minuteText by remember { mutableStateOf((leadMinutes % 60).toString()) }
        var leadError by remember { mutableStateOf<String?>(null) }
        fun applyCustom() {
            val hours = hourText.toIntOrNull() ?: 0
            val minutes = minuteText.toIntOrNull() ?: 0
            val total = hours * 60 + minutes
            leadError = when {
                minutes > 59 -> "分钟需在 0–59 之间"
                total < 1 -> "请填写大于 0 的提醒提前量"
                total > NotificationPrefs.MAX_LEAD_MINUTES -> "自定义范围：1 分钟 – 72 小时"
                else -> null
            }
            if (leadError == null) {
                onLeadMinutesChange(total)
                showLeadDialog = false
            }
        }
        AlertDialog(
            onDismissRequest = { showLeadDialog = false },
            title = { Text("提前提醒时间") },
            text = {
                Column {
                    Text("常用时长：", fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
                    REMINDER_LEAD_OPTIONS.forEach { minutes ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onLeadMinutesChange(minutes); showLeadDialog = false }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = leadMinutes == minutes,
                                onClick = { onLeadMinutesChange(minutes); showLeadDialog = false },
                                colors = RadioButtonDefaults.colors(selectedColor = WarmOrange)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("截止前 ${leadLabel(minutes)}", fontSize = 15.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("自定义（精确到分钟）：", fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { input -> hourText = input.filter(Char::isDigit).take(2); leadError = null },
                            label = { Text("小时") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        Spacer(Modifier.width(10.dp))
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { input -> minuteText = input.filter(Char::isDigit).take(2); leadError = null },
                            label = { Text("分钟") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    }
                    if (leadError != null) Text(leadError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(::applyCustom) { Text("确定", color = Ink) } },
            dismissButton = { TextButton({ showLeadDialog = false }) { Text("取消", color = Ink) } }
        )
    }
    if (showPasswordDialog) {
        var oldPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        var pwError by remember { mutableStateOf<String?>(null) }
        var changing by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!changing) showPasswordDialog = false },
            title = { Text("修改密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(oldPw, { oldPw = it; pwError = null }, label = { Text("当前密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !changing)
                    OutlinedTextField(newPw, { newPw = it; pwError = null }, label = { Text("新密码（至少 10 个字符）") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !changing)
                    OutlinedTextField(confirmPw, { confirmPw = it; pwError = null }, label = { Text("确认新密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !changing)
                    pwError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                }
            },
            confirmButton = {
                TextButton({
                    pwError = when {
                        oldPw.isBlank() -> "请输入当前密码"
                        newPw.length < 10 -> "新密码至少 10 个字符"
                        newPw.toByteArray(Charsets.UTF_8).size > 72 -> "新密码过长（最多 72 字节）"
                        newPw == oldPw -> "新密码不能与当前密码相同"
                        newPw != confirmPw -> "两次输入的新密码不一致"
                        else -> null
                    }
                    if (pwError == null) {
                        changing = true
                        scope.launch {
                            try {
                                // 后端 password() 会作废当前会话（末尾 logout(actor)）：
                                // 用登录时保存的企业域+登录名和新密码静默重登，保持在应用内
                                if (auth.changePassword(oldPw, newPw) != null) {
                                    showPasswordDialog = false
                                    toast = "密码修改成功"
                                } else {
                                    // 静默重登失败（网络异常/缺少登录要素）：密码已修改成功，回登录页重新认证
                                    showPasswordDialog = false
                                    onLoggedOut()
                                }
                            } catch (e: ApiException) {
                                pwError = e.friendlyMessage()
                            } catch (e: Exception) {
                                pwError = "网络异常，请重试"
                            } finally {
                                changing = false
                            }
                        }
                    }
                }) { Text(if (changing) "提交中…" else "确定", color = WarmOrange, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton({ if (!changing) showPasswordDialog = false }) { Text("取消", color = Ink.copy(alpha = 0.7f)) } }
        )
    }
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("切换主题") },
            text = {
                Column {
                    appThemes.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onThemeChange(t.key); showThemeDialog = false }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(22.dp).clip(CircleShape).background(t.background).border(1.5.dp, t.backgroundDeep, CircleShape))
                            Spacer(Modifier.width(10.dp))
                            Text(t.label, Modifier.weight(1f), fontSize = 15.sp)
                            RadioButton(selected = t.key == themeKey, onClick = { onThemeChange(t.key); showThemeDialog = false }, colors = RadioButtonDefaults.colors(selectedColor = WarmOrange))
                        }
                    }
                    Text("切换后仅更改背景颜色，演示数据不受影响。", fontSize = 12.sp, color = Ink.copy(alpha = 0.55f))
                }
            },
            confirmButton = { TextButton({ showThemeDialog = false }) { Text("关闭", color = Ink) } }
        )
    }
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除缓存") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cacheItems.isEmpty()) {
                        Text("暂无可清理的缓存。", color = Ink.copy(alpha = .6f))
                    } else {
                        Text("共 ${AppCache.formatSize(totalCache)}，明细：", color = Ink.copy(alpha = .75f))
                        cacheItems.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(item.label, Modifier.weight(1f), fontSize = 13.sp, lineHeight = 18.sp)
                                Text(AppCache.formatSize(item.size), fontSize = 13.sp, color = Ink.copy(alpha = .6f))
                            }
                        }
                        Text("不影响登录状态、设置与当前使用的头像。", fontSize = 12.sp, color = Ink.copy(alpha = .5f))
                    }
                }
            },
            confirmButton = {
                TextButton({
                    cacheItems.forEach { item -> item.files.forEach { runCatching { it.delete() } } }
                    cacheVersion++
                    showClearCacheDialog = false
                    toast = if (totalCache > 0) "已清除 ${AppCache.formatSize(totalCache)}" else "没有需要清除的缓存"
                }) { Text("清除", color = if (totalCache > 0) WarmOrange else Ink.copy(alpha = .4f), fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton({ showClearCacheDialog = false }) { Text("取消", color = Ink.copy(alpha = 0.7f)) } }
        )
    }
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Pandora6ix") },
            text = { Text("版本 v0.1.0\n一个温暖的六维工作生活导图。") },
            confirmButton = { TextButton({ showAboutDialog = false }) { Text("好的", color = WarmOrange, fontWeight = FontWeight.Bold) } }
        )
    }
    if (showLogoutDialog) {
        var loggingOut by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { if (!loggingOut) showLogoutDialog = false },
            title = { Text("退出登录？") },
            text = { Text("退出后需要重新登录才能使用。") },
            confirmButton = { TextButton({
                if (!loggingOut) {
                    loggingOut = true
                    scope.launch {
                        try { auth.logout() } catch (_: Exception) { /* 本地会话仍会清理 */ }
                        showLogoutDialog = false
                        onLoggedOut()
                    }
                }
            }) { Text(if (loggingOut) "退出中…" else "退出", color = CoralDark, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton({ if (!loggingOut) showLogoutDialog = false }) { Text("取消", color = Ink.copy(alpha = 0.7f)) } }
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        color = Ink.copy(alpha = 0.55f),
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = WarmCard),
        elevation = CardDefaults.cardElevation(2.dp),
        border = cardBorderIfBlended()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    isDestructive: Boolean = false,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {}
) {
    val tint = if (isDestructive) CoralDark else WarmOrange
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(19.dp), tint = tint)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = if (isDestructive) CoralDark else Ink)
            if (!subtitle.isNullOrBlank())
                Text(subtitle, fontSize = 12.sp, color = Ink.copy(alpha = 0.5f), lineHeight = 18.sp)
        }
        trailing()
    }
}

/** 通知提醒分项开关：与全局开关同一套配色（关闭态中性灰边圈，不再有彩色描边） */
@Composable
private fun ReminderSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = pandoraSwitchColors()
    )
}

private fun fontScaleLabel(s: Float): String = when {
    s <= 0.9f -> "小"
    s >= 1.15f -> "大"
    else -> "标准"
}

/** 子项取值行（如提前提醒时间）：缩进展示，点击弹出选择弹窗 */
@Composable
private fun SubValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        // 紧凑子项：随「截止时间临近」开关收展，不再占据独立大行
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 50.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(14.dp), tint = WarmOrange.copy(alpha = 0.65f))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 13.sp, color = Ink)
            Text(subtitle, fontSize = 11.sp, color = Ink.copy(alpha = 0.5f), lineHeight = 16.sp)
        }
        Text(value, color = WarmOrange, fontSize = 13.sp)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = Ink.copy(alpha = 0.4f))
    }
}

/** 提醒弹窗：新任务 / 截止临近 / 状态变化统一入口，逐条确认 */
@Composable
private fun ReminderDialog(event: ReminderEvent, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.title, fontWeight = FontWeight.Bold) },
        text = { Text(event.message, color = Ink.copy(alpha = 0.75f), lineHeight = 22.sp) },
        confirmButton = { TextButton(onDismiss) { Text("知道了", color = Ink) } }
    )
}
