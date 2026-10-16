package com.pandora6ix.app

import android.Manifest
import android.content.Context
import android.graphics.BitmapFactory
import android.provider.MediaStore
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import com.pandora6ix.app.ui.theme.*
import kotlinx.coroutines.launch
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = SessionStore(this)
        val api = ApiClient(session)
        val auth = AuthRepository(api, session)
        setContent { PandoraApp(session, api, auth) }
    }
}

private enum class Route { HOME, VIEW, LOGS, AI, ME }

/** 任务操作面板覆盖的动作集合（进度/自完成/申请验收/验收决定），用于详情页入口判定 */
private val TASK_ACTIONS = setOf("UPDATE_PROGRESS", "COMPLETE_PERSONAL", "REQUEST_COMPLETION", "DECIDE_COMPLETION")
private data class NavItem(val route: Route, val label: String, val icon: ImageVector)
private data class HomePanelDef(val title: String, val items: List<String>)

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

// 应用主题：预置背景配色方案（key 用于 rememberSaveable 持久化当前选择）
private data class AppTheme(val key: String, val label: String, val background: Color, val backgroundDeep: Color, val dashboard: Color)
private val appThemes = listOf(
    AppTheme("warm", "暖米", Cream, CreamDeep, WarmDashboard),
    AppTheme("mint", "薄荷", Color(0xFFE8F4EC), Color(0xFFD9EAE0), Color(0xFFE2F0E7)),
    AppTheme("sky", "晴空", Color(0xFFE9F1FA), Color(0xFFD9E7F4), Color(0xFFE3EDF8)),
    AppTheme("sakura", "樱花", Color(0xFFFBF0F3), Color(0xFFF7E1E8), Color(0xFFF9E7EB)),
    AppTheme("lilac", "紫藤", Color(0xFFF1EDFA), Color(0xFFE4DEF3), Color(0xFFEAE5F7))
)
private val LocalAppTheme = staticCompositionLocalOf { appThemes.first() }

@Composable
fun PandoraApp(session: SessionStore, api: ApiClient, auth: AuthRepository) {
    PandoraTheme {
        var route by rememberSaveable { mutableStateOf(Route.HOME.name) }
        var modeName by rememberSaveable { mutableStateOf(CalendarMode.WEEK.name) }
        var dateKey by rememberSaveable { mutableStateOf(todayDate().let { "${it.year}-${it.month}-${it.day}" }) }
        var detail by rememberSaveable { mutableStateOf<String?>(null) }
        var homeDetail by remember { mutableStateOf<HomeDetail?>(null) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var showReviews by rememberSaveable { mutableStateOf(false) }
        // 标签管理覆盖层（我的页进入）
        var showTagManage by rememberSaveable { mutableStateOf(false) }
        // 进度记录浮层：从任务详情进入，显示当前任务的执行关系
        var progressTaskId by remember { mutableStateOf<String?>(null) }
        var fontScale by rememberSaveable { mutableStateOf(1f) }
        var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
        var themeKey by rememberSaveable { mutableStateOf("warm") }
        var currentUser by remember { mutableStateOf<UserDto?>(null) }
        var authChecked by remember { mutableStateOf(false) }
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

        // 启动恢复会话：已有 token 时用 GET /me 校验；失败（含 401）回到登录页
        LaunchedEffect(Unit) {
            currentUser = if (session.token != null) runCatching { auth.me() }.getOrNull() else null
            authChecked = true
        }
        // 任意带 token 请求返回 401：清理会话并回登录（切换到主线程更新状态）
        DisposableEffect(api) {
            val main = Handler(Looper.getMainLooper())
            api.onSessionExpired = { main.post { currentUser = null; showSettings = false } }
            onDispose { api.onSessionExpired = null }
        }
        val calendarRepo = remember { CalendarRepository(api) }
        val journalRepo = remember { JournalRepository(api) }
        val selectionRepo = remember { SelectionRepository(api) }
        val taskRepo = remember { TaskRepository(api) }
        val tagRepo = remember { TagRepository(api) }
        val appScope = rememberCoroutineScope()
        fun refreshTags() {
            appScope.launch {
                runCatching { calendarRepo.tags() }.getOrNull()?.let { list ->
                    serverTags.clear(); serverTags.addAll(list)
                }
            }
        }
        // 登录后加载企业标签目录与本人统计
        var statistics by remember { mutableStateOf<StatisticsDto?>(null) }
        LaunchedEffect(currentUser?.id) {
            if (currentUser == null) return@LaunchedEffect
            runCatching { calendarRepo.tags() }.getOrNull()?.let { list ->
                serverTags.clear(); serverTags.addAll(list)
            }
            runCatching { calendarRepo.statistics() }.getOrNull()?.let { statistics = it }
        }
        // 视图页：按当前模式与日期加载真实任务；日/周/月走 /calendar，年视图按 /tasks 全年区间查询
        var calLoading by remember { mutableStateOf(false) }
        var calError by remember { mutableStateOf<String?>(null) }
        var calRetry by remember { mutableStateOf(0) }
        // 年视图任务范围：mine=我的任务 / managed=我管理的 / created=我创建的
        var taskScope by rememberSaveable { mutableStateOf("mine") }
        LaunchedEffect(current, modeName, dateKey, currentUser?.id, calRetry, taskScope) {
            if (current != Route.VIEW || currentUser == null) return@LaunchedEffect
            calLoading = true; calError = null
            val anchor = "%04d-%02d-%02d".format(date.year, date.month, date.day)
            try {
                val loaded = when (CalendarMode.valueOf(modeName)) {
                    CalendarMode.DAY -> calendarRepo.calendar("day", anchor).tasks
                    CalendarMode.WEEK -> calendarRepo.calendar("week", anchor).tasks
                    CalendarMode.MONTH -> calendarRepo.calendar("month", anchor).tasks
                    CalendarMode.YEAR -> calendarRepo.tasks(scope = taskScope, from = "%04d-01-01".format(date.year), to = "%04d-12-31".format(date.year)).items
                }
                taskItems.clear(); taskItems.addAll(loaded.map { it.toTaskItem() })
                taskDtoById.clear(); taskDtoById.putAll(loaded.associateBy { it.id })
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: Exception) {
                // 加载失败显示空视图与重试入口，不保留过期内容
                calError = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
                taskItems.clear(); taskDtoById.clear()
            } finally {
                calLoading = false
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
        Scaffold(containerColor = theme.background, bottomBar = { NavigationBar(containerColor = theme.backgroundDeep) { nav.forEach { item -> NavigationBarItem(selected = current == item.route, onClick = { route = item.route.name; detail = null; homeDetail = null; showSettings = false; showReviews = false; showTagManage = false; progressTaskId = null }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = WarmOrange, selectedTextColor = WarmOrange, indicatorColor = theme.backgroundDeep)) } } }) { padding ->
            Surface(Modifier.padding(padding).fillMaxSize(), color = theme.background) {
                when {
                    homeDetail != null -> HomeItemDetailScreen(dashboardRepo, homeDetail!!) { homeDetail = null }
                    showSettings -> SettingsScreen(fontScale = fontScale, onFontScaleChange = { fontScale = it }, notificationsEnabled = notificationsEnabled, onNotificationsChange = { notificationsEnabled = it }, themeKey = themeKey, onThemeChange = { themeKey = it }, auth = auth, onLoggedOut = { currentUser = null }, onClose = { showSettings = false })
                    showReviews -> ReviewScreen(taskRepo, serverTags) { showReviews = false }
                    showTagManage -> TagManageScreen(
                        tagRepo,
                        // TODO(权限)：标签管理权限待定级——未来按角色/等级填充（如 capabilities.createTags 与更细的修改/删除权限）
                        TagPermissions(),
                        reloadTags = { refreshTags() }
                    ) { showTagManage = false }
                    detail != null -> {
                        val dto = taskDtoById[detail]
                        DetailScreen(detail!!, dto, serverTags, onBack = { detail = null; progressTaskId = null }, onRecordProgress = dto
                            ?.takeIf { t -> t.authorizedAssignments.any { a -> a.allowedActions.any { it in TASK_ACTIONS } } }
                            ?.let { { progressTaskId = detail } })
                        progressTaskId?.let { id -> taskDtoById[id]?.let { t ->
                            ProgressSheet(t, taskRepo, onDismiss = { progressTaskId = null }, onChanged = { calRetry++ })
                        } }
                    }
                    else -> AnimatedContent(current, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { target ->
                        when (target) {
                            Route.HOME -> HomeScreen(dashboardRepo, selectionRepo, onOpenHome = { homeDetail = it }, onOpenSettings = { showSettings = true })
                            Route.VIEW -> CalendarScreen(CalendarMode.valueOf(modeName), date, taskItems, canCreatePersonal, canCreateDelegated, serverTags, taskRepo, { modeName = it.name }, { dateKey = "${it.year}-${it.month}-${it.day}" }, { calRetry++ }, { detail = it.id }, taskScope = taskScope, onTaskScopeChange = { taskScope = it }, loading = calLoading, error = calError, onRetry = { calRetry++ })
                            Route.LOGS -> LogsScreen(journalRepo, serverTags.toList()) { detail = it }
                            Route.AI -> AiMapScreen()
                            Route.ME -> ProfileScreen(currentUser!!, statistics, notificationsEnabled, showReviewCenter, onOpenTagManage = { showTagManage = true }, onOpenReviews = { showReviews = true }, onOpenSettings = { showSettings = true })
                        }
                    }
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
            }, Modifier.fillMaxWidth().height(50.dp), enabled = !loading, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(14.dp)) { Text(if (loading) "登录中…" else "登录", fontSize = 16.sp) }
        }
    }
}

@Composable private fun HomeScreen(dashboardRepo: DashboardRepository, selectionRepo: SelectionRepository, onOpenHome: (HomeDetail) -> Unit, onOpenSettings: () -> Unit) {
    var expandedPanel by rememberSaveable { mutableStateOf<String?>(null) }
    var notifications by rememberSaveable { mutableStateOf(false) }
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
        HomePanelDef("公司十大重要事项", companyPager.items.map { it.title }),
        HomePanelDef("公司十大派发任务", taskPager.items.map { it.title }),
        HomePanelDef("个人十大重要事项", d?.personalImportantEntries?.entries?.map { it.content } ?: emptyList()),
        HomePanelDef("个人日志", entryPager.items.map { it.content })
    )
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
        onOpenHome(HomeDetail(panels[panelIndex].title, index, text, WarmOrange, kind, id))
    }
    Box(Modifier.fillMaxSize().background(LocalAppTheme.current.dashboard)) {
      Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(todayDate().shortLabelWithWeekday(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { notifications = true }) { Surface(shape = RoundedCornerShape(50), color = CreamDeep) { Icon(Icons.Default.Email, "邮箱", Modifier.padding(9.dp), tint = Ink) } }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onOpenSettings) { Surface(shape = RoundedCornerShape(50), color = CreamDeep) { Icon(Icons.Default.Settings, "设置", Modifier.padding(9.dp), tint = Ink) } }
        }
        Box(Modifier.fillMaxWidth().weight(1f)) {
            when {
                dashError != null -> Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(dashError!!, color = Ink.copy(alpha = .7f))
                    Button({ loadDashboard() }, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(12.dp)) { Text("重试") }
                }
                d == null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(color = WarmOrange, strokeWidth = 3.dp)
                    Text("正在加载…", color = Ink.copy(alpha = .6f))
                }
                else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomePanel(panels[0], Modifier.weight(1f).fillMaxHeight(), onExpand = { expandedPanel = panels[0].title }, onItemClick = { i, t -> openItem(0, i, t) })
                HomePanel(panels[1], Modifier.weight(1f).fillMaxHeight(), onExpand = { expandedPanel = panels[1].title }, onItemClick = { i, t -> openItem(1, i, t) })
            }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomePanel(
                    panels[2],
                    Modifier.weight(1f).fillMaxHeight(),
                    trailing = {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(WarmOrange.copy(alpha = .35f)).clickable { editingPersonal = true },
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Default.Edit, "编辑个人事项", Modifier.size(15.dp), tint = Ink) }
                    },
                    onExpand = { expandedPanel = panels[2].title },
                    onItemClick = { i, t -> openItem(2, i, t) }
                )
                HomePanel(panels[3], Modifier.weight(1f).fillMaxHeight(), onExpand = { expandedPanel = panels[3].title }, onItemClick = { i, t -> openItem(3, i, t) })
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
                      list.forEachIndexed { i, item ->
                          Row(
                              Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 10.dp),
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
          NotificationDrawer(onClose = { notifications = false })
      }
    }
}

@Composable private fun HomePanel(
    panel: HomePanelDef,
    modifier: Modifier,
    accent: Color = WarmOrange,
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
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                list.forEachIndexed { i, text ->
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
                        Text("共 ${selected.size}/10 · 候选打钩加入 · 保存后导图更新", color = Ink.copy(alpha = .6f), fontSize = 12.sp)
                    }
                    Button({ save() }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink), shape = RoundedCornerShape(12.dp)) { Text(if (saving) "保存中…" else "保存", fontWeight = FontWeight.Bold) }
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
                        Button({ retry++ }, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(12.dp)) { Text("重试") }
                    }
                    else -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                        if (selected.isEmpty()) {
                            Text("暂无正式事项；可在下方候选中勾选，也可保存空选择（视为主动清空）。", color = Ink.copy(alpha = .55f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        selected.forEachIndexed { i, item ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(accent.copy(alpha = .3f)), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(item.content, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text(parseIsoDate(item.workDate)?.shortLabel() ?: item.workDate, fontSize = 11.sp, color = Ink.copy(alpha = .5f))
                                }
                                IconButton({ toggle(item) }, Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Close, "移出", Modifier.size(18.dp), tint = CoralDark)
                                }
                            }
                            if (i < selected.lastIndex) DashedDivider(accent)
                        }
                        // 分割线：上为正式事项，下为候选事项
                        Spacer(Modifier.height(14.dp))
                        DashedDivider(accent)
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text("候选事项", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Text("来自最近十个自然日日报", color = Ink.copy(alpha = .55f), fontSize = 12.sp, modifier = Modifier.weight(1f))
                            Text("${candidates.size} 条", color = Ink.copy(alpha = .5f), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        if (candidates.isEmpty()) {
                            Text("最近十个自然日没有可勾选的日报条目。", color = Ink.copy(alpha = .55f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        candidates.forEach { entry ->
                            val id = entry.entryId.ifBlank { entry.id }
                            val isSelected = id in selectedIds
                            val canAdd = isSelected || selected.size < 10
                            Row(
                                Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // 勾选圆形：点击加入/移出正式事项
                                Box(
                                    Modifier.size(26.dp).clip(CircleShape)
                                        .background(if (isSelected) accent else if (canAdd) accent.copy(alpha = .12f) else Color.Transparent)
                                        .border(1.5.dp, if (isSelected || canAdd) accent else Ink.copy(alpha = .25f), CircleShape)
                                        .clickable(enabled = canAdd) { toggle(entry) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) Icon(Icons.Default.Check, "已选", Modifier.size(16.dp), tint = Ink)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    entry.content,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(parseIsoDate(entry.workDate)?.shortLabel() ?: entry.workDate, fontSize = 12.sp, color = Ink.copy(alpha = .5f))
                            }
                        }
                        if (selected.size >= 10 && candidates.isNotEmpty()) {
                            Text("已达 10 条上限，移出正式事项后可继续勾选。", Modifier.fillMaxWidth().padding(top = 6.dp), textAlign = TextAlign.Center, color = Ink.copy(alpha = .55f), fontSize = 12.sp)
                        }
                    }
                }
                saveError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp)) }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
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
                            Button({ attempt++ }, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(12.dp)) { Text("重试") }
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

/** publishedAt(ISO) 距今是否在 24 小时内（minSdk 24 无 java.time，用 Calendar 手工解析；解析失败按可编辑处理） */
private fun within24h(iso: String?): Boolean {
    if (iso.isNullOrBlank() || iso.length < 19) return true
    return try {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(
            iso.substring(0, 4).toInt(), iso.substring(5, 7).toInt() - 1, iso.substring(8, 10).toInt(),
            iso.substring(11, 13).toInt(), iso.substring(14, 16).toInt(), iso.substring(17, 19).toInt()
        )
        var createdUtc = cal.timeInMillis
        val tail = iso.substring(19)
        if (tail.isNotEmpty() && (tail[0] == '+' || tail[0] == '-')) {
            val oh = tail.substring(1, 3).toInt()
            val om = if (tail.length >= 6) tail.substring(4, 6).toInt() else 0
            createdUtc -= (if (tail[0] == '+') 1 else -1) * (oh * 3600L + om * 60L) * 1000L
        }
        createdUtc >= System.currentTimeMillis() - 24L * 3_600_000L
    } catch (e: Exception) {
        true
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable private fun LogsScreen(repo: JournalRepository, availableTags: List<TagDto>, onOpen: (String) -> Unit) {
    var expandedLogs by rememberSaveable { mutableStateOf(false) }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var historyFilter by rememberSaveable { mutableStateOf("全部日期") }
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
    fun todayIso(): String { val t = todayDate(); return "%04d-%02d-%02d".format(t.year, t.month, t.day) }
    fun isoOf(d: DemoDate): String = "%04d-%02d-%02d".format(d.year, d.month, d.day)
    fun threeDays(): List<DemoDate> { val t = todayDate(); return listOf(t, t.plusDays(-1), t.plusDays(-2)) }
    fun loadJournals() {
        loading = true; loadError = null
        scope.launch {
            try {
                val t = todayDate()
                val start = t.plusDays(-30)
                val from = "%04d-%02d-%02d".format(start.year, start.month, start.day)
                val to = "%04d-%02d-%02d".format(t.year, t.month, t.day)
                journals = repo.journals(from = from, to = to).items.sortedByDescending { it.workDate }
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
    val todayJournal = journals.firstOrNull { it.workDate == todayIso() }
    fun dateLabel(iso: String): String = parseIsoDate(iso)?.shortLabel() ?: iso
    val flatEntries = journals.flatMap { j -> j.entries.sortedBy { it.displayOrder }.map { j.workDate to it } }
    val filteredEntries = if (historyFilter == "全部日期") flatEntries else flatEntries.filter { dateLabel(it.first) == historyFilter }
    val filterOptions = listOf("全部日期") + journals.map { dateLabel(it.workDate) }.distinct()
    fun openEditor(dateKey: String) {
        val draft = savedJournalDraft
        editorDateKey = draft?.dateKey ?: dateKey
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
            PageHeader("日志")
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                item { Button({ openEditor(todayIso()) }, Modifier.fillMaxWidth(), enabled = !publishing, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(if (todayJournal != null) "补充今日工作" else "记录今日工作") } }
                item {
                    // 最近三日：创建后 24 小时内的日报可编辑/删除
                    val recent = journals.filter { it.workDate in threeDays().map { d -> isoOf(d) } }
                    if (recent.isNotEmpty()) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("最近三日 · 可在 24 小时内修改或删除", fontSize = 13.sp, color = Ink.copy(alpha = .55f))
                        recent.forEach { j ->
                            val editable = within24h(j.publishedAt)
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(14.dp)) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(dateLabel(j.workDate), fontWeight = FontWeight.SemiBold)
                                        Text("${j.entries.size} 条 · " + if (editable) "可编辑/删除" else "已超过 24 小时", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
                                        if (j.tags.isNotEmpty()) Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            j.tags.forEach { t ->
                                                Text(t.name, fontSize = 11.sp, color = Ink, maxLines = 1, modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(tagHexColor(t.colorHex)).padding(horizontal = 6.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    if (editable) {
                                        TextButton({ openEditor(j.workDate) }, enabled = !publishing, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("编辑", color = Ink) }
                                        TextButton({ deleteError = null; deletingJournal = j }, enabled = !publishing, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("删除", color = CoralDark) }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box {
                            Button({ filterOpen = true }, contentPadding = PaddingValues(horizontal = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = CreamDeep, contentColor = Ink), shape = RoundedCornerShape(12.dp)) { Text(historyFilter); Icon(Icons.Default.ArrowDropDown, null) }
                            DropdownMenu(filterOpen, { filterOpen = false }, containerColor = CreamDeep) {
                                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                                    filterOptions.forEach { option -> DropdownMenuItem({ Text(option) }, { historyFilter = option; filterOpen = false; expandedLogs = false }) }
                                }
                            }
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
                        Button({ loadRetry++ }, colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(12.dp)) { Text("重试") }
                    }
                }
                items(if (expandedLogs) filteredEntries else filteredEntries.take(5)) { (workDate, entry) ->
                    val time = entry.recordedAt?.substringAfter("T")?.take(5).orEmpty()
                    val title = if (time.isBlank()) dateLabel(workDate) else "${dateLabel(workDate)} · $time"
                    DetailCard(title, entry.content, Color.White) { onOpen("日志 · $title§${entry.content}") }
                }
                if (filteredEntries.size > 5) item { TextButton({ expandedLogs = !expandedLogs }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.textButtonColors(containerColor = CreamDeep, contentColor = Ink)) { Text(if (expandedLogs) "收起" else "展开") } }
                if (!loading && journals.isNotEmpty() && filteredEntries.isEmpty()) item { Text("所选日期暂无日志", color = Ink.copy(alpha = .6f)) }
                if (!loading && journals.isEmpty() && loadError == null) item { Text("最近 30 天暂无日志，点击上方按钮记录今日工作。", color = Ink.copy(alpha = .6f)) }
            }
        }
        if (writing) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)).clickable { closeEditor() })
        if (writing) {
            androidx.activity.compose.BackHandler { closeEditor() }
            val imeVisible = WindowInsets.isImeVisible
            Card(Modifier.align(Alignment.Center).imePadding().fillMaxWidth(.9f).fillMaxHeight(if (imeVisible) .92f else .8f).border(2.dp, CreamDeep, RoundedCornerShape(24.dp)).clickable { }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WarmCard)) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(if (editingJournal != null) "编辑日志" else "记录日志", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ closeEditor() }) { Icon(Icons.Default.Close, "关闭") } }
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
                        val editorEditable = editingJournal == null || within24h(editingJournal?.publishedAt)
                        if (!editorEditable) Text("已超过 24 小时，此日志不可修改；如需更正请联系管理员。", fontSize = 12.sp, color = Ink.copy(alpha = .55f))
                        Text("· 标签（可多选，先选的为主）", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                        if (tags.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            tags.forEach { name ->
                                val tagColor = availableTags.firstOrNull { it.name == name }?.let { tagHexColor(it.colorHex) } ?: CreamDeep
                                FilterChip(selected = true, onClick = { tags = tags - name }, label = { Text(name, fontSize = 13.sp) }, colors = FilterChipDefaults.filterChipColors(containerColor = tagColor, labelColor = Ink, selectedContainerColor = tagColor, selectedLabelColor = Ink))
                            }
                        }
                        OutlinedButton({ showTagPicker = true }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), border = BorderStroke(1.dp, Ink.copy(alpha = .25f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Icon(Icons.Default.Add, "选择标签", Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("选择标签", fontSize = 13.sp) }
                        Text("· 完成的工作", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    }
                    Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        entryDrafts.forEachIndexed { i, draft ->
                            Row(verticalAlignment = Alignment.Top) {
                                OutlinedTextField(draft.content, { value -> entryDrafts = entryDrafts.toMutableList().also { it[i] = draft.copy(content = value) } }, Modifier.weight(1f), placeholder = { Text(if (editingJournal == null) "第 ${i + 1} 条：记录完成的工作" else "第 ${i + 1} 条") }, colors = mutedColors, minLines = 2)
                                if (editingJournal == null && entryDrafts.size > 1) IconButton({ entryDrafts = entryDrafts.filterIndexed { idx, _ -> idx != i } }, Modifier.size(40.dp)) { Icon(Icons.Default.Close, "删除此条", Modifier.size(16.dp)) }
                            }
                        }
                        if (editingJournal == null) TextButton({ entryDrafts = entryDrafts + EntryDraft(null, "") }, enabled = !publishing) { Icon(Icons.Default.Add, null, Modifier.size(14.dp)); Spacer(Modifier.width(4.dp)); Text("添加一条", color = WarmOrange, fontWeight = FontWeight.Bold) }
                    }
                    publishError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton({ closeEditor() }, Modifier.weight(1f), enabled = !publishing, colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Text("取消") }
                        Button(
                            { publish() },
                            Modifier.weight(1f),
                            enabled = !publishing && (entryDrafts.any { it.content.isNotBlank() } || (editingJournal?.entries?.any { e -> entryDrafts.none { it.entryId == e.entryId } } == true)) && (editingJournal == null || within24h(editingJournal?.publishedAt)),
                            colors = ButtonDefaults.buttonColors(containerColor = Ink),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text(if (publishing) "发布中…" else "发布") }
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

@Composable private fun BoxScope.NotificationDrawer(onClose: () -> Unit) {
    Card(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.75f).fillMaxHeight(), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = WarmCard), elevation = CardDefaults.cardElevation(14.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("消息", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClose) { Icon(Icons.Default.Close, "关闭") } }
            Text("站内消息接口尚未开放", color = Ink.copy(alpha = .6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
            Text("消息中心建设中。\n\n任务审核结果请在视图页的任务中查看。", color = Ink.copy(alpha = .55f), fontSize = 14.sp, lineHeight = 22.sp)
        }
    }
}
@Composable private fun AiMapScreen() { Column(Modifier.fillMaxSize().padding(16.dp)) { PageHeader("AI地图"); Spacer(Modifier.height(48.dp)); Icon(Icons.Default.Info, null, Modifier.size(70.dp).align(Alignment.CenterHorizontally), tint = Coral); Text("AI 地图", Modifier.fillMaxWidth().padding(top = 18.dp), textAlign = TextAlign.Center, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("功能规划中", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, color = Ink.copy(alpha = .6f)) } }
@Composable private fun ProfileScreen(user: UserDto, statistics: StatisticsDto?, notificationsEnabled: Boolean, showReviewCenter: Boolean, onOpenTagManage: () -> Unit, onOpenReviews: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("profile_avatar", Context.MODE_PRIVATE) }
    var avatarType by remember { mutableStateOf(prefs.getString("avatarType", "color") ?: "color") }
    var avatarColorIndex by remember { mutableStateOf(prefs.getInt("avatarColor", 0)) }
    var avatarPath by remember { mutableStateOf(prefs.getString("avatarPath", "") ?: "") }
    var showAvatarMenu by remember { mutableStateOf(false) }
    var showAvatarPalette by remember { mutableStateOf(false) }
    fun saveAvatar(type: String, colorIndex: Int = avatarColorIndex, path: String = avatarPath) {
        avatarType = type; avatarColorIndex = colorIndex; avatarPath = path
        prefs.edit().putString("avatarType", type).putInt("avatarColor", colorIndex).putString("avatarPath", path).apply()
    }
    fun importImage(uri: Uri?) {
        if (uri == null) return
        runCatching {
            val dir = File(context.filesDir, "avatars").apply { mkdirs() }
            val dest = File(dir, "picked_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)!!.use { input -> dest.outputStream().use { output -> input.copyTo(output) } }
            saveAvatar("photo", path = dest.absolutePath)
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
        PageHeader("我的", "个人资料")
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CreamDeep), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(64.dp).clickable { showAvatarMenu = true }) {
                        AvatarView(avatarType, avatarColorIndex, avatarPath, displayName, 64.dp)
                        Box(Modifier.size(20.dp).align(Alignment.BottomEnd).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.PhotoCamera, "更换头像", Modifier.size(12.dp), tint = Color.White)
                        }
                    }
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(displayName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(user.jobTitle ?: "未设置职位", color = Ink.copy(alpha = .7f))
                        Text("登录名 ${user.loginName}", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
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
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("任务统计", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        ProfileInfoRow("任务总数", "${st.total}")
                        ProfileInfoRow("已完成", "${st.completed}")
                        ProfileInfoRow("未完成", "${st.uncompleted}")
                        Text("口径：本人已批准任务；进度 100% 不计为完成。", fontSize = 12.sp, color = Ink.copy(alpha = .5f))
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("组织信息", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    ProfileInfoRow("所属企业", user.enterprise?.name ?: "未设置")
                    DashedDivider(Ink.copy(alpha = .18f))
                    ProfileInfoRow("所属部门", user.department ?: user.organization?.name ?: "未设置")
                    ProfileInfoRow("当前角色", user.roles.joinToString("、").ifBlank { "未设置" })
                    ProfileInfoRow("业务时区", user.businessTimezone ?: "未设置")
                }
            }
            SettingsSectionLabel("基础设置")
            SettingsCard {
                SettingsRow(icon = Icons.Default.Label, title = "标签管理", onClick = onOpenTagManage) {
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Ink.copy(alpha = .4f))
                }
                DashedDivider(Ink.copy(alpha = .18f))
                if (showReviewCenter) {
                    SettingsRow(icon = Icons.Default.FactCheck, title = "审批中心", onClick = onOpenReviews) {
                        Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Ink.copy(alpha = .4f))
                    }
                    DashedDivider(Ink.copy(alpha = .18f))
                }
                SettingsRow(icon = Icons.Default.Tune, title = "全部设置", onClick = onOpenSettings) {
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Ink.copy(alpha = .4f))
                }
                DashedDivider(Ink.copy(alpha = .18f))
                SettingsRow(icon = Icons.Default.Notifications, title = "消息通知", onClick = onOpenSettings) {
                    Text(if (notificationsEnabled) "已开启" else "已关闭", color = Ink.copy(alpha = .55f), fontSize = 13.sp)
                }
            }
            Text("关于 Pandora", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Android 端正在接入真实后端\n个人资料来自真实接口，业务数据接入进行中。", color = Ink.copy(alpha = .6f), lineHeight = 22.sp)
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
@Composable private fun DetailCard(title: String, body: String, color: Color, onClick: () -> Unit = {}) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .78f)), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, fontSize = 13.sp, color = Ink.copy(alpha = .75f), maxLines = 2, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun DetailScreen(title: String, dto: TaskDto?, availableTags: List<TagDto>, onBack: () -> Unit, onRecordProgress: (() -> Unit)? = null) {
    val isLog = title.startsWith("日志 · ")
    val logContent = if (isLog) title.substringAfter("§", "") else null
    val displayTitle = dto?.title ?: if (isLog) title.substringBefore("§") else title
    val item = dto?.toTaskItem()
    androidx.activity.compose.BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        PageHeader(displayTitle, if (isLog) null else "任务详情", onBack, actions = { onRecordProgress?.let { cb -> TextButton(cb) { Text("记录进度") } } })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (!isLog) { Text("${displayTitle}详情", fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)) }
            if (dto != null && item != null) {
                Text("任务说明", fontWeight = FontWeight.Bold)
                Text(dto.description?.takeIf { it.isNotBlank() } ?: "暂无备注", lineHeight = 22.sp)
                Spacer(Modifier.height(10.dp))
                DetailCard("时间节点", "${item.start.year}/${item.start.month}/${item.start.day}—${item.end.year}/${item.end.month}/${item.end.day}\n${taskTimeLabel(item)}", Color(0xFFE2E2E2))
                Spacer(Modifier.height(10.dp))
                DetailCard("类型与状态", "${kindLabelOf(dto.kind)} · ${reviewStatusLabel(dto.reviewStatus)}", Color(0xFFE2E2E2))
                Spacer(Modifier.height(10.dp))
                DetailCard("重要程度", "★".repeat(dto.priorityLevel.coerceIn(1, 5)), Color(0xFFE2E2E2))
                Spacer(Modifier.height(10.dp))
                Text("执行情况", fontWeight = FontWeight.Bold)
                if (dto.authorizedAssignments.isEmpty()) Text("暂无可见执行关系", color = Ink.copy(alpha = .6f), lineHeight = 22.sp)
                dto.authorizedAssignments.forEachIndexed { i, a ->
                    Text("${if (dto.authorizedAssignments.size > 1) "执行关系 ${i + 1} · " else ""}${executionStatusLabel(a.executionStatus)} · 进度 ${a.progressPercent}%", lineHeight = 22.sp)
                }
                if (dto.tags.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("标签", fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        dto.tags.forEach { tag ->
                            val tagColor = runCatching { Color(android.graphics.Color.parseColor(tag.colorHex)) }.getOrDefault(Ink.copy(alpha = .18f))
                            Text(if (tag.id == dto.primaryTagId) "${tag.name} · 主" else tag.name, Modifier.clip(RoundedCornerShape(10.dp)).background(tagColor).padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 12.sp, color = Ink)
                        }
                    }
                }
            } else if (isLog) {
                Text(logContent ?: "", color = Ink.copy(alpha = .75f), fontSize = 16.sp, lineHeight = 26.sp)
            } else {
                Text("任务不存在或已被移除。", color = Ink.copy(alpha = .75f), lineHeight = 24.sp)
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
    var toast by remember { mutableStateOf<String?>(null) }
    // 真实缓存清点：草稿（任务/日报未发布内容）+ 被替换的旧头像副本
    val context = LocalContext.current
    val avatarPrefs = remember { context.getSharedPreferences("profile_avatar", Context.MODE_PRIVATE) }
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
                    DashedDivider(Ink.copy(alpha = 0.18f))
                    SettingsRow(
                        icon = Icons.Default.Notifications,
                        title = "通知提醒",
                        onClick = { onNotificationsChange(!notificationsEnabled) }
                    ) {
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onNotificationsChange,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Cream,
                                checkedTrackColor = WarmOrange,
                                uncheckedThumbColor = Cream,
                                uncheckedTrackColor = Ink.copy(alpha = 0.25f)
                            )
                        )
                    }
                    DashedDivider(Ink.copy(alpha = 0.18f))
                    SettingsRow(
                        icon = Icons.Default.Lock,
                        title = "修改密码",
                        onClick = { showPasswordDialog = true }
                    ) {
                        Text("已设置", color = Ink.copy(alpha = 0.55f), fontSize = 13.sp)
                    }
                    DashedDivider(Ink.copy(alpha = 0.18f))
                    SettingsRow(
                        icon = Icons.Default.Palette,
                        title = "切换主题",
                        onClick = { showThemeDialog = true }
                    ) {
                        Text(appThemes.firstOrNull { it.key == themeKey }?.label ?: "暖米", color = Ink.copy(alpha = 0.55f), fontSize = 13.sp)
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
                    DashedDivider(Ink.copy(alpha = 0.18f))
                    SettingsRow(icon = Icons.Default.Logout, title = "退出登录", isDestructive = true, onClick = { showLogoutDialog = true })
                }
            }
            if (toast != null) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp)) {
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
                        newPw != confirmPw -> "两次输入的新密码不一致"
                        else -> null
                    }
                    if (pwError == null) {
                        changing = true
                        scope.launch {
                            try {
                                auth.changePassword(oldPw, newPw)
                                // 成功 204：服务端已使会话失效，回登录页重新认证
                                showPasswordDialog = false
                                onLoggedOut()
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
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
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
        Text(title, fontSize = 15.sp, color = if (isDestructive) CoralDark else Ink, modifier = Modifier.weight(1f))
        trailing()
    }
}

private fun fontScaleLabel(s: Float): String = when {
    s <= 0.9f -> "小"
    s >= 1.15f -> "大"
    else -> "标准"
}
