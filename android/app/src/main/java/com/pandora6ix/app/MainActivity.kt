package com.pandora6ix.app

import com.pandora6ix.app.ui.components.*
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
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
import com.pandora6ix.app.ui.ai.AiExplorerScreen
import com.pandora6ix.app.ui.review.ReviewScreen
import com.pandora6ix.app.ui.task.ProgressSheet
import com.pandora6ix.app.ui.summary.TaskSummaryFlow
import com.pandora6ix.app.ui.tags.TagManageScreen
import com.pandora6ix.app.ui.logs.JournalDatePickerDialog
import com.pandora6ix.app.ui.logs.JournalEntryField
import com.pandora6ix.app.ui.logs.initialJournalEntries
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
    private val approvalPromptEvents = Channel<ApprovalPrompt>(Channel.BUFFERED)
    private var approvalReceiver: android.content.BroadcastReceiver? = null

    companion object {
        @Volatile var isForeground = false
            private set
    }

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
        setContent { PandoraApp(session, api, auth, notificationOpenEvents.receiveAsFlow(), approvalPromptEvents.receiveAsFlow()) }
    }

    override fun onStart() {
        super.onStart()
        isForeground = true
        if (approvalReceiver != null) return
        approvalReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val prompt = ApprovalPrompt.from(
                    intent.getStringExtra(PandoraPushReceiver.EXTRA_NOTIFICATION_ID),
                    intent.getStringExtra(PandoraPushReceiver.EXTRA_EVENT_TYPE)
                ) ?: return
                approvalPromptEvents.trySend(prompt)
            }
        }.also { receiver ->
            ContextCompat.registerReceiver(
                this,
                receiver,
                android.content.IntentFilter(PandoraPushReceiver.APPROVAL_ARRIVED),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onStop() {
        isForeground = false
        approvalReceiver?.let { unregisterReceiver(it) }
        approvalReceiver = null
        super.onStop()
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

/** 推送前台到达：服务端通知 id + SDK 通知栏 id（用于清掉那条系统通知） */
private data class PushArrival(val notificationId: String, val notifyId: Int)

/** 提醒轮询间隔（App 前台；退到后台暂停，回到前台立刻补一次检查） */
private const val REMINDER_POLL_MILLIS = 10L * 1000L
/** 截止时间提前量预设值（分钟）：1 / 2 / 6 / 12 / 24 小时 */
private val REMINDER_LEAD_OPTIONS = listOf(60, 120, 360, 720, 1440)

/** 服务端通知是否允许弹出提醒：kind 与设置中「通知提醒」三个子开关一一对应 */
private fun NotificationDto.alertEnabled(newTask: Boolean, deadline: Boolean, statusChange: Boolean): Boolean =
    when (kind) {
        ReminderKind.NEW_TASK -> newTask
        ReminderKind.DEADLINE -> deadline
        ReminderKind.STATUS -> statusChange
        else -> false
    }

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

/** 底部导航图标：待审批与未读结果在“我的”图标上挂数字角标（99+ 封顶） */
@Composable
private fun MeNavIcon(item: NavItem, badgeCount: Int) {
    // 卡通主题用手绘位图（不着色）；原主题保持矢量图标
    val art = LocalAppTheme.current.art
    val glyph: @Composable () -> Unit = {
        if (art != null) androidx.compose.foundation.Image(
            androidx.compose.ui.res.painterResource(art.navIcons[item.route.ordinal]), item.label,
            Modifier.size(PandoraDimensions.icon + 4.dp)
        ) else Icon(item.icon, item.label)
    }
    if (item.route == Route.ME && badgeCount > 0) {
        androidx.compose.material3.BadgedBox(badge = { androidx.compose.material3.Badge { Text(if (badgeCount >= 99) "99+" else "$badgeCount") } }) {
            glyph()
        }
    } else {
        glyph()
    }
}

/** 卡通主题场景插画：底部对齐、铺满宽度；视图页优先用专属淡彩插画，否则降低透明度 */
@Composable
private fun ThemeScene(art: ThemeArt, calendar: Boolean, modifier: Modifier = Modifier) {
    val backdrop = art.calendarBackdrop
    if (calendar && backdrop != null) androidx.compose.foundation.Image(
        androidx.compose.ui.res.painterResource(backdrop), null,
        modifier.fillMaxWidth(0.8f).padding(bottom = PandoraSpacing.space8), contentScale = androidx.compose.ui.layout.ContentScale.FillWidth
    ) else androidx.compose.foundation.Image(
        androidx.compose.ui.res.painterResource(art.scene), null,
        modifier.fillMaxWidth(), contentScale = androidx.compose.ui.layout.ContentScale.FillWidth, alpha = if (calendar) 0.35f else 1f
    )
}

@Composable
fun PandoraApp(
    session: SessionStore,
    api: ApiClient,
    auth: AuthRepository,
    notificationOpenEvents: Flow<String>,
    approvalPromptEvents: Flow<ApprovalPrompt>
) {
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
        var approvalPrompt by remember { mutableStateOf<ApprovalPrompt?>(null) }
        // 从消息进入审批中心时定位到具体那条审批
        var reviewFocusId by rememberSaveable { mutableStateOf<String?>(null) }
        // 消息在抽屉之外被置为已读（如直接点开任务详情）后，用于刷新信封角标
        var notificationReadRevision by remember { mutableIntStateOf(0) }
        // 标签管理覆盖层（我的页进入，入口仅对有权限角色显示）
        var showTagManage by rememberSaveable { mutableStateOf(false) }
        // 进度记录浮层：从任务详情进入，显示当前任务的执行关系
        var progressTaskId by remember { mutableStateOf<String?>(null) }
        var fontScale by rememberSaveable { mutableStateOf(1f) }
        var currentUser by remember { mutableStateOf<UserDto?>(null) }
        val appearanceContext = LocalContext.current
        val themePreferences = remember(appearanceContext) {
            val prefs = appearanceContext.getSharedPreferences("pandora_appearance", Context.MODE_PRIVATE)
            ThemePreferences({ key -> prefs.getString(key, null) },
                { key, value -> prefs.edit().putString(key, value).apply() }, appThemes.map { it.key }.toSet())
        }
        val themeOwner = currentUser?.id ?: session.ownerId
        var themeKey by remember(themeOwner) { mutableStateOf(themePreferences.theme(themeOwner)) }

        var showTaskSummary by rememberSaveable(currentUser?.id) { mutableStateOf(false) }
        val calendarUiState = key(currentUser?.id) { rememberSaveableStateHolder() }
        // 子页面替换“我的”时仍保留滚动位置，退出登录或切换账号时重置。
        val profileScrollState = rememberSaveable(currentUser?.id, saver = ScrollState.Saver) { ScrollState(0) }
        var authChecked by remember { mutableStateOf(false) }
        // 标签增改删权限：与服务端一致（仅 FOUNDER/DEPARTMENT_MANAGER）；未来接入 capabilities 后再细分
        val tagManager = currentUser?.capabilities?.createTags == true
        val theme = appThemes.firstOrNull { it.key == themeKey } ?: appThemes.first()
        // 视图页任务与企业标签目录来自真实接口（登录后按模式/日期加载）
        val taskItems = remember { mutableStateListOf<TaskItem>() }
        // 保留服务端原始 TaskDto（执行关系/allowedActions/进度），供详情与进度记录使用
        val serverTags = remember { mutableStateListOf<TagDto>() }
        val date = dateKey.split("-").map(String::toInt).let { DemoDate(it[0], it[1], it[2]) }
        val current = Route.valueOf(route)
        val nav = listOf(
            NavItem(Route.HOME, "首页", Icons.Default.GridView),
            NavItem(Route.VIEW, "公司", Icons.Default.CalendarMonth),
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
        // 每日事项：本地按账号隔离存储（服务端接口后续接入时替换）
        val dailyStore = remember(currentUser?.id) { DailyItemsStore(appContext, currentUser?.id) }
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
        // 推送前台到达事件：id 由 PandoraPushReceiver 广播过来，交给统一提醒队列处理
        val pushArrivedEvents = remember { kotlinx.coroutines.channels.Channel<PushArrival>(kotlinx.coroutines.channels.Channel.UNLIMITED) }
        // 会话内已处理过的通知 id：推送弹过的轮询不再重复弹，反之亦然
        val announcedNotifications = remember(currentUser?.id) { java.util.Collections.synchronizedSet(mutableSetOf<String>()) }
        LaunchedEffect(notificationOpenEvents) {
            notificationOpenEvents.collect { id ->
                openedNotificationId = id
                notificationOpenSequence++
                // 只切到首页并高亮该消息，不再自动拉开消息抽屉
                route = Route.HOME.name
                detail = null
                homeDetail = null
                showTaskSummary = false
            }
        }
        LaunchedEffect(approvalPromptEvents, currentUser?.id, notifyMaster, notifyStatus) {
            approvalPromptEvents.collect { prompt ->
                if (currentUser == null || !notifyMaster || !notifyStatus) return@collect
                // 两个 PR 共用去重记录；审批专用弹窗替换同一消息尚未确认的普通提醒。
                if (claimApprovalPrompt(prompt.notificationId, announcedNotifications, reminderQueue)) approvalPrompt = prompt
            }
        }
        DisposableEffect(appContext) {
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: Context, intent: android.content.Intent) {
                    if (intent.action == PandoraPushReceiver.MESSAGE_ARRIVED)
                        intent.getStringExtra(PandoraPushReceiver.EXTRA_NOTIFICATION_ID)
                            ?.let { id ->
                                pushArrivedEvents.trySend(
                                    PushArrival(id, intent.getIntExtra(PandoraPushReceiver.EXTRA_NOTIFY_ID, -1))
                                )
                            }
                    else notificationPermissionRevision++
                }
            }
            val filter = android.content.IntentFilter(PandoraPushReceiver.REGISTRATION_CHANGED).apply {
                addAction(PandoraPushReceiver.MESSAGE_ARRIVED)
            }
            ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
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
        val taskRepo = remember { TaskRepository(api) }
        val userRepo = remember(currentUser?.id) { UserRepository(api) }
        val tagRepo = remember { TagRepository(api) }
        val aiRepo = remember { AiRepository(api) }
        val appScope = rememberCoroutineScope()
        fun refreshTags() {
            appScope.launch {
                runCatching { calendarRepo.tags() }.getOrNull()?.let { list ->
                    serverTags.clear(); serverTags.addAll(list)
                }
            }
        }
        val reviewReads = remember(currentUser?.id) { ReviewResultReads(appContext, currentUser?.id ?: "anon") }
        var readReviewKeys by remember(reviewReads) { mutableStateOf(reviewReads.keys()) }
        var unreadReviewKeys by remember(reviewReads) { mutableStateOf(emptySet<String>()) }
        var reviewRefreshRevision by remember(reviewReads) { mutableIntStateOf(0) }
        // 两项独立刷新：查询失败保留上次成功结果。
        var pendingReviewCount by remember(currentUser?.id, currentUser?.capabilities?.reviewTasks) { mutableStateOf(0) }
        // 完成验收待办：发起人（派发任务）与任务审批人（个人 / 自派任务）都可能有待验收申请
        var pendingCompletionCount by remember(currentUser?.id) { mutableStateOf(0) }
        val reviewBadgeCount = (pendingReviewCount + pendingCompletionCount + (unreadReviewKeys - readReviewKeys).size).coerceAtMost(99)
        LaunchedEffect(currentUser?.id, authChecked, currentUser?.capabilities?.reviewTasks, current, showReviews) {
            if (currentUser == null || !authChecked) return@LaunchedEffect
            (appContext as ComponentActivity).lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    if (currentUser?.capabilities?.reviewTasks == true) {
                        try {
                            val page = taskRepo.reviews("pending", limit = 100)
                            coroutineContext.ensureActive()
                            pendingReviewCount = if (page.nextCursor != null) 99 else page.items.size
                        } catch (cancelled: kotlinx.coroutines.CancellationException) {
                            throw cancelled
                        } catch (_: Exception) { }
                    }
                    try {
                        pendingCompletionCount = taskRepo.completionPendingCount().count.coerceIn(0, 99).toInt()
                        coroutineContext.ensureActive()
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) { }
                    try {
                        val keys = unreadReviewResults(reviewReads.keys()) { cursor ->
                            taskRepo.reviews("created", cursor = cursor, limit = 100)
                        }
                        coroutineContext.ensureActive()
                        unreadReviewKeys = keys
                        reviewRefreshRevision++
                    } catch (cancelled: kotlinx.coroutines.CancellationException) {
                        throw cancelled
                    } catch (_: Exception) { }
                    kotlinx.coroutines.delay(30_000)
                }
            }
        }
        // 登录后加载企业标签目录
        LaunchedEffect(currentUser?.id) {
            taskItems.clear()
            serverTags.clear()
            progressTaskId = null
            if (currentUser == null) return@LaunchedEffect
            runCatching { calendarRepo.tags() }.getOrNull()?.let { list ->
                serverTags.clear(); serverTags.addAll(list)
            }
        }
        // 提醒默认开启：首次登录后申请一次系统通知权限（Android 13+），被拒只失去系统通知，页内弹窗照常
        LaunchedEffect(currentUser?.id, notifyMaster) {
            if (currentUser == null || !notifyMaster || notifyPrefs.permissionRequested) return@LaunchedEffect
            notifyPrefs.permissionRequested = true
            requestNotifyPermission()
        }
        // 统一出口：轮询发现的新消息与推送前台到达的消息，都从这里发系统通知并入页内弹窗队列
        fun announceNotification(n: NotificationDto) {
            announcedNotifications.add(n.id)
            NotificationHelper.notify(appContext, NotificationHelper.idOf(n.id), n.title, n.body)
            reminderQueue.add(ReminderEvent(kind = n.kind, key = n.id, title = n.title, message = n.body))
        }
        // 提醒轮询：直接读服务端通知（消息一入库，下一轮就产出弹窗），不再依赖本地任务快照比对；
        // repeatOnLifecycle(STARTED)：退到后台暂停轮询，回到前台立刻补一次检查
        LaunchedEffect(currentUser?.id, notifyMaster, notifyNewTask, notifyDeadline, notifyStatus) {
            // 权限只在用户主动开启提醒时申请（见设置页），进入软件不打扰
            if (currentUser == null || !notifyMaster) return@LaunchedEffect
            NotificationHelper.createChannel(appContext)
            // 本轮会话已登记的消息 id：首次只把现有消息作为基线，避免把历史消息一次性弹出来
            var baselineReady = false
            (appContext as ComponentActivity).lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    val fresh = runCatching { notificationRepo.recent() }.getOrElse {
                        if (it is kotlinx.coroutines.CancellationException) throw it
                        emptyList()
                    }
                    coroutineContext.ensureActive()
                    if (!baselineReady) {
                        baselineReady = true
                        announcedNotifications.addAll(fresh.map { it.id })
                    } else {
                        val pending = fresh.filter {
                            it.readAt == null && it.id !in announcedNotifications &&
                                it.alertEnabled(notifyNewTask, notifyDeadline, notifyStatus)
                        }
                        announcedNotifications.addAll(fresh.map { it.id })
                        pending.forEach { n ->
                            announceNotification(n)
                        }
                    }
                    kotlinx.coroutines.delay(REMINDER_POLL_MILLIS)
                }
            }
        }
        // 不论从消息进入还是自己点开：打开任务详情就把该任务的未读消息置为已读，并刷新信封角标
        LaunchedEffect(detail, currentUser?.id) {
            val taskId = detail ?: return@LaunchedEffect
            runCatching {
                notificationRepo.list()
                    .filter { it.taskId == taskId && it.readAt == null }
                    .forEach { notificationRepo.markRead(it.id) }
            }
            notificationReadRevision++
        }
        // 推送前台到达：不必等下一轮轮询，立刻按 id 取服务端这条消息弹出来
        LaunchedEffect(pushArrivedEvents, notifyMaster, notifyNewTask, notifyDeadline, notifyStatus) {
            for (arrival in pushArrivedEvents) {
                // 前台收到推送：SDK 也会往通知栏塞一条，这里清掉，只保留页内弹窗
                if (arrival.notifyId >= 0) runCatching { JPushInterface.clearNotificationById(appContext, arrival.notifyId) }
                val id = arrival.notificationId
                if (currentUser == null || !notifyMaster || id in announcedNotifications) continue
                val n = runCatching { notificationRepo.recent() }.getOrNull()?.firstOrNull { it.id == id } ?: continue
                // 请求期间审批广播可能已先展示该消息，不再重复入队。
                if (id in announcedNotifications) continue
                if (!n.alertEnabled(notifyNewTask, notifyDeadline, notifyStatus)) continue
                announceNotification(n)
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
                taskItems.clear()
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
            !authChecked -> Box(Modifier.fillMaxSize().background(theme.colors.appBackground), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.GridView, null, Modifier.size(64.dp), tint = PandoraTheme.colors.primary)
                    Spacer(Modifier.height(PandoraSpacing.space12))
                    Text("正在恢复会话…", color = PandoraTheme.colors.textTertiary)
                }
            }
            // 未登录：登录页，禁止进入保护页
            currentUser == null -> LoginScreen(auth) { user ->
                currentUser = user
                route = Route.HOME.name; detail = null; homeDetail = null; showSettings = false; showReviews = false
            }
            else -> PandoraTheme(theme) { CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Scaffold(containerColor = theme.colors.appBackground, bottomBar = { NavigationBar(containerColor = theme.art?.navBand ?: theme.colors.appBackground) { nav.forEach { item -> NavigationBarItem(selected = current == item.route, onClick = { route = item.route.name; detail = null; homeDetail = null; journalDetailId = null; showSettings = false; showReviews = false; showTagManage = false; progressTaskId = null; showTaskSummary = false }, icon = { MeNavIcon(item, reviewBadgeCount) }, label = { Text(item.label, style = if (current == item.route && theme.art?.navLabel != null) PandoraTypography.caption.copy(fontWeight = FontWeight.SemiBold) else PandoraTypography.caption) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = theme.colors.primary, selectedTextColor = theme.art?.navLabel ?: theme.colors.primary, indicatorColor = theme.colors.primaryContainer, unselectedIconColor = theme.colors.textSecondary, unselectedTextColor = theme.art?.navLabel ?: theme.colors.textSecondary)) } } }) { padding ->
            Surface(Modifier.padding(padding).fillMaxSize(), color = theme.colors.appBackground) {
                when {
                    homeDetail?.kind == "TASK" && homeDetail?.id != null -> FullTaskDetail(
                        homeDetail!!.id!!, taskRepo, serverTags, currentUser?.businessTimezone, currentUser, userRepo,
                        onBack = { homeDetail = null }, onChanged = { calRetry++ })
                    homeDetail != null -> HomeItemDetailScreen(dashboardRepo, homeDetail!!) { homeDetail = null }
                    journalDetailId != null -> JournalDetailScreen(journalRepo, journalDetailId!!, currentUser?.businessTimezone, currentUser, userRepo) { journalDetailId = null }
                    showTaskSummary -> key(currentUser!!.id) {
                        TaskSummaryFlow(
                            currentUser!!, calendarRepo, taskRepo, journalRepo, userRepo,
                            taskDetail = { id, onBack, onChanged ->
                                FullTaskDetail(id, taskRepo, serverTags, currentUser?.businessTimezone, currentUser, userRepo, onBack) { onChanged(); calRetry++ }
                            },
                            journalDetail = { id, onBack -> JournalDetailScreen(journalRepo, id, currentUser?.businessTimezone, currentUser, userRepo, onBack) },
                            onClose = { showTaskSummary = false }
                        )
                    }
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
                        onThemeChange = { selected ->
                            themeOwner?.let { owner -> themePreferences.select(owner, selected); themeKey = selected }
                        },
                        auth = auth,
                        onLoggedOut = { currentUser = null },
                        onClose = { showSettings = false }
                    )
                    showReviews -> key(currentUser?.id) {
                        ReviewScreen(taskRepo, serverTags, currentUser?.businessTimezone, userRepo,
                            canReview = canReviewTasks, readResultKeys = readReviewKeys, currentUser = currentUser,
                            refreshRevision = reviewRefreshRevision, focusReviewId = reviewFocusId,
                            onResultRead = { review ->
                                reviewReads.markRead(review)
                                readReviewKeys = reviewReads.keys()
                            },
                            onOpenTask = { taskId -> showReviews = false; reviewFocusId = null; detail = taskId }) { showReviews = false; reviewFocusId = null }
                    }
                    showTagManage -> TagManageScreen(
                        tagRepo,
                        // 与服务端一致的权限：仅 FOUNDER/DEPARTMENT_MANAGER 可建/改/删；无权限者根本进不了此页
                        TagPermissions(canCreate = tagManager, canUpdate = tagManager, canDelete = tagManager),
                        reloadTags = { refreshTags() }
                    ) { showTagManage = false }
                    detail != null -> FullTaskDetail(detail!!, taskRepo, serverTags, currentUser?.businessTimezone,
                        currentUser, userRepo, onBack = { detail = null }, onChanged = { calRetry++ })
                    else -> Box(Modifier.fillMaxSize()) {
                      // 卡通主题：一级页面底部的场景插画在内容下层；视图页淡化，不影响日历可读性
                      theme.art?.let { art -> ThemeScene(art, calendar = current == Route.VIEW, modifier = Modifier.align(Alignment.BottomCenter)) }
                      AnimatedContent(current, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { target ->
                        when (target) {
                            Route.HOME -> calendarUiState.SaveableStateProvider("home") { HomeScreen(dashboardRepo, calendarRepo, notificationRepo, dailyStore, openedNotificationId, notificationOpenSequence, notificationReadRevision, businessTimezone = currentUser?.businessTimezone, onDrawerClosed = { openedNotificationId = null }, onOpenHome = { homeDetail = it }, onOpenNotification = { item ->
                                // 只有仍在「待我审批」队列里的审批申请才进审批中心并展开该条；
                                // 审批结果 / 完成验收结果等消息直接看对应任务详情
                                val awaitingReview = item.reviewId != null && item.eventType == "REVIEW_REQUEST" && !item.resolved
                                if (awaitingReview) {
                                    reviewFocusId = item.reviewId
                                    showReviews = true
                                } else if (item.taskId.isNotBlank()) detail = item.taskId
                            }) }
                            Route.VIEW -> calendarUiState.SaveableStateProvider("calendar") { CalendarScreen(CalendarMode.valueOf(modeName), date, taskItems, canCreatePersonal, canCreateDelegated, serverTags, taskRepo, { modeName = it.name }, { dateKey = "${it.year}-${it.month}-${it.day}" }, { calRetry++ }, { detail = it.id }, taskScope = taskScope, onTaskScopeChange = { taskScope = it; calNotice = null }, currentUser = currentUser, canReviewTasks = canReviewTasks, businessTimezone = currentUser?.businessTimezone, loading = calLoading, error = calError, onRetry = { calRetry++ }, notice = calNotice, onOpenSummary = { showTaskSummary = true }) }
                            Route.LOGS -> LogsScreen(journalRepo, serverTags.toList(), currentUser?.businessTimezone, currentUser, userRepo, dailyStore) { journalDetailId = it }
                            Route.AI -> AiExplorerScreen(aiRepo, currentUser!!.id)
                            Route.ME -> ProfileScreen(currentUser!!, notifyMaster, showReviewCenter, reviewBadgeCount, tagManager, userRepo, scrollState = profileScrollState, onOpenTagManage = { showTagManage = true }, onOpenReviews = { reviewFocusId = null; showReviews = true }, onOpenSettings = { showSettings = true })
                        }
                      }
                    }
                }
                // 提醒弹窗：逐条展示，关闭后显示下一条
                reminderQueue.firstOrNull()?.takeIf { approvalPrompt == null }?.let { event ->
                    ReminderDialog(event) { if (reminderQueue.isNotEmpty()) reminderQueue.removeAt(0) }
                }
                if (approvalPrompt != null && currentUser != null) {
                    ApprovalPromptDialog(
                        prompt = approvalPrompt!!,
                        onReview = {
                            approvalPrompt = null
                            showReviews = true
                        },
                        onDismiss = { approvalPrompt = null }
                    )
                }
            }
        }
        }
        }
        }
    }
}

@Composable
private fun ApprovalPromptDialog(prompt: ApprovalPrompt, onReview: () -> Unit, onDismiss: () -> Unit) {
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("审批提醒") },
        text = { Text(prompt.message) },
        confirmButton = { PandoraDialogConfirm("查看审批", onReview) },
        dismissButton = { PandoraDialogDismiss("稍后", onDismiss) }
    )
}

@Composable private fun PageHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) =
    PandoraTopBar(title, subtitle, onBack, actions)

/** 企业登录页：POST /auth/login；错误密码/网络异常分别提示，成功后进入应用 */
@Composable private fun LoginScreen(auth: AuthRepository, onLoggedIn: (UserDto) -> Unit) {
    var domain by rememberSaveable { mutableStateOf("") }
    var loginName by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // 登录：水平 24dp、内容最宽 480dp 居中；品牌字使用 Display（规范例外白名单）
    Column(Modifier.fillMaxSize().background(PandoraTheme.colors.appBackground).verticalScroll(rememberScrollState()).padding(horizontal = PandoraSpacing.loginHorizontal), horizontalAlignment = Alignment.CenterHorizontally) {
      Column(Modifier.widthIn(max = PandoraDimensions.loginMaxWidth).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(72.dp))
        Icon(Icons.Default.GridView, null, Modifier.size(PandoraDimensions.placeholderIcon), tint = PandoraTheme.colors.primary)
        Spacer(Modifier.height(PandoraSpacing.space12))
        Text("Pandora", style = PandoraTypography.display)
        Text("智能掌上日志系统", style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
        Spacer(Modifier.height(PandoraSpacing.space32))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
            PandoraTextField(domain, { domain = it; error = null }, Modifier.fillMaxWidth(), label = { Text("企业域") }, singleLine = true)
            PandoraTextField(loginName, { loginName = it; error = null }, Modifier.fillMaxWidth(), label = { Text("登录名") }, singleLine = true)
            PandoraTextField(password, { password = it; error = null }, Modifier.fillMaxWidth(), label = { Text("密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = PandoraTypography.bodyMedium) }
            PandoraButton("登录", {
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
            }, Modifier.fillMaxWidth(), loading = loading, loadingText = "登录中…")
        }
      }
    }
}

@Composable private fun HomeScreen(dashboardRepo: DashboardRepository, calendarRepo: CalendarRepository, notificationRepo: NotificationRepository, dailyStore: DailyItemsStore, openedNotificationId: String?, notificationOpenSequence: Int, notificationReadRevision: Int, businessTimezone: String? = null, onDrawerClosed: () -> Unit, onOpenHome: (HomeDetail) -> Unit, onOpenNotification: (NotificationDto) -> Unit) {
    var expandedPanel by rememberSaveable { mutableStateOf<String?>(null) }
    // 消息抽屉开关用 remember：切走 tab 再回来不应恢复上次打开状态（rememberSaveable 会把 true 带回来，造成「一点首页就开消息」）
    var notifications by remember { mutableStateOf(false) }
    // 信封角标：未读消息数（99+ 封顶）；打开抽屉、关闭抽屉、收到推送打开、以及任何地方把消息置为已读时刷新
    var unreadCount by remember { mutableStateOf(0) }
    LaunchedEffect(notifications, notificationOpenSequence, notificationReadRevision) {
        unreadCount = runCatching { notificationRepo.unreadCount() }.getOrDefault(0)
    }
    val scope = rememberCoroutineScope()
    var dashboard by remember { mutableStateOf<DashboardDto?>(null) }
    var dashLoading by remember { mutableStateOf(true) }
    var dashError by remember { mutableStateOf<String?>(null) }
    val companyPager = remember { HomePanelPager<CompanyItemDto>() }
    val taskPager = remember { HomePanelPager<TaskDto>() }
    // 每日事项：会话内列表 + 本地持久化（按账号隔离）
    val dailyItems = remember { mutableStateListOf<DailyItem>().apply { addAll(dailyStore.load()) } }
    fun mutateDaily(change: MutableList<DailyItem>.() -> Unit) {
        dailyItems.change()
        dailyStore.save(dailyItems)
    }
    fun loadDashboard() {
        dashLoading = true; dashError = null
        scope.launch {
            try {
                val d = dashboardRepo.dashboard()
                dashboard = d
                companyPager.items.clear(); companyPager.items.addAll(d.companyImportantItems.items)
                companyPager.nextCursor = d.companyImportantItems.nextCursor
                companyPager.moreUrl = d.companyImportantItems.moreUrl
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理，回到登录页
            } catch (e: Exception) {
                dashError = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
            } finally {
                dashLoading = false
            }
        }
    }
    // 任务看板：执行人视角，展示被派发给我的任务（scope=mine）
    var taskBoardLoading by remember { mutableStateOf(true) }
    var taskBoardError by remember { mutableStateOf<String?>(null) }
    fun loadTaskBoard() {
        taskBoardLoading = true; taskBoardError = null
        scope.launch {
            try {
                val page = calendarRepo.tasks(scope = "mine", limit = 20)
                taskPager.items.clear(); taskPager.items.addAll(page.items)
                taskPager.nextCursor = page.nextCursor
                taskPager.moreUrl = null
            } catch (e: SessionExpiredException) {
            } catch (e: Exception) {
                taskBoardError = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
            } finally {
                taskBoardLoading = false
            }
        }
    }
    fun loadMoreTasks() {
        val cursor = taskPager.nextCursor ?: return
        if (taskPager.loadingMore) return
        taskPager.loadingMore = true
        scope.launch {
            try {
                val page = calendarRepo.tasks(scope = "mine", cursor = cursor, limit = 20)
                taskPager.items.addAll(page.items)
                taskPager.nextCursor = page.nextCursor
            } catch (_: Exception) {
                // 翻页失败：保留已加载内容，用户可再次点击重试
            } finally {
                taskPager.loadingMore = false
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
    LaunchedEffect(Unit) { loadDashboard(); loadTaskBoard() }
    val d = dashboard
    val panels = listOf(
        HomePanelDef("事件纪", companyPager.items.map { it.title }, "暂无事件"),
        HomePanelDef("任务看板", taskPager.items.map { it.title }, "暂无任务")
    )
    val panelAccent = PandoraTheme.colors.primary
    fun openItem(panelIndex: Int, index: Int, text: String) {
        val kind: String?
        val id: String?
        when (panelIndex) {
            0 -> { kind = "COMPANY_ITEM"; id = companyPager.items.getOrNull(index)?.id }
            1 -> { kind = "TASK"; id = taskPager.items.getOrNull(index)?.id }
            else -> { kind = null; id = null }
        }
        // 进入详情前收起展开浮层，返回后不会叠着一层
        expandedPanel = null
        onOpenHome(HomeDetail(panels[panelIndex].title, index, text, panelAccent, kind, id))
    }
    val colors = PandoraTheme.colors
    // 背景由外层容器绘制，卡通主题的场景插画才能显示在页面下层
    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize().padding(horizontal = PandoraSpacing.pageHorizontal)) {
        PandoraPageTitle(todayDate().shortLabelWithWeekday()) {
            // 与「我的」页红点同一实现：未读消息数角标（99+ 封顶）
            BadgedBox(badge = { if (unreadCount > 0) Badge { Text(if (unreadCount >= 99) "99+" else "$unreadCount") } }) {
                IconButton(onClick = { notifications = true }) { Surface(shape = PandoraShapes.circle, color = colors.primaryContainer) { Icon(Icons.Default.Email, "邮箱", Modifier.padding(PandoraSpacing.space8).size(PandoraDimensions.icon), tint = colors.textPrimary) } }
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                dashError != null -> Column(Modifier.align(Alignment.Center).padding(PandoraSpacing.space24), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                    Text(dashError!!, style = PandoraTypography.bodyMedium, color = colors.textSecondary)
                    PandoraButton("重试", { loadDashboard() })
                }
                d == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 3.dp)
                    Text("正在加载…", style = PandoraTypography.bodyMedium, color = colors.textSecondary)
                }
                else -> Column(Modifier.fillMaxSize().padding(bottom = PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraGrid.gutter)) {
            Row(Modifier.fillMaxWidth().weight(0.9f), horizontalArrangement = Arrangement.spacedBy(PandoraGrid.gutter)) {
                HomePanel(panels[0], Modifier.weight(1f).fillMaxHeight(), loading = dashLoading, error = dashError, onRetry = { loadDashboard() }, onExpand = { expandedPanel = panels[0].title }, onItemClick = { i, t -> openItem(0, i, t) })
                HomePanel(panels[1], Modifier.weight(1f).fillMaxHeight(), loading = taskBoardLoading, error = taskBoardError, onRetry = { loadTaskBoard() }, onExpand = { expandedPanel = panels[1].title }, onItemClick = { i, t -> openItem(1, i, t) })
            }
            DailyItemsPanel(
                Modifier.fillMaxWidth().weight(1.1f).padding(bottom = PandoraSpacing.space4),
                items = dailyItems,
                onAdd = { date, content -> mutateDaily { add(DailyItem(UUID.randomUUID().toString(), date.toString(), content)) } },
                onToggle = { item -> mutateDaily { val i = indexOfFirst { it.id == item.id }; if (i >= 0) this[i] = this[i].copy(done = !this[i].done) } },
                onDelete = { item -> mutateDaily { removeAll { it.id == item.id } } }
            )
                }
            }
        }
      }
      val selected = panels.firstOrNull { it.title == expandedPanel }
      if (selected != null) {
          androidx.activity.compose.BackHandler { expandedPanel = null }
          Box(Modifier.fillMaxSize().background(colors.scrim).clickable { expandedPanel = null })
          PandoraOverlaySurface(Modifier.align(Alignment.Center).padding(horizontal = PandoraSpacing.space24).widthIn(max = PandoraDimensions.dialogMaxWidth).fillMaxWidth().fillMaxHeight(.82f)) {
              Column(Modifier.fillMaxSize().padding(PandoraSpacing.dialogPadding)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                      Text(selected.title, style = PandoraTypography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
                      IconButton({ expandedPanel = null }) { Icon(Icons.Default.Close, "关闭", tint = colors.textSecondary) }
                  }
                  Text("共 ${selected.items.size} 条", style = PandoraTypography.caption, color = colors.textTertiary, modifier = Modifier.padding(top = PandoraSpacing.space4, bottom = PandoraSpacing.space12))
                  Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                      val list = selected.items
                      // 展开后的每一条也可点击进入该事项详情（与面板内点击一致）
                      val panelIndex = panels.indexOfFirst { it.title == selected.title }
                      list.forEachIndexed { i, item ->
                          Row(
                              Modifier.fillMaxWidth().heightIn(min = PandoraDimensions.touchTarget).clip(PandoraShapes.small)
                                  .clickable { if (panelIndex >= 0) openItem(panelIndex, i, item) }
                                  .padding(horizontal = PandoraSpacing.space4, vertical = PandoraSpacing.space12),
                              verticalAlignment = Alignment.CenterVertically
                          ) {
                              Box(Modifier.size(32.dp).clip(PandoraShapes.circle).background(colors.primaryContainer), contentAlignment = Alignment.Center) {
                                  Text("${i + 1}", style = PandoraTypography.labelLarge, color = colors.textPrimary)
                              }
                              Spacer(Modifier.width(PandoraSpacing.space12))
                              Text(item, style = PandoraTypography.bodyLarge, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                          }
                          if (i < list.lastIndex) PandoraDivider()
                      }
                      // 分页：nextCursor 非空时提供“加载更多”
                      val pagerState = when (selected.title) {
                          panels[0].title -> companyPager.nextCursor to companyPager.loadingMore
                          panels[1].title -> taskPager.nextCursor to taskPager.loadingMore
                          else -> null to false
                      }
                      if (pagerState.first != null) {
                          TextButton({
                              when (selected.title) {
                                  panels[0].title -> loadMoreCompany()
                                  panels[1].title -> loadMoreTasks()
                              }
                          }, Modifier.fillMaxWidth().padding(top = PandoraSpacing.space8)) {
                              Text(if (pagerState.second) "加载中…" else "加载更多", style = PandoraTypography.labelLarge, color = colors.primary)
                          }
                      }
                  }
              }
          }
      }
      if (notifications) {
          androidx.activity.compose.BackHandler { notifications = false; onDrawerClosed() }
          Box(Modifier.fillMaxSize().background(colors.scrim).clickable { notifications = false; expandedPanel = null; onDrawerClosed() })
          NotificationDrawer(notificationRepo, openedNotificationId, businessTimezone = businessTimezone, onClose = { notifications = false; onDrawerClosed() }, onOpen = { onDrawerClosed(); onOpenNotification(it) })
      }
    }
}

@Composable private fun HomePanel(
    panel: HomePanelDef,
    modifier: Modifier,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
    onExpand: () -> Unit,
    onItemClick: (index: Int, text: String) -> Unit
) {
    val list = panel.items.take(10)
    val colors = PandoraTheme.colors
    // 导图四板块是 2×2 紧凑面板：12dp 内距（规范“紧凑业务项”），1dp 中性边框替代 2dp 彩边
    PandoraPanel(modifier, contentPadding = PaddingValues(start = PandoraSpacing.space12, end = PandoraSpacing.space12, top = PandoraSpacing.space8, bottom = PandoraSpacing.space4)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                panel.title,
                style = PandoraTypography.labelLarge,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            trailing()
        }
        HorizontalDivider(Modifier.padding(top = PandoraSpacing.space4), color = colors.divider)
        // 空 / 加载中 / 加载失败分别给出对应提示，而不是一片空白
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            when {
                loading && list.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space16),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = colors.primary, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(PandoraSpacing.space8))
                    Text("加载中…", style = PandoraTypography.caption, color = colors.textSecondary)
                }
                error != null && list.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space12),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("加载失败", style = PandoraTypography.bodyMedium, color = colors.textSecondary)
                    PandoraButton("点击重试", onRetry, variant = PandoraButtonVariant.Text, size = PandoraButtonSize.Compact)
                }
                list.isEmpty() -> Column(
                    Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space16),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(panel.emptyText, style = PandoraTypography.bodyMedium, color = colors.textSecondary)
                }
                else -> list.forEachIndexed { i, text ->
                    Row(
                        Modifier.fillMaxWidth().clip(PandoraShapes.small)
                            .clickable { onItemClick(i, text) }
                            .padding(horizontal = PandoraSpacing.space4, vertical = PandoraSpacing.space8),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${i + 1}. $text",
                            style = PandoraTypography.bodyMedium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (i < list.lastIndex) PandoraDivider()
                }
            }
        }
        TextButton(onExpand, Modifier.fillMaxWidth()) {
            Text("展开查看 →", style = PandoraTypography.labelLarge, color = colors.primary)
        }
    }
}

/** 列表行分割：统一 1dp Divider；保留原调用名，颜色参数由 Token 决定 */
@Composable private fun DashedDivider(@Suppress("UNUSED_PARAMETER") color: Color) = PandoraDivider()

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
    val accent = PandoraTheme.colors.primary
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
        PandoraOverlaySurface(
            Modifier.padding(horizontal = PandoraSpacing.space16).widthIn(max = PandoraDimensions.dialogMaxWidth).fillMaxWidth().fillMaxHeight(.88f).clickable(enabled = false) { }
        ) {
            Column(Modifier.fillMaxSize().padding(PandoraSpacing.dialogPadding)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
                    Spacer(Modifier.width(PandoraSpacing.space12))
                    Column(Modifier.weight(1f)) {
                        Text("编辑 · 个人十大重要事项", style = PandoraTypography.titleMedium)
                    }
                    IconButton(onClose) { Icon(Icons.Default.Close, "关闭") }
                }
                Spacer(Modifier.height(PandoraSpacing.space12))
                when {
                    loading -> Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = accent, strokeWidth = 3.dp)
                        Spacer(Modifier.height(PandoraSpacing.space12))
                        Text("正在加载…", color = PandoraTheme.colors.textTertiary, style = PandoraTypography.bodyMedium)
                    }
                    loadError != null -> Column(Modifier.fillMaxWidth().weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                        Text(loadError!!, color = PandoraTheme.colors.textSecondary)
                        PandoraButton("重试", { retry++ })
                    }
                    else -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        Text("正式事项", style = PandoraTypography.labelLarge)
                        Spacer(Modifier.height(PandoraSpacing.space4))
                        selected.forEachIndexed { i, item ->
                            val dragging = draggingIndex == i
                            Row(
                                Modifier.fillMaxWidth()
                                    .clip(PandoraShapes.medium)
                                    .background(if (dragging) PandoraTheme.colors.primaryContainer else Color.Transparent)
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
                                    .padding(horizontal = PandoraSpacing.space4, vertical = PandoraSpacing.space8),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(28.dp).clip(PandoraShapes.circle).background(PandoraTheme.colors.primaryContainer), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", style = PandoraTypography.caption, fontWeight = FontWeight.Medium, color = PandoraTheme.colors.textPrimary)
                                }
                                Spacer(Modifier.width(PandoraSpacing.space8))
                                Column(Modifier.weight(1f)) {
                                    Text(item.content, style = PandoraTypography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                // 排序中不提供移出，避免与拖动冲突
                                if (!sorting) IconButton({ toggle(item) }) {
                                    Icon(Icons.Default.Close, "移出", Modifier.size(PandoraDimensions.iconSmall + 2.dp), tint = PandoraTheme.colors.error)
                                }
                            }
                            if (i < selected.lastIndex) DashedDivider(accent)
                        }
                        // 排序模式下不显示候选区，只专注于正式事项排序
                        if (!sorting) {
                            // 分割线：上为正式事项，下为候选事项
                            Spacer(Modifier.height(PandoraSpacing.space16))
                            DashedDivider(accent)
                            Spacer(Modifier.height(PandoraSpacing.space12))
                            Text("候选事项", style = PandoraTypography.labelLarge)
                            Spacer(Modifier.height(PandoraSpacing.space4))
                        }
                        if (!sorting) visibleCandidates.forEach { entry ->
                            val canAdd = selected.size < 10
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = PandoraSpacing.space4, vertical = PandoraSpacing.space8),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 勾选圆形：打钩即加入正式事项，并随即从候选区移除
                                // 视觉 28dp 圆形、外围 40dp 命中区
                                Box(Modifier.size(40.dp).clip(PandoraShapes.circle).clickable(enabled = canAdd, onClickLabel = "加入正式事项") { toggle(entry) }, contentAlignment = Alignment.Center) {
                                    Box(
                                        Modifier.size(28.dp).clip(PandoraShapes.circle)
                                            .background(if (canAdd) PandoraTheme.colors.primaryContainer else Color.Transparent)
                                            .border(PandoraDimensions.borderWidth, if (canAdd) PandoraTheme.colors.primary else PandoraTheme.colors.border, PandoraShapes.circle),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, null, Modifier.size(PandoraDimensions.iconSmall), tint = if (canAdd) PandoraTheme.colors.textPrimary else PandoraTheme.colors.textDisabled)
                                    }
                                }
                                Spacer(Modifier.width(PandoraSpacing.space8))
                                Text(
                                    entry.content, style = PandoraTypography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = PandoraTypography.bodyMedium, modifier = Modifier.padding(top = PandoraSpacing.space8)) }
                Spacer(Modifier.height(PandoraSpacing.space8))
                // 左下：排序 / 取消；右下：保存 / 完成
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // 排序与保存同宽同高（96×40），左右两角对称
                    if (sorting) {
                        PandoraButton("取消", { cancelSorting() }, Modifier.defaultMinSize(minWidth = 96.dp), variant = PandoraButtonVariant.Outline, size = PandoraButtonSize.Compact)
                    } else {
                        PandoraButton("排序", { showSortHint = true }, Modifier.defaultMinSize(minWidth = 96.dp), variant = PandoraButtonVariant.Outline, size = PandoraButtonSize.Compact, enabled = selected.size > 1)
                    }
                    Spacer(Modifier.weight(1f))
                    PandoraButton(
                        if (sorting) "完成" else "保存",
                        { if (sorting) { sorting = false; draggingIndex = null; dragOffset = 0f } else save() },
                        Modifier.defaultMinSize(minWidth = 96.dp),
                        size = PandoraButtonSize.Compact,
                        loading = saving
                    )
                }
            }
        }
    }
    if (showSortHint) PandoraAlertDialog(
        onDismissRequest = { showSortHint = false },
        title = { Text("排序") },
        text = { Text("长按正式事项即可自由排序。", color = PandoraTheme.colors.textSecondary) },
        confirmButton = { PandoraDialogConfirm("确定", { showSortHint = false; startSorting() }) },
        dismissButton = { PandoraDialogDismiss("取消", { showSortHint = false }) }
    )
}

@Composable private fun HomeItemDetailScreen(repo: DashboardRepository, detail: HomeDetail, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    var eventTitle by remember(detail.id, detail.text) { mutableStateOf(detail.text) }
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
                "COMPANY_ITEM" -> repo.companyItem(id).let {
                    eventTitle = it.title
                    it.content
                }
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
    Column(Modifier.fillMaxSize().background(PandoraTheme.colors.appBackground)) {
        PageHeader(detail.section, onBack = onBack)
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(PandoraSpacing.pageHorizontal)
        ) {
            PandoraPanel(Modifier.fillMaxWidth(), tone = PandoraPanelTone.White, contentPadding = PaddingValues(0.dp)) {
                Column(Modifier.fillMaxWidth().padding(PandoraSpacing.space24), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
                    Box(
                        Modifier.align(Alignment.CenterHorizontally).size(PandoraDimensions.avatarLarge).clip(PandoraShapes.circle).background(PandoraTheme.colors.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${detail.index + 1}", style = PandoraTypography.titleMedium, color = PandoraTheme.colors.textPrimary)
                    }
                    when {
                        loading -> Column(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space12), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                            CircularProgressIndicator(color = PandoraTheme.colors.primary, strokeWidth = 3.dp)
                            Text("正在加载全文…", color = PandoraTheme.colors.textTertiary)
                        }
                        error != null -> Column(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space12), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                            Text(error!!, color = MaterialTheme.colorScheme.error)
                            PandoraButton("重试", { attempt++ })
                        }
                        detail.kind == "COMPANY_ITEM" -> {
                            Text(
                                eventTitle,
                                style = PandoraTypography.titleLarge,
                                modifier = Modifier.fillMaxWidth().semantics { heading() }
                            )
                            content?.takeIf { it.isNotBlank() }?.let { body ->
                                Text(
                                    body,
                                    style = PandoraTypography.bodyLarge,
                                    modifier = Modifier.fillMaxWidth(),
                                    softWrap = true
                                )
                            }
                        }
                        else -> {
                            // 详情正文前补上面板里的标题：面板只看到标题，点开后仍是「标题：全文」
                            val text = content ?: detail.text
                            val shown = if (detail.text.isNotBlank() && !text.startsWith(detail.text)) "${detail.text}：$text" else text
                            Text(shown, style = PandoraTypography.bodyLarge)
                        }
                    }
                }
            }
        }
    }
}


/** 编辑中的一条工作条目：entryId 非空表示修改已有条目，空表示新增（发布时生成 clientEntryKey） */
private typealias EntryDraft = JournalDraftEntryDto

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun LogsScreen(repo: JournalRepository, availableTags: List<TagDto>, businessTimezone: String?, currentUser: UserDto?, userRepo: UserRepository, dailyStore: DailyItemsStore, onOpen: (String) -> Unit) {
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
    var editNotice by remember { mutableStateOf<String?>(null) }
    var editNoticeEvent by remember { mutableStateOf(0L) }
    LaunchedEffect(editNoticeEvent) {
        if (editNotice != null) {
            kotlinx.coroutines.delay(2_000)
            editNotice = null
        }
    }
    LaunchedEffect(writing) { editNotice = null }
    fun showEditNotice(message: String) {
        editNotice = message
        editNoticeEvent++
    }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    // 编辑器业务日期（最近三日可选）；删除确认目标与错误
    var editorDateKey by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showTagPicker by rememberSaveable { mutableStateOf(false) }
    var deletingJournal by remember { mutableStateOf<JournalDto?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val mutedColors = pandoraTextFieldColors()
    // 日报草稿（缓存目录）：上次编辑未发布的内容
    val context = LocalContext.current
    var savedJournalDraft by remember(dailyStore) {
        mutableStateOf(AppCache.readDraft(context, "journal_draft.json")?.let { s -> runCatching { AppCache.json.decodeFromString<JournalDraftDto>(s) }.getOrNull() })
    }
    // 同次编辑切换日期时保留用户删改后的正文（包括空正文）及标签。
    val sessionDrafts = remember(dailyStore) { mutableMapOf<String, JournalDraftDto>() }
    // 编辑器打开期间草稿实时落盘；发布成功或放弃时清除
    LaunchedEffect(writing, editorDateKey, entryDrafts, tags) {
        if (writing) {
            val draft = JournalDraftDto(
                editorDateKey, entryDrafts,
                tags.mapNotNull { name -> availableTags.firstOrNull { it.name == name }?.id }
            )
            savedJournalDraft = draft
            AppCache.writeDraft(context, "journal_draft.json", AppCache.json.encodeToString(JournalDraftDto.serializer(), draft))
        }
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
    fun loadEditorDate(dateKey: String): Boolean {
        val draft = sessionDrafts[dateKey] ?: savedJournalDraft?.takeIf { it.dateKey == dateKey && it.dateKey in threeDays().map(::isoOf) && (it.dateKey == todayIso() || journals.none { j -> j.workDate == it.dateKey }) }
        // 编辑窗口按创建时间（allowedActions 由后端按 published_at 判定），而不是 work_date
        val existing = journals.firstOrNull { it.workDate == dateKey }
        if (existing != null && "EDIT" !in existing.allowedActions) { showEditNotice("日志创建已满 24 小时，不能再修改或删除"); return false }
        editNotice = null
        editorDateKey = dateKey
        editingJournal = existing
        entryDrafts = initialJournalEntries(
            dateKey, dailyStore.load(), draft?.entries,
            existing?.entries?.sortedBy { it.displayOrder }?.map { EntryDraft(it.entryId, it.content) }
        )
        tags = if (draft != null) draft.tagIds.mapNotNull { id -> availableTags.firstOrNull { it.id == id }?.name }
        else existing?.tags?.sortedBy { if (it.id == existing.primaryTagId) 0 else 1 }?.map { it.name } ?: emptyList()
        publishError = null
        return true
    }
    fun openEditor(dateKey: String) {
        if (businessTimezone.isNullOrBlank()) { publishError = "请管理员先配置公司业务时区"; return }
        sessionDrafts.clear()
        if (loadEditorDate(dateKey)) writing = true
    }
    fun switchEditorDate(dateKey: String) {
        if (dateKey == editorDateKey) { editNotice = null; return }
        sessionDrafts[editorDateKey] = JournalDraftDto(
            editorDateKey, entryDrafts,
            tags.mapNotNull { name -> availableTags.firstOrNull { it.name == name }?.id }
        )
        loadEditorDate(dateKey)
    }
    fun clearJournalDrafts() {
        sessionDrafts.clear()
        savedJournalDraft = null
        AppCache.deleteDraft(context, "journal_draft.json")
    }
    fun closeEditor() {
        if (entryDrafts.any { it.content.isNotBlank() }) confirmExit = true else writing = false
    }
    fun publish() {
        if (editingJournal == null && editorDateKey !in threeDays().map(::isoOf)) { publishError = "只能填写今天、昨天和前天的日志"; return }
        if (editingJournal != null && "EDIT" !in editingJournal!!.allowedActions) { publishError = "日志创建已满 24 小时，不能再修改或删除"; return }
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
                clearJournalDrafts()
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
        Column(Modifier.fillMaxSize().padding(horizontal = PandoraSpacing.space16)) {
            // 一级页面统一标题行：与导图、AI地图、我的同高同字号
            PandoraPageTitle("日志")
            Spacer(Modifier.height(PandoraSpacing.space4))
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12), contentPadding = PaddingValues(bottom = PandoraSpacing.space16)) {
                // 主操作与视图页「回到今年」同为实心主题按钮
                item { PandoraButton({ openEditor(todayIso()) }, Modifier.fillMaxWidth(), enabled = !publishing) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(PandoraSpacing.space8)); Text(if (todayJournal != null) "补充今日工作" else "记录今日工作") } }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        PandoraButton({ filterOpen = true }, variant = PandoraButtonVariant.Secondary, size = PandoraButtonSize.Compact) {
                            Text(historyFilter?.let { dateLabel(it) } ?: "全部日期")
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }
                }
                if (loading && journals.isEmpty()) item {
                    Row(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space24), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = PandoraTheme.colors.primary, strokeWidth = 3.dp)
                        Spacer(Modifier.width(PandoraSpacing.space12))
                        Text("正在加载…", color = PandoraTheme.colors.textTertiary)
                    }
                }
                if (loadError != null && journals.isEmpty()) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space16), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                        Text(loadError!!, color = PandoraTheme.colors.textSecondary)
                        PandoraButton("重试", { loadRetry++ })
                    }
                }
                items(if (expandedLogs) filteredJournals else filteredJournals.take(5)) { j ->
                    JournalCard(
                        currentUser = currentUser, userRepo = userRepo,
                        journal = j,
                        businessTimezone = businessTimezone,
                        enabled = !publishing,
                        onEdit = { openEditor(j.workDate) },
                        onDelete = { deleteError = null; deletingJournal = j },
                        onOpen = { onOpen(j.journalId.ifBlank { j.id }) }
                    )
                }
                if (filteredJournals.size > 5) item { PandoraButton(if (expandedLogs) "收起" else "展开", { expandedLogs = !expandedLogs }, Modifier.fillMaxWidth(), variant = PandoraButtonVariant.Secondary) }
                if (!loading && journals.isNotEmpty() && filteredJournals.isEmpty()) item { Text("所选日期暂无日志", color = PandoraTheme.colors.textTertiary) }
                if (!loading && journals.isEmpty() && loadError == null) item { Text("最近 30 天暂无日志，点击上方按钮记录今日工作。", color = PandoraTheme.colors.textTertiary) }
            }
        }
        if (filterOpen) JournalDatePickerDialog(
            loggedDates = loggedDates,
            today = todayIso(),
            selected = historyFilter,
            onSelect = { picked -> historyFilter = picked; filterOpen = false; expandedLogs = false },
            onDismiss = { filterOpen = false }
        )
        if (writing) Box(Modifier.fillMaxSize().background(PandoraTheme.colors.scrim).clickable { closeEditor() })
        if (writing) {
            androidx.activity.compose.BackHandler { closeEditor() }
            val imeVisible = WindowInsets.isImeVisible
            val journalFocusManager = LocalFocusManager.current
            // 底部对齐 + 导航栏/键盘避让：弹窗直接落在键盘（或导航栏）上方，不再留出一段空白
            Box(Modifier.fillMaxSize()) {
            PandoraOverlaySurface(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(horizontal = PandoraSpacing.space16).widthIn(max = PandoraDimensions.dialogMaxWidth).fillMaxWidth().fillMaxHeight(if (imeVisible) .92f else .9f).clickable(enabled = false) { }) {
                Column(Modifier.fillMaxSize().padding(PandoraSpacing.dialogPadding)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(if (editingJournal != null) "编辑日志" else "记录日志", style = PandoraTypography.titleMedium, modifier = Modifier.weight(1f)); IconButton({ closeEditor() }) { Icon(Icons.Default.Close, "关闭", tint = PandoraTheme.colors.textSecondary) } }
                    // 日期、标签与工作条目共用滚动视口，保留键盘和导航栏避让。
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        Text("· 日志日期", style = PandoraTypography.labelLarge)
                        Row(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space12), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                            threeDays().forEach { d ->
                                val key = isoOf(d)
                                val selected = editorDateKey == key
                                Text(
                                    d.shortLabel(),
                                    Modifier.weight(1f).heightIn(min = PandoraDimensions.compactButtonHeight).clip(PandoraShapes.medium).background(if (selected) PandoraTheme.colors.primary else PandoraTheme.colors.primaryContainer).clickable { switchEditorDate(key) }.padding(vertical = PandoraSpacing.space8), style = PandoraTypography.labelLarge,
                                    color = if (selected) PandoraTheme.colors.textOnPrimary else PandoraTheme.colors.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        val editorEditable = editingJournal == null || "EDIT" in editingJournal!!.allowedActions
                        if (!editorEditable) Text("日志仅在创建后 24 小时内可修改或删除。", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                        Text("· 标签（可多选，可切换主标签）", style = PandoraTypography.labelLarge, modifier = Modifier.padding(top = PandoraSpacing.space12, bottom = PandoraSpacing.space8))
                        if (tags.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space2), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                            tags.forEach { name ->
                                PandoraTagChip(name, availableTags.firstOrNull { it.name == name }?.colorHex, Modifier.heightIn(min = PandoraDimensions.chipButtonHeight), primary = name == tags.first(), onClick = { tags = tags - name })
                            }
                        }
                        OutlinedButton({ journalFocusManager.clearFocus(force = true); showTagPicker = true }, contentPadding = PaddingValues(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8), border = BorderStroke(1.dp, PandoraTheme.colors.border), colors = ButtonDefaults.outlinedButtonColors(contentColor = PandoraTheme.colors.textPrimary)) { Icon(Icons.Default.Add, "选择标签", Modifier.size(16.dp)); Spacer(Modifier.width(PandoraSpacing.space4)); Text("选择标签", style = PandoraTypography.bodyMedium) }
                        Text("· 完成的工作", style = PandoraTypography.labelLarge, modifier = Modifier.padding(top = PandoraSpacing.space12, bottom = PandoraSpacing.space8))
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                            entryDrafts.forEachIndexed { i, draft ->
                                Row(verticalAlignment = Alignment.Top) {
                                    JournalEntryField(
                                        text = draft.content,
                                        onValueChange = { value -> entryDrafts = entryDrafts.toMutableList().also { it[i] = draft.copy(content = value) } },
                                        colors = mutedColors,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (editingJournal == null && entryDrafts.size > 1) IconButton({ entryDrafts = entryDrafts.filterIndexed { idx, _ -> idx != i } }, Modifier.size(40.dp)) { Icon(Icons.Default.Close, "删除此条", Modifier.size(16.dp)) }
                                }
                            }
                        }
                    }
                    (publishError ?: editNotice)?.let { Text(it, color = MaterialTheme.colorScheme.error, style = PandoraTypography.bodyMedium, modifier = Modifier.padding(top = PandoraSpacing.space8, bottom = PandoraSpacing.space4)) }
                    Row(Modifier.fillMaxWidth().padding(top = PandoraSpacing.space12), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                        PandoraButton("取消", { closeEditor() }, Modifier.weight(1f), variant = PandoraButtonVariant.Secondary, enabled = !publishing)
                        PandoraButton(
                            "发布",
                            { publish() },
                            Modifier.weight(1f),
                            enabled = (entryDrafts.any { it.content.isNotBlank() } || (editingJournal?.entries?.any { e -> entryDrafts.none { it.entryId == e.entryId } } == true)) && (editingJournal == null || "EDIT" in editingJournal!!.allowedActions),
                            loading = publishing,
                            loadingText = "发布中…"
                        )
                    }
                }
            }
            }
        }
        if (confirmExit) PandoraAlertDialog(onDismissRequest = { confirmExit = false }, title = { Text("放弃未保存内容？") }, text = { Text("退出后当前填写内容会被清空。") }, confirmButton = { PandoraDialogConfirm("放弃并退出", { confirmExit = false; entryDrafts = listOf(EntryDraft(null, "")); writing = false; clearJournalDrafts() }, destructive = true) }, dismissButton = { PandoraDialogDismiss("继续编辑", { confirmExit = false }) })
        if (showTagPicker) PandoraAlertDialog(
        onDismissRequest = { showTagPicker = false },
        title = { Text("选择标签") },
        text = {
            Column {
                Text("可多选；点“设为主”可更换主标签。再次点击已选标签可取消。", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                Spacer(Modifier.height(PandoraSpacing.space8))
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    if (availableTags.isEmpty()) Text("暂无标签，可在「我的 → 标签管理」中创建。", style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textTertiary)
                    availableTags.sortedBy { if (it.name == tags.firstOrNull()) 0 else 1 }.forEach { tag ->
                        val selected = tag.name in tags
                        val isPrimary = selected && tags.first() == tag.name
                        Row(
                            Modifier.fillMaxWidth().clip(PandoraShapes.small).clickable { tags = if (selected) tags - tag.name else tags + tag.name }.padding(horizontal = PandoraSpacing.space4, vertical = PandoraSpacing.space4),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(PandoraDimensions.iconSmall).clip(PandoraShapes.circle).background(tagHexColor(tag.colorHex)).border(PandoraDimensions.borderWidth, PandoraTheme.colors.border, PandoraShapes.circle))
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
    deletingJournal?.let { j ->
            PandoraAlertDialog(
                onDismissRequest = { if (!publishing) deletingJournal = null },
                title = { Text("删除这篇日志？") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        Text("${dateLabel(j.workDate)} 的 ${j.entries.size} 条工作记录将被删除，且不可恢复。")
                        deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = PandoraTypography.bodyMedium) }
                    }
                },
                confirmButton = {
                    if (publishing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = PandoraTheme.colors.primary)
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
                    }) { Text("删除", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.error) }
                },
                dismissButton = { if (!publishing) PandoraDialogDismiss("取消", { deletingJournal = null }) }
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
        horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8),
        verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)
    ) {
        tags.sortedBy { if (it.id == primaryTagId) 0 else 1 }.forEach { t ->
            PandoraTagChip(t.name, t.colorHex, primary = t.id == primaryTagId)
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
    currentUser: UserDto?,
    userRepo: UserRepository,
    businessTimezone: String?,
    enabled: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    val editable = "EDIT" in journal.allowedActions
    // publishedAt 是 UTC 时间戳，按公司业务时区换算，保证与现实时间一致
    val created = businessTimeLabel(journal.publishedAt, businessTimezone)
    PandoraPanel(Modifier.fillMaxWidth(), shape = PandoraShapes.medium, contentPadding = PaddingValues(0.dp), onClick = onOpen) {
        Column(Modifier.padding(horizontal = PandoraSpacing.space16, vertical = PandoraSpacing.space12)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RemoteAvatar(journal.authorUserId, journal.author?.displayName?.takeIf { it.isNotBlank() } ?: currentUser?.displayName.orEmpty(), 32.dp, 14.sp, userRepo)
                Spacer(Modifier.width(PandoraSpacing.space12))
                Text(isoLabel(journal.workDate), style = PandoraTypography.labelLarge, modifier = Modifier.weight(1f))
                if (created.isNotBlank()) Text(created, style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
            }
            if (journal.tags.isNotEmpty()) {
                Spacer(Modifier.height(PandoraSpacing.space8))
                TagChips(journal.tags, journal.primaryTagId)
            }
            Spacer(Modifier.height(PandoraSpacing.space8))
            journal.entries.sortedBy { it.displayOrder }.forEachIndexed { i, entry ->
                Text(
                    entry.content, style = PandoraTypography.bodyMedium,
                    color = PandoraTheme.colors.textSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                if (i < journal.entries.lastIndex) Spacer(Modifier.height(PandoraSpacing.space4))
            }
            if (editable) {
                // 编辑/删除居中放在工作内容下方
                Row(
                    Modifier.fillMaxWidth().padding(top = PandoraSpacing.space8),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onEdit, enabled = enabled, contentPadding = PaddingValues(horizontal = PandoraSpacing.space16)) { Text("编辑", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
                    TextButton(onDelete, enabled = enabled, contentPadding = PaddingValues(horizontal = PandoraSpacing.space16)) { Text("删除", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.error) }
                }
            }
        }
    }
}

/** 日志详情分区：小标题 + 主题卡片 */
@Composable
private fun JournalSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title, style = PandoraTypography.labelLarge, color = PandoraTheme.colors.textSecondary, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(PandoraSpacing.space8))
        PandoraPanel(Modifier.fillMaxWidth(), tone = PandoraPanelTone.White, content = content)
    }
}

/** 日志详情：创建时间 / 标签 / 完成的工作 三个分区 */
@Composable
private fun JournalDetailScreen(repo: JournalRepository, journalId: String, businessTimezone: String?, currentUser: UserDto?, userRepo: UserRepository, onBack: () -> Unit) {
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
    Column(Modifier.fillMaxSize().background(PandoraTheme.colors.appBackground)) {
        // 抬头与导图片事项详情同为 PageHeader，两个详情界面的抬头高度一致
        PageHeader("日志详情", onBack = onBack)
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PandoraTheme.colors.primary, strokeWidth = 3.dp)
            }
            error != null -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(error!!, color = PandoraTheme.colors.textSecondary)
                Spacer(Modifier.height(PandoraSpacing.space12))
                PandoraButton("重试", { attempt++ })
            }
            j == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("日志不存在或已被删除。", color = PandoraTheme.colors.textSecondary)
            }
            else -> Column(
                Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(PandoraSpacing.space16),
                verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)
            ) {
                // 作者：姓名 + 部门/组织 + 职位（详情接口返回 author；旧数据回退到本人或占位名）
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                    val author = j.author
                    val authorName = author?.displayName?.takeIf { it.isNotBlank() }
                        ?: if (j.authorUserId == currentUser?.id) currentUser.displayName.ifBlank { currentUser.name } else "成员 ${j.authorUserId.takeLast(4)}"
                    RemoteAvatar(j.authorUserId, authorName, 40.dp, 17.sp, userRepo)
                    Column(Modifier.weight(1f)) {
                        Text(authorName, style = PandoraTypography.titleSmall, color = PandoraTheme.colors.textPrimary)
                        val unit = listOfNotNull(author?.departmentName, author?.organizationName?.takeIf { it != author.departmentName })
                            .filter { it.isNotBlank() }.joinToString(" · ")
                        val line = listOf(unit.ifBlank { "未分配部门" }, author?.jobTitle.orEmpty()).filter { it.isNotBlank() }.joinToString("  |  ")
                        if (author != null) Text(line, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
                    }
                }
                JournalSection("创建时间") {
                    val created = com.pandora6ix.app.data.businessDateTimeLabel(j.publishedAt, businessTimezone)
                    Text(
                        created.ifBlank { "未知" }, style = PandoraTypography.bodyLarge)
                }
                if (j.tags.isNotEmpty()) JournalSection("标签") { TagChips(j.tags, j.primaryTagId) }
                JournalSection("完成的工作") {
                    val entries = j.entries.sortedBy { it.displayOrder }
                    entries.forEachIndexed { i, entry ->
                        // 正文与导图片事项详情一致（19sp / 30sp 行高），日志详情更易读
                        Text(entry.content, style = PandoraTypography.bodyLarge)
                        if (i < entries.lastIndex) Spacer(Modifier.height(PandoraSpacing.space12))
                    }
                }
                // 上级留言：放在内容下方，上级有大输入框，作者只读
                com.pandora6ix.app.ui.logs.JournalCommentsSection(repo, j.id, businessTimezone, userRepo)
            }
        }
    }
}

@Composable private fun BoxScope.NotificationDrawer(repository: NotificationRepository, requestedId: String?, businessTimezone: String? = null, onClose: () -> Unit, onOpen: (NotificationDto) -> Unit) {
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
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
    // 点推送进 App 只做定位高亮，不自动置已读：未读标记统一在进入任务/审批详情时才消除
    // 通知抽屉：保留右侧进入，白色浮层，最大 360dp 或窗口 90%
    PandoraOverlaySurface(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.9f).widthIn(max = 360.dp).fillMaxHeight(), shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)) {
        Column(Modifier.fillMaxSize().padding(horizontal = PandoraSpacing.space16, vertical = PandoraSpacing.space16)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("消息", style = PandoraTypography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
                IconButton(onClose) { Icon(Icons.Default.Close, "关闭", tint = PandoraTheme.colors.textSecondary) }
            }
            when {
                loading -> Text("正在加载消息…", color = PandoraTheme.colors.textTertiary, modifier = Modifier.padding(vertical = PandoraSpacing.space16))
                loadError != null -> Text(loadError!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(vertical = PandoraSpacing.space16))
                records.isEmpty() -> Text("暂无消息", color = PandoraTheme.colors.textTertiary, modifier = Modifier.padding(vertical = PandoraSpacing.space16))
                else -> LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                    items(records, key = { it.id }) { item ->
                        // 未读主题色、已读白色；点推送进来的那条（requestedId）始终高亮，方便一眼找到
                        PandoraItemContainer(color = if (item.id == requestedId || item.readAt == null) PandoraTheme.colors.primaryContainer else PandoraTheme.colors.surface) {
                            Column(Modifier.fillMaxWidth().clickable {
                                if (item.readAt == null) scope.launch {
                                    if (runCatching { repository.markRead(item.id) }.isSuccess)
                                        records = records.map { if (it.id == item.id) it.copy(readAt = "read") else it }
                                }
                                onOpen(item)
                            }.padding(PandoraSpacing.space12)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.title, modifier = Modifier.weight(1f), color = PandoraTheme.colors.textPrimary, style = PandoraTypography.labelLarge)
                                    val decided = item.eventType == "REVIEW_REQUEST" && item.resolved
                                    if (decided) Text("已审批", color = PandoraTheme.colors.textPrimary, style = PandoraTypography.labelLarge)
                                    else if (item.readAt == null) Text("未读", color = PandoraTheme.colors.textPrimary, style = PandoraTypography.caption)
                                }
                                Spacer(Modifier.height(PandoraSpacing.space4))
                                Text(item.body, color = PandoraTheme.colors.textPrimary, style = PandoraTypography.bodyMedium)
                                // createdAt 是 UTC 时间戳，按业务时区换算显示（否则东八区差 8 小时）
                                Text(com.pandora6ix.app.data.businessDateTimeLabel(item.createdAt, businessTimezone).ifBlank { item.createdAt.replace('T', ' ').take(16) }, color = PandoraTheme.colors.textPrimary, style = PandoraTypography.caption, modifier = Modifier.padding(top = PandoraSpacing.space8))
                            }
                        }
                    }
                }
            }
        }
    }
}
/** AI 地图：保持规划中占位，不扩展业务；占位插图 64dp、品牌 Coral 仅作小面积装饰 */
@Composable private fun ProfileScreen(user: UserDto, notificationsEnabled: Boolean, showReviewCenter: Boolean, pendingReviewCount: Int, canManageTags: Boolean, userRepo: UserRepository, scrollState: ScrollState, onOpenTagManage: () -> Unit, onOpenReviews: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val avatarScope = rememberCoroutineScope()
    val prefs = remember(user.id) { context.getSharedPreferences("profile_avatar_" + user.id, Context.MODE_PRIVATE) }
    var avatarType by remember(user.id) { mutableStateOf(prefs.getString("avatarType", "color") ?: "color") }
    var avatarColorIndex by remember(user.id) { mutableStateOf(prefs.getInt("avatarColor", 0)) }
    var avatarPath by remember(user.id) { mutableStateOf(prefs.getString("avatarPath", "") ?: "") }
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
            try { userRepo.uploadAvatar(user.id, bitmap) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { Toast.makeText(context, "头像未同步，请重新选择后重试", Toast.LENGTH_SHORT).show() }
        }
    }
    fun syncDefaultAvatar(colorIndex: Int) {
        val bitmap = android.graphics.Bitmap.createBitmap(256, 256, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(AvatarPalette[colorIndex].toArgb())
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 100f
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val name = user.displayName.ifBlank { user.name }.take(1).ifBlank { "我" }
        canvas.drawText(name, 128f, 128f - (paint.ascent() + paint.descent()) / 2, paint)
        avatarScope.launch {
            try { userRepo.uploadAvatar(user.id, bitmap) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { Toast.makeText(context, "头像未同步，请重新选择后重试", Toast.LENGTH_SHORT).show() }
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
        if (ok) cameraFile?.let {
            saveAvatar("photo", path = it.absolutePath)
            uploadAvatarFile(Uri.fromFile(it))
        }
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
    Column(Modifier.fillMaxSize().padding(horizontal = PandoraSpacing.space16)) {
        PandoraPageTitle("我的")
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(scrollState).padding(vertical = PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
            PandoraPanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Row(Modifier.padding(PandoraSpacing.panelPadding), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clickable { showAvatarMenu = true }) {
                        RemoteAvatar(user.id, displayName, 64.dp, 23.sp, userRepo) {
                            AvatarView(avatarType, avatarColorIndex, avatarPath, displayName, 64.dp)
                        }
                        Box(Modifier.size(20.dp).align(Alignment.BottomEnd).clip(PandoraShapes.circle).background(PandoraTheme.colors.textPrimary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PhotoCamera, "更换头像", Modifier.size(12.dp), tint = PandoraTheme.colors.elevatedSurface)
                        }
                    }
                    Column(Modifier.padding(start = PandoraSpacing.space16)) {
                        Text(displayName, style = PandoraTypography.titleLarge, color = PandoraTheme.colors.textPrimary)
                        Text(user.jobTitle ?: "未设置职位", color = PandoraTheme.colors.textSecondary)
                    }
                }
            }
            if (showAvatarMenu) PandoraDialog(onDismissRequest = { showAvatarMenu = false }, contentPadding = PaddingValues(vertical = PandoraSpacing.space16)) {
                Column {
                    Text("更换头像", style = PandoraTypography.titleMedium, modifier = Modifier.padding(horizontal = PandoraSpacing.space24, vertical = PandoraSpacing.space8))
                    PandoraListItem("从相册选择", Modifier.padding(horizontal = PandoraSpacing.space8), onClick = { showAvatarMenu = false; pickFromGallery() })
                    PandoraListItem("拍照", Modifier.padding(horizontal = PandoraSpacing.space8), onClick = { showAvatarMenu = false; cameraPermission.launch(Manifest.permission.CAMERA) })
                    PandoraListItem("使用默认头像", Modifier.padding(horizontal = PandoraSpacing.space8), onClick = { showAvatarMenu = false; showAvatarPalette = true })
                    Row(Modifier.fillMaxWidth().padding(horizontal = PandoraSpacing.space16), horizontalArrangement = Arrangement.End) {
                        PandoraDialogDismiss("取消", { showAvatarMenu = false })
                    }
                }
            }
            if (showAvatarPalette) PandoraAlertDialog(
                onDismissRequest = { showAvatarPalette = false },
                title = { Text("默认头像") },
                text = {
                    LazyVerticalGrid(columns = GridCells.Fixed(4), modifier = Modifier.height(200.dp), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                        items(AvatarPalette.size) { i ->
                            Box(
                                Modifier.size(52.dp).clip(CircleShape).background(AvatarPalette[i])
                                    .border(2.dp, if (avatarType == "color" && avatarColorIndex == i) PandoraTheme.colors.textPrimary else Color.Transparent, CircleShape)
                                    .clickable { saveAvatar("color", colorIndex = i); syncDefaultAvatar(i); showAvatarPalette = false },
                                contentAlignment = Alignment.Center
                            ) { Text(displayName.take(1), color = Color.White, style = PandoraTypography.titleMedium) }
                        }
                    }
                },
                confirmButton = { PandoraDialogConfirm("完成", { showAvatarPalette = false }) }
            )
            if (showGallery) PandoraAlertDialog(
                onDismissRequest = { showGallery = false },
                title = { Text("选择头像照片") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        Text(if (galleryUris.isEmpty()) "当前没有可访问的照片。" else "仅显示已授权可访问的照片；点击照片设为头像。", style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary)
                        if (galleryUris.isEmpty()) {
                            TextButton({ galleryPermission.launch(photoPerms()) }, Modifier.fillMaxWidth()) { Text("重新授权 / 管理可访问的照片", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
                        } else {
                            LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.height(380.dp), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                                items(galleryUris.size) { i ->
                                    val uri = galleryUris[i]
                                    val bmp = remember(uri) { decodeUriThumbnail(context, uri) }
                                    Box(Modifier.size(96.dp).clip(PandoraShapes.medium).background(PandoraTheme.colors.primaryContainer).clickable { importImage(uri); showGallery = false }, contentAlignment = Alignment.Center) {
                                        if (bmp != null) Image(bmp.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        else CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = PandoraTheme.colors.primary)
                                    }
                                }
                            }
                            TextButton({ galleryPermission.launch(photoPerms()) }, Modifier.fillMaxWidth()) { Text("管理可访问的照片（增删）", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
                        }
                    }
                },
                confirmButton = { PandoraDialogConfirm("关闭", { showGallery = false }) }
            )
            if (showReviewCenter) {
                SettingsCard {
                    SettingsRow(icon = Icons.Default.FactCheck, title = "审批中心", onClick = onOpenReviews) {
                        if (pendingReviewCount > 0) {
                            Text(
                                if (pendingReviewCount >= 99) "99+" else "$pendingReviewCount", style = PandoraTypography.caption, color = PandoraTheme.colors.textOnPrimary, fontWeight = FontWeight.Medium,
                                modifier = Modifier.clip(CircleShape).background(PandoraTheme.colors.error).padding(horizontal = PandoraSpacing.space8, vertical = PandoraSpacing.space2)
                            )
                            Spacer(Modifier.width(PandoraSpacing.space8))
                        }
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = PandoraTheme.colors.textTertiary)
                    }
                }
            }
            SettingsCard {
                // 仅 FOUNDER/DEPARTMENT_MANAGER 可见标签管理入口
                if (canManageTags) {
                    SettingsRow(icon = Icons.Default.Label, title = "标签管理", onClick = onOpenTagManage) {
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = PandoraTheme.colors.textTertiary)
                    }
                    DashedDivider(PandoraTheme.colors.border)
                }
                SettingsRow(icon = Icons.Default.Tune, title = "全部设置", onClick = onOpenSettings) {
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = PandoraTheme.colors.textTertiary)
                }
            }
            PandoraPanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Column(Modifier.padding(PandoraSpacing.panelPadding), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
                    Text("组织信息", style = PandoraTypography.titleSmall, modifier = Modifier.semantics { heading() })
                    ProfileInfoRow("所属企业", user.enterprise?.name ?: "未分配")
                    DashedDivider(PandoraTheme.colors.border)
                    ProfileInfoRow("所属部门", user.department ?: user.organization?.name ?: "未分配")
                    DashedDivider(PandoraTheme.colors.border)
                    // 角色显示中文；未分配直属上级时明确说明（数据来源 /me.supervisorUserId）
                    ProfileInfoRow("当前角色", user.roles.joinToString("、") { roleLabel(it) }.ifBlank { "未分配" })
                    ProfileInfoRow("直属上级", user.supervisorName?.takeIf { it.isNotBlank() } ?: "未配置（请联系管理员设置）")
                }
            }
            Spacer(Modifier.height(PandoraSpacing.space16))
        }
    }
}

@Composable private fun ProfileInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
        Text(label, Modifier.widthIn(min = 72.dp), color = PandoraTheme.colors.textSecondary, style = PandoraTypography.bodyMedium)
        Text(value, Modifier.weight(1f), style = PandoraTypography.bodyLarge)
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
@Composable private fun DetailCard(title: String, body: String, @Suppress("UNUSED_PARAMETER") color: Color, maxLines: Int = 2, onClick: () -> Unit = {}) {
    PandoraPanel(Modifier.fillMaxWidth(), onClick = onClick) {
        Text(title, style = PandoraTypography.titleSmall)
        Spacer(Modifier.height(PandoraSpacing.space4))
        Text(body, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textSecondary, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}
/** 完整详情独立加载，避免日历简略列表刷新覆盖执行记录。 */
@Composable
private fun FullTaskDetail(id: String, repo: TaskRepository, tags: List<TagDto>, businessTimezone: String?, currentUser: UserDto?, userRepo: UserRepository, onBack: () -> Unit, onChanged: () -> Unit) {
    var task by remember(id) { mutableStateOf<TaskDto?>(null) }
    var loading by remember(id) { mutableStateOf(true) }
    var error by remember(id) { mutableStateOf<String?>(null) }
    var revision by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id, revision) {
        loading = true
        error = null
        try {
            task = repo.task(id)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            error = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
        } finally { loading = false }
    }
    when {
        loading || error != null -> Column(Modifier.fillMaxSize()) {
            PageHeader("任务详情", onBack = onBack)
            Column(Modifier.padding(PandoraSpacing.space20), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                if (loading) CircularProgressIndicator(color = PandoraTheme.colors.primary)
                else {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    TextButton({ revision++ }) { Text("重试", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
                }
            }
        }
        else -> DetailScreen(id, task, tags, repo, onBack, onChanged = { revision++; onChanged() }, businessTimezone = businessTimezone, currentUser = currentUser, userRepo = userRepo)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun DetailScreen(title: String, dto: TaskDto?, availableTags: List<TagDto>, taskRepo: TaskRepository, onBack: () -> Unit, onChanged: () -> Unit, businessTimezone: String? = null, currentUser: UserDto? = null, userRepo: UserRepository? = null) {
    val isLog = title.startsWith("日志 · ")
    val logContent = if (isLog) title.substringAfter("§", "") else null
    val displayTitle = dto?.title ?: if (isLog) title.substringBefore("§") else title
    val item = dto?.toTaskItem()
    androidx.activity.compose.BackHandler(onBack = onBack)
    val scope = rememberCoroutineScope()
    // 任务说明弹窗：状态标签（已完成/未完成）+ 阶段性内容记录（不做百分比）
    // 可执行关系与更新权限：无权限时详情只做展示（只读状态与已有记录）
    val ownerId = SessionStore(LocalContext.current).ownerId
    val detailAssignment = dto?.authorizedAssignments?.firstOrNull { it.recipientUserId == ownerId }
    val executionNames = remember(dto?.id, dto?.version) { mutableStateMapOf<String,String>().apply {
        dto?.authorizedAssignments?.forEach { assignment ->
            assignment.recipientName?.takeIf { it.isNotBlank() }?.let { put(assignment.recipientUserId, it) }
        }
        currentUser?.let { put(it.id, it.displayName.ifBlank { it.name }) }
    } }
    LaunchedEffect(dto?.id, dto?.version, currentUser?.id) {
        val ids = dto?.authorizedAssignments?.map { it.recipientUserId }.orEmpty()
        if (currentUser?.capabilities?.manage != true || ids.all { !executionNames[it].isNullOrBlank() }) return@LaunchedEffect
        try {
            var cursor: String? = null
            val seen = mutableSetOf<String>()
            do {
                val result = taskRepo.recipientCandidatePage(cursor = cursor, limit = 100)
                executionNames.putAll(result.items.filter { it.displayName.isNotBlank() }.associate { it.id to it.displayName })
                cursor = result.nextCursor
                check(cursor == null || seen.add(cursor))
            } while (cursor != null && ids.any { executionNames[it].isNullOrBlank() })
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { /* The authorized detail remains usable if legacy name lookup fails. */ }
    }

    val canUpdateTask = detailAssignment?.allowedActions?.any { it == "UPDATE_PROGRESS" || it == "COMPLETE_PERSONAL" || it == "REQUEST_COMPLETION" } == true ||
        (dto?.seriesId != null && dto.kind == "PERSONAL" && dto.reviewStatus == "APPROVED" && detailAssignment != null)
    var repeatStatus by remember { mutableStateOf<String?>(null) }
    var editOpen by remember { mutableStateOf(false) }
    var stageNote by remember { mutableStateOf("") }
    var stageBusy by remember { mutableStateOf(false) }
    var stageError by remember { mutableStateOf<String?>(null) }
    val stageKeys = remember { mutableStateMapOf<String, String>() }
    fun stageKey(action: String, id: String): String = stageKeys.getOrPut("$action:$id") { UUID.randomUUID().toString() }
    fun markCompleted(assignment: AssignmentDto) {
        if (dto?.seriesId != null) { repeatStatus = "COMPLETED"; return }
        stageBusy = true; stageError = null
        scope.launch {
            try {
                if (assignment.allowedActions.none { it == "COMPLETE_PERSONAL" || it == "REQUEST_COMPLETION" }) {
                    stageError = "当前账号无权变更该任务状态"; stageBusy = false; return@launch
                }
                taskRepo.scopedStatus(dto!!.id, ScopedStatusDto(
                    scope = "SINGLE", workStatus = "COMPLETED", note = stageNote.trim().ifBlank { null },
                    versions = listOf(ScopeVersionDto(dto.id, dto.version, assignment.id, assignment.version))
                ), stageKey("complete", assignment.id))
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
    fun decideCompletion(request: CompletionRequestDto, decision: String) {
        if (stageBusy) return
        stageBusy = true; stageError = null
        scope.launch {
            try {
                taskRepo.decideCompletion(requestId = request.id, decision = decision, expectedVersion = request.version,
                    idempotencyKey = stageKey("decision:$decision", request.id))
                onChanged()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled
            } catch (error: ApiException) {
                stageError = error.friendlyMessage()
                if (error.httpStatus == 409) onChanged()
            } catch (_: Exception) { stageError = "操作失败，请重试"
            } finally { stageBusy = false }
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
    if (editOpen && dto != null && item != null) TaskEntrySheet(
        initialDate = item.start, canCreatePersonal = currentUser?.capabilities?.createPersonalTasks == true, canCreateDelegated = currentUser?.capabilities?.createDelegatedTasks == true,
        availableTags = availableTags, taskRepo = taskRepo, businessTimezone = businessTimezone, currentUser = currentUser,
        onDismiss = { editOpen = false }, onCreated = { onChanged() }, editingTask = dto)
    if (repeatStatus != null && dto != null) com.pandora6ix.app.ui.task.SeriesStatusEditor(
        dto, taskRepo, repeatStatus!!, businessTimezone, stageNote.trim().ifBlank { null },
        onDismiss = { repeatStatus = null }, onSaved = { repeatStatus = null; stageNote = ""; onChanged() })
    Column(Modifier.fillMaxSize()) {
        PageHeader(displayTitle, null, onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space16)) {
            if (dto != null && item != null) {
                Surface(Modifier.fillMaxWidth(), shape = PandoraShapes.large, color = PandoraTheme.colors.panelSurface, border = BorderStroke(PandoraDimensions.borderWidth, PandoraTheme.colors.border)) {
                    Column(Modifier.padding(PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("任务信息", Modifier.weight(1f), style = PandoraTypography.titleSmall)
                            // 有修改待上级审批（pendingEditReviewId 非空）或任务已完成时锁定编辑
                            val taskCompleted = dto.authorizedAssignments.isNotEmpty() && dto.authorizedAssignments.all { it.executionStatus == "COMPLETED" }
                            if (dto.pendingEditReviewId == null && !taskCompleted && dto.allowedActions.any { it == "EDIT_TASK" || it == "EDIT_PENDING" }) {
                                TextButton({ editOpen = true }) { Text("编辑", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
                            }
                        }
                        Text(dto.title, style = PandoraTypography.titleMedium)
                        if (dto.pendingEditReviewId != null) Text("修改待上级审批", color = PandoraTheme.colors.primary, style = PandoraTypography.bodyMedium)
                        if (dto.tags.isNotEmpty()) {
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                                dto.tags.sortedBy { if (it.id == dto.primaryTagId) 0 else 1 }.forEach { tag ->
                                    PandoraTagChip(tag.name, tag.colorHex, primary = tag.id == dto.primaryTagId)
                                }
                            }
                        }
                        HorizontalDivider(color = PandoraTheme.colors.border)
                        Text("任务说明", style = PandoraTypography.titleSmall)
                        Text(dto.description?.takeIf { it.isNotBlank() } ?: "暂无备注", style = PandoraTypography.bodyLarge)
                        HorizontalDivider(color = PandoraTheme.colors.border)
                        Text("时间节点", style = PandoraTypography.titleSmall)
                        Text("${item.start.year}/${item.start.month}/${item.start.day}—${item.end.year}/${item.end.month}/${item.end.day}\n${taskTimeLabel(item)}", style = PandoraTypography.bodyLarge)
                        item.recurrenceLabel?.let { Text("重复安排 · $it", style = PandoraTypography.bodyLarge) }
                        HorizontalDivider(color = PandoraTheme.colors.border)
                        Text("重要程度 · ${priorityMeta(dto.priorityLevel).second}")
                        if (dto.companyWide) Text("全公司可见", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary)
                    }
                }
                Surface(Modifier.fillMaxWidth(), shape = PandoraShapes.large, color = PandoraTheme.colors.panelSurface, border = BorderStroke(PandoraDimensions.borderWidth, PandoraTheme.colors.border)) {
                    Column(Modifier.padding(PandoraSpacing.space16), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                        Text("执行情况", style = PandoraTypography.titleSmall)
                        if (dto.authorizedAssignments.isEmpty()) Text("暂无可见执行关系", color = PandoraTheme.colors.textSecondary)
                        dto.authorizedAssignments.forEachIndexed { i, assignment ->
                            if (i > 0) HorizontalDivider(color = PandoraTheme.colors.border)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                                val name = executionNames[assignment.recipientUserId]?.takeIf { it.isNotBlank() } ?: "姓名暂不可用"
                                RemoteAvatar(assignment.recipientUserId, name, 32.dp, 14.sp, userRepo)
                                // 被驳回的任务执行关系仍停在「待审批」态，展示为「被驳回」更符合用户预期
                                val execLabel = if (dto.reviewStatus == "REJECTED" && assignment.executionStatus == "PENDING_APPROVAL") "被驳回"
                                    else executionStatusLabel(assignment.executionStatus)
                                Text("$name · $execLabel", style = PandoraTypography.bodyLarge)
                            }

                            val pending = dto.completionRequests.lastOrNull { it.assignmentId == assignment.id && it.decision == "PENDING" }
                            if ("DECIDE_COMPLETION" in assignment.allowedActions && pending != null) {
                                pending.submittedNote?.takeIf { it.isNotBlank() }?.let { Text(it, style = PandoraTypography.bodyLarge) }
                                Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                                    PandoraButton("验收通过", { decideCompletion(pending, "ACCEPT") }, enabled = !stageBusy)
                                    PandoraButton("不通过", { decideCompletion(pending, "REJECT") }, variant = PandoraButtonVariant.Destructive, enabled = !stageBusy)
                                }
                            }

                        }
                        stageError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = PandoraTypography.bodyMedium) }
                        if (stageBusy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = PandoraTheme.colors.primary)
                        if (canUpdateTask && detailAssignment != null) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text("更新执行状态", style = PandoraTypography.labelLarge)
                            Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                                FilterChip(selected = detailAssignment.executionStatus == "COMPLETED", enabled = !stageBusy,
                                    onClick = { if (detailAssignment.executionStatus != "COMPLETED") markCompleted(detailAssignment) }, label = { Text("已完成") })
                                if (dto.seriesId != null && dto.kind == "PERSONAL" && detailAssignment.executionStatus == "COMPLETED") {
                                    PandoraButton("恢复未完成", { repeatStatus = "NOT_STARTED" }, variant = PandoraButtonVariant.Outline, enabled = !stageBusy)
                                }
                            }
                            if ("UPDATE_PROGRESS" in detailAssignment.allowedActions) {
                                PandoraTextField(stageNote, { stageNote = it; stageError = null }, Modifier.fillMaxWidth(),
                                    label = { Text("阶段性内容记录") }, minLines = 3, enabled = !stageBusy)
                                TextButton({ recordStage(detailAssignment) }, enabled = !stageBusy && stageNote.isNotBlank()) { Text("保存记录", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Text("执行记录", style = PandoraTypography.titleSmall)
                        val records = dto.progressRecords.filter { !it.note.isNullOrBlank() }.sortedByDescending { it.recordedAt }
                        if (records.isEmpty()) Text("暂无执行记录", color = PandoraTheme.colors.textSecondary)
                        records.forEach { record ->
                            Surface(Modifier.fillMaxWidth(), color = PandoraTheme.colors.surface, shape = PandoraShapes.medium,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                Column(Modifier.padding(PandoraSpacing.space12), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                                    Text(record.note.orEmpty(), style = PandoraTypography.bodyLarge)
                                    record.recordedAt?.let { Text(businessDateTimeLabel(it, businessTimezone), style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary) }
                                }
                            }
                        }

                    }
                }
            } else if (isLog) {
                Text(logContent ?: "", color = PandoraTheme.colors.textSecondary, style = PandoraTypography.bodyLarge)
            } else {
                Text("任务不存在或已被移除。", color = PandoraTheme.colors.textSecondary, style = PandoraTypography.bodyLarge)
            }
        }
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

    Column(Modifier.fillMaxSize().background(PandoraTheme.colors.appBackground)) {
        PageHeader("设置", onBack = onClose)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PandoraSpacing.space16, vertical = PandoraSpacing.space12)) {
                SettingsSectionLabel("通用")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.TextFields,
                        title = "字号大小",
                        onClick = { showFontDialog = true }
                    ) {
                        Text(fontScaleLabel(fontScale), color = PandoraTheme.colors.textTertiary, style = PandoraTypography.bodyMedium)
                    }
                    DashedDivider(PandoraTheme.colors.border)
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "修改登录密码",
                        onClick = { showPasswordDialog = true }
                    )
                    DashedDivider(PandoraTheme.colors.border)
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "切换主题",
                        onClick = { showThemeDialog = true }
                    ) {
                        Text(appThemes.firstOrNull { it.key == themeKey }?.label ?: "暖米", color = PandoraTheme.colors.textTertiary, style = PandoraTypography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(PandoraSpacing.space16))
                // 通知提醒独立成一级分区：主开关 + 三个同级开关（提前量是截止时间临近的子项）
                SettingsSectionLabel("通知提醒")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "通知提醒",
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
                            Text("去设置", color = PandoraTheme.colors.primary, style = PandoraTypography.labelLarge)
                        }
                        DashedDivider(PandoraTheme.colors.border)
                    }
                    // 总控关闭则全部静默：不展示分项
                    if (notificationsEnabled) {
                        DashedDivider(PandoraTheme.colors.border)
                        SettingsRow(
                            icon = Icons.Default.Assignment,
                            title = "新任务提醒",
                            onClick = { onNewTaskChange(!newTaskEnabled) }
                        ) {
                            ReminderSwitch(newTaskEnabled, onNewTaskChange)
                        }
                        DashedDivider(PandoraTheme.colors.border)
                        SettingsRow(
                            icon = Icons.Default.Schedule,
                            title = "截止时间临近",
                            onClick = { onDeadlineChange(!deadlineEnabled) }
                        ) {
                            ReminderSwitch(deadlineEnabled, onDeadlineChange)
                        }
                        // 提前提醒时间是「截止时间临近」的子项：缩进展示
                        if (deadlineEnabled) {
                            SubValueRow(
                                icon = Icons.Default.Timelapse,
                                title = "提前提醒时间",
                                value = leadLabel(leadMinutes),
                                onClick = { showLeadDialog = true }
                            )
                        }
                        DashedDivider(PandoraTheme.colors.border)
                        SettingsRow(
                            icon = Icons.Default.SyncAlt,
                            title = "任务状态变化",
                            onClick = { onStatusChangeChange(!statusChangeEnabled) }
                        ) {
                            ReminderSwitch(statusChangeEnabled, onStatusChangeChange)
                        }
                    }
                }
                Spacer(Modifier.height(PandoraSpacing.space16))
                SettingsSectionLabel("数据")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.CleaningServices,
                        title = "清除缓存",
                        onClick = { showClearCacheDialog = true }
                    ) {
                        Text(AppCache.formatSize(totalCache), color = PandoraTheme.colors.textTertiary, style = PandoraTypography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(PandoraSpacing.space16))
                SettingsSectionLabel("关于")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.Info,
                        title = "关于我们",
                        onClick = { showAboutDialog = true }
                    ) {
                        Text("v0.1.0", color = PandoraTheme.colors.textTertiary, style = PandoraTypography.bodyMedium)
                    }
                    DashedDivider(PandoraTheme.colors.border)
                    SettingsRow(icon = Icons.Default.Logout, title = "退出登录", isDestructive = true, onClick = { showLogoutDialog = true })
                }
            }
            if (toast != null) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(PandoraSpacing.space16), contentAlignment = Alignment.Center) {
                    PandoraToast(toast!!, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
            }
        }
    }

    if (showFontDialog) {
        PandoraAlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("选择字号大小") },
            text = {
                Column {
                    val options = listOf(0.9f to "小", 1.0f to "标准", 1.15f to "大")
                    options.forEach { (v, label) ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onFontScaleChange(v); showFontDialog = false }.padding(vertical = PandoraSpacing.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = fontScale == v,
                                onClick = { onFontScaleChange(v); showFontDialog = false },
                                colors = RadioButtonDefaults.colors(selectedColor = PandoraTheme.colors.primary)
                            )
                            Spacer(Modifier.width(PandoraSpacing.space8))
                            Text(label, style = PandoraTypography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { PandoraDialogConfirm("关闭", { showFontDialog = false }) }
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
        PandoraAlertDialog(
            onDismissRequest = { showLeadDialog = false },
            title = { Text("提前提醒时间") },
            text = {
                Column {
                    Text("常用时长：", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                    REMINDER_LEAD_OPTIONS.forEach { minutes ->
                        Row(
                            Modifier.fillMaxWidth().clickable { onLeadMinutesChange(minutes); showLeadDialog = false }.padding(vertical = PandoraSpacing.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = leadMinutes == minutes,
                                onClick = { onLeadMinutesChange(minutes); showLeadDialog = false },
                                colors = RadioButtonDefaults.colors(selectedColor = PandoraTheme.colors.primary)
                            )
                            Spacer(Modifier.width(PandoraSpacing.space8))
                            Text("截止前 ${leadLabel(minutes)}", style = PandoraTypography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(PandoraSpacing.space12))
                    Text("自定义（精确到分钟）：", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PandoraTextField(
                            value = hourText,
                            onValueChange = { input -> hourText = input.filter(Char::isDigit).take(2); leadError = null },
                            label = { Text("小时") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                        Spacer(Modifier.width(PandoraSpacing.space12))
                        PandoraTextField(
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
                    if (leadError != null) Text(leadError!!, color = MaterialTheme.colorScheme.error, style = PandoraTypography.caption)
                }
            },
            confirmButton = { PandoraDialogConfirm("确定", ::applyCustom) },
            dismissButton = { PandoraDialogDismiss("取消", { showLeadDialog = false }) }
        )
    }
    if (showPasswordDialog) {
        var oldPw by remember { mutableStateOf("") }
        var newPw by remember { mutableStateOf("") }
        var confirmPw by remember { mutableStateOf("") }
        var pwError by remember { mutableStateOf<String?>(null) }
        var changing by remember { mutableStateOf(false) }
        PandoraAlertDialog(
            onDismissRequest = { if (!changing) showPasswordDialog = false },
            title = { Text("修改密码") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                    PandoraTextField(oldPw, { oldPw = it; pwError = null }, label = { Text("当前密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !changing)
                    PandoraTextField(newPw, { newPw = it; pwError = null }, label = { Text("新密码（至少 10 个字符）") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !changing)
                    PandoraTextField(confirmPw, { confirmPw = it; pwError = null }, label = { Text("确认新密码") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, enabled = !changing)
                    pwError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = PandoraTypography.caption) }
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
                }) { Text(if (changing) "提交中…" else "确定", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.primary) }
            },
            dismissButton = { PandoraDialogDismiss("取消", { if (!changing) showPasswordDialog = false }) }
        )
    }
    if (showThemeDialog) {
        PandoraAlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("切换主题") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    appThemes.forEach { t ->
                        Row(
                            Modifier.fillMaxWidth().clip(PandoraShapes.small).clickable { onThemeChange(t.key); showThemeDialog = false }.padding(vertical = PandoraSpacing.space8),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 卡通主题显示场景缩略图；原主题为背景色 + 主色描边的色块
                            val art = t.art
                            if (art != null) androidx.compose.foundation.Image(
                                androidx.compose.ui.res.painterResource(art.scene), null,
                                Modifier.size(width = PandoraDimensions.avatarProfile, height = PandoraDimensions.icon + 8.dp).clip(PandoraShapes.small)
                                    .background(t.colors.appBackground).border(PandoraDimensions.borderWidth, t.colors.cardBorder, PandoraShapes.small),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            ) else Box(Modifier.size(PandoraDimensions.icon).clip(PandoraShapes.circle).background(t.colors.appBackground).border(PandoraDimensions.focusWidth, t.colors.primary, PandoraShapes.circle))
                            Spacer(Modifier.width(PandoraSpacing.space12))
                            Text(t.label, Modifier.weight(1f), style = PandoraTypography.bodyMedium)
                            RadioButton(selected = t.key == themeKey, onClick = { onThemeChange(t.key); showThemeDialog = false }, colors = RadioButtonDefaults.colors(selectedColor = PandoraTheme.colors.primary))
                        }
                    }
                    Text("切换后仅更改界面外观（配色、图标与插画），数据不受影响。", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                }
            },
            confirmButton = { PandoraDialogConfirm("关闭", { showThemeDialog = false }) }
        )
    }
    if (showClearCacheDialog) {
        PandoraAlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除缓存") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    if (cacheItems.isEmpty()) {
                        Text("暂无可清理的缓存。", color = PandoraTheme.colors.textTertiary)
                    } else {
                        Text("共 ${AppCache.formatSize(totalCache)}，明细：", color = PandoraTheme.colors.textSecondary)
                        cacheItems.forEach { item ->
                            Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                                Text(item.label, Modifier.weight(1f), style = PandoraTypography.bodyMedium)
                                Text(AppCache.formatSize(item.size), style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textTertiary)
                            }
                        }
                        Text("不影响登录状态、设置与当前使用的头像。", style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
                    }
                }
            },
            confirmButton = {
                TextButton({
                    cacheItems.forEach { item -> item.files.forEach { runCatching { it.delete() } } }
                    cacheVersion++
                    showClearCacheDialog = false
                    toast = if (totalCache > 0) "已清除 ${AppCache.formatSize(totalCache)}" else "没有需要清除的缓存"
                }) { Text("清除", style = PandoraTypography.labelLarge, color = if (totalCache > 0) PandoraTheme.colors.primary else PandoraTheme.colors.textDisabled) }
            },
            dismissButton = { PandoraDialogDismiss("取消", { showClearCacheDialog = false }) }
        )
    }
    if (showAboutDialog) {
        PandoraAlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Pandora6ix") },
            text = { Text("版本 v0.1.0\n一个温暖的六维工作生活导图。") },
            confirmButton = { PandoraDialogConfirm("好的", { showAboutDialog = false }) }
        )
    }
    if (showLogoutDialog) {
        var loggingOut by remember { mutableStateOf(false) }
        PandoraAlertDialog(
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
            }) { Text(if (loggingOut) "退出中…" else "退出", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.error) } },
            dismissButton = { PandoraDialogDismiss("取消", { if (!loggingOut) showLogoutDialog = false }) }
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text, style = PandoraTypography.caption,
        color = PandoraTheme.colors.textTertiary, fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = PandoraSpacing.space4, bottom = PandoraSpacing.space8)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    PandoraPanel(Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp), content = content)
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
    val tint = if (isDestructive) PandoraTheme.colors.error else PandoraTheme.colors.primary
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(horizontal = PandoraSpacing.space16, vertical = PandoraSpacing.space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(PandoraDimensions.avatarList).clip(PandoraShapes.circle).background(if (isDestructive) PandoraTheme.colors.errorContainer else PandoraTheme.colors.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, Modifier.size(PandoraDimensions.icon - 4.dp), tint = tint)
        }
        Spacer(Modifier.width(PandoraSpacing.space12))
        Column(Modifier.weight(1f)) {
            Text(title, style = PandoraTypography.bodyLarge, color = if (isDestructive) PandoraTheme.colors.error else PandoraTheme.colors.textPrimary)
            if (!subtitle.isNullOrBlank())
                Text(subtitle, style = PandoraTypography.caption, color = PandoraTheme.colors.textTertiary)
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
    value: String,
    onClick: () -> Unit
) {
    Row(
        // 紧凑子项：随「截止时间临近」开关收展，不再占据独立大行
        // 子项与上方标题文字左对齐：16 内距 + 40 图标 + 12 间距
        Modifier.fillMaxWidth().heightIn(min = PandoraDimensions.touchTarget).clickable(onClick = onClick).padding(start = 68.dp, end = PandoraSpacing.space16, top = PandoraSpacing.space4, bottom = PandoraSpacing.space4),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, Modifier.size(PandoraDimensions.iconSmall), tint = PandoraTheme.colors.textSecondary)
        Spacer(Modifier.width(PandoraSpacing.space12))
        Column(Modifier.weight(1f)) {
            Text(title, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.textPrimary)
        }
        Text(value, color = PandoraTheme.colors.primary, style = PandoraTypography.bodyMedium)
        Spacer(Modifier.width(PandoraSpacing.space4))
        Icon(Icons.Default.ChevronRight, null, Modifier.size(18.dp), tint = PandoraTheme.colors.textTertiary)
    }
}

/** 提醒弹窗：新任务 / 截止临近 / 状态变化统一入口，逐条确认 */
@Composable
private fun ReminderDialog(event: ReminderEvent, onDismiss: () -> Unit) {
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(event.title) },
        text = { Text(event.message, color = PandoraTheme.colors.textSecondary, style = PandoraTypography.bodyLarge) },
        confirmButton = { PandoraDialogConfirm("知道了", onDismiss) }
    )
}
