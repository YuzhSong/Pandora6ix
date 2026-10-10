package com.pandora6ix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.mock.*
import com.pandora6ix.app.ui.calendar.*
import com.pandora6ix.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PandoraApp() } }
}

private enum class Route { HOME, VIEW, LOGS, AI, ME }
private data class NavItem(val route: Route, val label: String, val icon: ImageVector)
private data class HomePanelDef(val title: String, val items: List<String>)
private data class HomeDetail(val section: String, val index: Int, val text: String, val accent: Color)

@Composable
fun PandoraApp() {
    PandoraTheme {
        var route by rememberSaveable { mutableStateOf(Route.HOME.name) }
        var modeName by rememberSaveable { mutableStateOf(CalendarMode.WEEK.name) }
        var dateKey by rememberSaveable { mutableStateOf(todayDate().let { "${it.year}-${it.month}-${it.day}" }) }
        var detail by rememberSaveable { mutableStateOf<String?>(null) }
        var homeDetail by remember { mutableStateOf<HomeDetail?>(null) }
        var showSettings by rememberSaveable { mutableStateOf(false) }
        var fontScale by rememberSaveable { mutableStateOf(1f) }
        var notificationsEnabled by rememberSaveable { mutableStateOf(true) }
        val personalItems = remember { mutableStateListOf<String>().apply { addAll(MockData.personalImportant) } }
        val logs = remember { mutableStateListOf<WorkLog>().apply { addAll(MockData.logs) } }
        val tasks = remember { mutableStateListOf<WorkTask>().apply { addAll(MockData.companyTasks); addAll(MockData.timedTasks) } }
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
        CompositionLocalProvider(LocalDensity provides scaledDensity) {
        Scaffold(containerColor = Cream, bottomBar = { NavigationBar(containerColor = CreamDeep) { nav.forEach { item -> NavigationBarItem(selected = current == item.route, onClick = { route = item.route.name; detail = null; homeDetail = null; showSettings = false }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = WarmOrange, selectedTextColor = WarmOrange, indicatorColor = CreamDeep)) } } }) { padding ->
            Surface(Modifier.padding(padding).fillMaxSize(), color = Cream) {
                when {
                    homeDetail != null -> HomeItemDetailScreen(homeDetail!!) { homeDetail = null }
                    showSettings -> SettingsScreen(fontScale = fontScale, onFontScaleChange = { fontScale = it }, notificationsEnabled = notificationsEnabled, onNotificationsChange = { notificationsEnabled = it }, onClose = { showSettings = false })
                    detail != null -> DetailScreen(detail!!, tasks, onBack = { detail = null })
                    else -> AnimatedContent(current, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { target ->
                        when (target) {
                            Route.HOME -> HomeScreen(tasks, personalItems, logs, onOpenHome = { homeDetail = it }, onOpenSettings = { showSettings = true })
                            Route.VIEW -> CalendarScreen(CalendarMode.valueOf(modeName), date, tasks, MockData.role.canCreateTasks, { modeName = it.name }, { dateKey = "${it.year}-${it.month}-${it.day}" }, { if (MockData.role.canCreateTasks) tasks.add(it) }, { detail = it.id })
                            Route.LOGS -> LogsScreen(tasks, logs) { detail = it }
                            Route.AI -> AiMapScreen()
                            Route.ME -> ProfileScreen(notificationsEnabled, onOpenSettings = { showSettings = true })
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

@Composable private fun HomeScreen(tasks: List<WorkTask>, personalItems: SnapshotStateList<String>, logs: List<WorkLog>, onOpenHome: (HomeDetail) -> Unit, onOpenSettings: () -> Unit) {
    var expandedPanel by rememberSaveable { mutableStateOf<String?>(null) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    var editingPersonal by remember { mutableStateOf(false) }
    val panels = listOf(
        HomePanelDef("公司十大重要事项", MockData.companyHighlights),
        HomePanelDef("公司十大派发任务", tasks.filter { it.status != "草稿（演示）" }.take(10).map { it.title }),
        HomePanelDef("个人十大重要事项", personalItems),
        HomePanelDef("个人日志", logs.sortedByDescending { it.date.ordinal() }.map { it.content })
    )
    fun openItem(panel: HomePanelDef, index: Int, text: String) {
        onOpenHome(HomeDetail(panel.title, index, text, WarmOrange))
    }
    Box(Modifier.fillMaxSize().background(WarmDashboard)) {
      Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(MockData.demoToday.shortLabelWithWeekday(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { notifications = true }) { Surface(shape = RoundedCornerShape(50), color = CreamDeep) { Icon(Icons.Default.Email, "邮箱", Modifier.padding(9.dp), tint = Ink) } }
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onOpenSettings) { Surface(shape = RoundedCornerShape(50), color = CreamDeep) { Icon(Icons.Default.Settings, "设置", Modifier.padding(9.dp), tint = Ink) } }
        }
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomePanel(panels[0], Modifier.weight(1f).fillMaxHeight(), onExpand = { expandedPanel = panels[0].title }, onItemClick = { i, t -> openItem(panels[0], i, t) })
                HomePanel(panels[1], Modifier.weight(1f).fillMaxHeight(), onExpand = { expandedPanel = panels[1].title }, onItemClick = { i, t -> openItem(panels[1], i, t) })
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
                    onItemClick = { i, t -> openItem(panels[2], i, t) }
                )
                HomePanel(panels[3], Modifier.weight(1f).fillMaxHeight(), onExpand = { expandedPanel = panels[3].title }, onItemClick = { i, t -> openItem(panels[3], i, t) })
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
                  Text("共 ${selected.items.take(10).size} 条", color = Ink.copy(alpha = .55f), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp, bottom = 12.dp))
                  Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                      val list = selected.items.take(10)
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
                  }
              }
          }
      }
      if (editingPersonal) {
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).clickable { editingPersonal = false })
          PersonalImportantEditor(items = personalItems, onClose = { editingPersonal = false })
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

@Composable private fun PersonalImportantEditor(items: SnapshotStateList<String>, onClose: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf("") }
    var newText by remember { mutableStateOf("") }
    var confirmDelete by remember { mutableStateOf<Int?>(null) }
    // drag-to-reorder state
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var hoverIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val rowHeights = remember { mutableMapOf<Int, Float>() }
    val accent = WarmOrange

    val onItemTap: (Int, String) -> Unit = { idx, t ->
        editingIndex = idx
        editText = t
    }
    val onLongPress: (Int) -> Unit = { idx ->
        draggedIndex = idx
        hoverIndex = idx
        dragOffsetY = 0f
    }
    val onDragDelta: (Offset) -> Unit = { delta ->
        dragOffsetY += delta.y
        val from = draggedIndex
        if (from != null) {
            val draggedH = rowHeights[from] ?: 56f
            // natural top of dragged item = cumulative height of rows before it
            var naturalAcc = 0f
            for (k in 0 until from) naturalAcc += rowHeights[k] ?: 56f
            val draggedCenter = naturalAcc + draggedH / 2 + dragOffsetY
            // pick the item whose natural center is closest to the dragged center
            var bestTarget = from
            var bestDist = Float.MAX_VALUE
            var acc = 0f
            for (i in items.indices) {
                val h = rowHeights[i] ?: 56f
                val itemCenter = acc + h / 2
                val dist = kotlin.math.abs(itemCenter - draggedCenter)
                if (dist < bestDist) {
                    bestDist = dist
                    bestTarget = i
                }
                acc += h
            }
            hoverIndex = bestTarget
        }
    }
    val onDragEnd: () -> Unit = {
        val from = draggedIndex
        val to = hoverIndex
        if (from != null && to != null && from != to && from in items.indices && to in items.indices) {
            val moved = items.removeAt(from)
            items.add(to, moved)
        }
        draggedIndex = null
        hoverIndex = null
        dragOffsetY = 0f
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
                        Text("共 ${items.size}/10 · 长按整行拖动排序", color = Ink.copy(alpha = .6f), fontSize = 12.sp)
                    }
                    TextButton(onClick = onClose) { Text("完成", color = Ink, fontWeight = FontWeight.Bold) }
                }
                Spacer(Modifier.height(10.dp))
                Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    if (items.isEmpty()) {
                        Text("还没有事项，点击下方「添加新事项」开始记录。", color = Ink.copy(alpha = .55f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
                    }
                    items.forEachIndexed { i, text ->
                        val from = draggedIndex
                        val to = hoverIndex
                        val isDragged = from == i
                        val draggedH = (from?.let { rowHeights[it] } ?: 56f)
                        val displacement = when {
                            from == null || to == null -> 0f
                            from < to && i in (from + 1)..to -> -draggedH
                            from > to && i in to..(from - 1) -> draggedH
                            else -> 0f
                        }
                        val visualMod = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                if (isDragged) {
                                    translationY = dragOffsetY
                                    shadowElevation = 12f
                                    scaleX = 1.03f
                                    scaleY = 1.03f
                                } else {
                                    translationY = displacement
                                }
                            }
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (i % 2 == 0) Color.Transparent else accent.copy(alpha = .07f))
                        if (editingIndex == i) {
                            Column(visualMod.padding(12.dp)) {
                                OutlinedTextField(
                                    editText, { editText = it },
                                    Modifier.fillMaxWidth(),
                                    label = { Text("第 ${i + 1} 条") },
                                    singleLine = true
                                )
                                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                                    TextButton({ editingIndex = null; editText = "" }) { Text("取消", color = Ink.copy(alpha = .7f)) }
                                    Spacer(Modifier.width(8.dp))
                                    Button({
                                        val t = editText.trim()
                                        if (t.isNotEmpty()) items[i] = t
                                        editingIndex = null; editText = ""
                                    }, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink)) { Text("保存") }
                                }
                            }
                        } else {
                            val rowMod = visualMod
                                .onSizeChanged { rowHeights[i] = it.height.toFloat() }
                                .pointerInput(items.size) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { onLongPress(i) },
                                        onDrag = { change, dragAmount ->
                                            onDragDelta(dragAmount)
                                            change.consume()
                                        },
                                        onDragEnd = { onDragEnd() },
                                        onDragCancel = { draggedIndex = null; hoverIndex = null; dragOffsetY = 0f }
                                    )
                                }
                                .clickable { onItemTap(i, text) }
                            Row(rowMod.padding(horizontal = 4.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(accent.copy(alpha = .3f)), contentAlignment = Alignment.Center) {
                                    Text("${i + 1}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text,
                                    fontSize = 14.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton({ confirmDelete = i }, Modifier.size(34.dp)) {
                                    Icon(Icons.Default.Delete, "删除", Modifier.size(18.dp), tint = CoralDark)
                                }
                            }
                        }
                        if (i < items.lastIndex) DashedDivider(accent)
                    }
                    if (adding) {
                        Spacer(Modifier.height(10.dp))
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(accent.copy(alpha = .18f))
                                .padding(12.dp)
                        ) {
                            Text("新增事项", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            OutlinedTextField(
                                newText, { newText = it },
                                Modifier.fillMaxWidth().padding(top = 6.dp),
                                placeholder = { Text("输入要追踪的重要事项…") },
                                singleLine = true
                            )
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                                TextButton({ adding = false; newText = "" }) { Text("取消", color = Ink.copy(alpha = .7f)) }
                                Spacer(Modifier.width(8.dp))
                                Button({
                                    val t = newText.trim()
                                    if (t.isNotEmpty() && items.size < 10) items.add(t)
                                    adding = false; newText = ""
                                }, enabled = newText.isNotBlank() && items.size < 10, colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Ink)) { Text("添加") }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (!adding) {
                    if (items.size < 10) {
                        OutlinedButton(
                            { adding = true },
                            Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, accent),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
                        ) { Icon(Icons.Default.Add, null, tint = accent); Spacer(Modifier.width(6.dp)); Text("添加新事项") }
                    } else {
                        Text("已达 10 条上限", Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Ink.copy(alpha = .55f), fontSize = 12.sp)
                    }
                }
            }
        }
    }
    if (confirmDelete != null) {
        val idx = confirmDelete!!
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("删除第 ${idx + 1} 条？") },
            text = { Text("「${items.getOrNull(idx)?.take(24) ?: ""}」将被移除。") },
            confirmButton = { TextButton({ items.removeAt(idx); confirmDelete = null }) { Text("删除", color = CoralDark) } },
            dismissButton = { TextButton({ confirmDelete = null }) { Text("取消") } }
        )
    }
}

@Composable private fun HomeItemDetailScreen(detail: HomeDetail, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize().background(WarmDashboard)) {
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
                    Text(detail.text, fontSize = 19.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable private fun LogsScreen(tasks: List<WorkTask>, logs: SnapshotStateList<WorkLog>, onOpen: (String) -> Unit) {
    var expandedLogs by rememberSaveable { mutableStateOf(false) }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var historyFilter by rememberSaveable { mutableStateOf("全部日期") }
    var writing by rememberSaveable { mutableStateOf(false) }
    var draftLog by rememberSaveable { mutableStateOf("") }
    var draftTask by rememberSaveable { mutableStateOf("不关联任务") }
    var savedDraft by rememberSaveable { mutableStateOf(false) }
    var confirmPublish by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val allLogs = logs.sortedWith(compareByDescending<WorkLog> { it.date.ordinal() }.thenByDescending { it.time })
    val todayLog = allLogs.firstOrNull { it.date.ordinal() == todayDate().ordinal() }
    val todayReported = todayLog != null
    val filteredLogs = if (historyFilter == "全部日期") allLogs else allLogs.filter { it.date.shortLabel() == historyFilter }
    val mutedColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .7f), unfocusedBorderColor = Ink.copy(alpha = .3f), focusedLabelColor = Ink, cursorColor = Ink)
    fun closeEditor() {
        if (draftLog.isNotBlank() || draftTask != "不关联任务") confirmExit = true
        else writing = false
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            PageHeader("日志")
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                item { Button({ if (todayLog != null && !savedDraft) draftLog = todayLog.content; writing = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(when { savedDraft -> "继续编辑草稿"; todayReported -> "补充今日工作"; else -> "记录今日工作" }) } }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box {
                            Button({ filterOpen = true }, contentPadding = PaddingValues(horizontal = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = CreamDeep, contentColor = Ink), shape = RoundedCornerShape(12.dp)) { Text(historyFilter); Icon(Icons.Default.ArrowDropDown, null) }
                            DropdownMenu(filterOpen, { filterOpen = false }, containerColor = CreamDeep) {
                                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                                    (listOf("全部日期") + allLogs.map { it.date.shortLabel() }.distinct()).forEach { option -> DropdownMenuItem({ Text(option) }, { historyFilter = option; filterOpen = false; expandedLogs = false }) }
                                }
                            }
                        }
                    }
                }
                items(if (expandedLogs) filteredLogs else filteredLogs.take(5)) { log -> DetailCard("${log.date.shortLabel()} · ${log.time}", log.content, Color.White) { onOpen("日志 · ${log.time}§${log.content}") } }
                if (filteredLogs.size > 5) item { TextButton({ expandedLogs = !expandedLogs }, Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.textButtonColors(containerColor = CreamDeep, contentColor = Ink)) { Text(if (expandedLogs) "收起" else "展开") } }
                if (filteredLogs.isEmpty()) item { Text("所选日期暂无日志", color = Ink.copy(alpha = .6f)) }
            }
        }
        if (writing) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)).clickable { closeEditor() })
        if (writing) {
            androidx.activity.compose.BackHandler { closeEditor() }
            val imeVisible = WindowInsets.isImeVisible
            Card(Modifier.align(Alignment.Center).imePadding().fillMaxWidth(.9f).fillMaxHeight(if (imeVisible) .92f else .8f).border(2.dp, CreamDeep, RoundedCornerShape(24.dp)).clickable { }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WarmCard)) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text(if (todayReported) "补充今日工作" else "记录今日工作", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ closeEditor() }) { Icon(Icons.Default.Close, "关闭") } }
                    if (imeVisible) {
                        Text(todayDate().shortLabelWithWeekday(), Modifier.padding(top = 2.dp, bottom = 8.dp), color = Ink.copy(alpha = .6f), fontSize = 13.sp)
                    } else {
                        Text("· 日志日期", fontWeight = FontWeight.Bold)
                        Text(todayDate().shortLabelWithWeekday(), Modifier.fillMaxWidth().padding(vertical = 12.dp), color = Ink.copy(alpha = .7f))
                        Text("· 关联任务（可选）", fontWeight = FontWeight.Bold)
                        var taskMenu by remember { mutableStateOf(false) }
                        Box {
                            Button({ taskMenu = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = CreamDeep, contentColor = Ink), shape = RoundedCornerShape(12.dp)) { Text(draftTask, Modifier.weight(1f), textAlign = TextAlign.Start); Icon(Icons.Default.ArrowDropDown, null) }
                            DropdownMenu(taskMenu, { taskMenu = false }, containerColor = CreamDeep) {
                                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                                    (listOf("不关联任务") + tasks.map { it.title }).forEach { option -> DropdownMenuItem({ Text(option) }, { draftTask = option; savedDraft = false; taskMenu = false }) }
                                }
                            }
                        }
                        Text("· 今日完成的工作", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    }
                    OutlinedTextField(draftLog, { draftLog = it; savedDraft = false }, Modifier.fillMaxWidth().weight(1f).heightIn(min = 140.dp), placeholder = { Text("记录今日完成的工作") }, colors = mutedColors)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton({ savedDraft = true; writing = false }, Modifier.weight(1f), enabled = draftLog.isNotBlank(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Text("保存为草稿") }
                        Button({ confirmPublish = true }, Modifier.weight(1f), enabled = draftLog.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("发布") }
                    }
                }
            }
        }
        if (confirmPublish) AlertDialog(onDismissRequest = { confirmPublish = false }, modifier = Modifier.border(2.dp, CreamDeep, RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), containerColor = Color.White, titleContentColor = Ink, textContentColor = Ink.copy(alpha = .75f), title = { Text("确认发布？", fontWeight = FontWeight.Bold) }, text = { Text(if (todayReported) "发布后将更新今日这一篇日志，不会新增第二条。" else "发布后将显示在日志列表中。") }, confirmButton = { Button({ saveDailyLog(logs, draftLog, todayDate(), nowTimeLabel()); confirmPublish = false; savedDraft = false; draftLog = ""; draftTask = "不关联任务"; writing = false }, colors = ButtonDefaults.buttonColors(containerColor = CreamDeep, contentColor = Ink), shape = RoundedCornerShape(14.dp)) { Text("确认发布", fontWeight = FontWeight.Bold) } }, dismissButton = { TextButton({ confirmPublish = false }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) { Text("取消") } })
        if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false }, modifier = Modifier.border(2.dp, CreamDeep, RoundedCornerShape(28.dp)), shape = RoundedCornerShape(28.dp), containerColor = Color.White, titleContentColor = Ink, textContentColor = Ink.copy(alpha = .75f), title = { Text("放弃未保存内容？", fontWeight = FontWeight.Bold) }, text = { Text("退出后当前填写内容会被清空。") }, confirmButton = { TextButton({ confirmExit = false; draftLog = ""; draftTask = "不关联任务"; savedDraft = false; writing = false }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) { Text("放弃并退出") } }, dismissButton = { TextButton({ confirmExit = false }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) { Text("继续编辑") } })
    }
}

@Composable private fun BoxScope.NotificationDrawer(onClose: () -> Unit) {
    Card(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.75f).fillMaxHeight(), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = WarmCard), elevation = CardDefaults.cardElevation(14.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("消息", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClose) { Icon(Icons.Default.Close, "关闭") } }; Text("今天收到的更新", color = Ink.copy(alpha = .6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp)); DetailCard("新任务", "完成登录模块 · 已派发", Peach); Spacer(Modifier.height(10.dp)); DetailCard("审核结果", "阶段汇报已通过", Mint); Spacer(Modifier.height(10.dp)); DetailCard("下属日报", "暂无新的日报更新", Color.White) }
    }
}
@Composable private fun AiMapScreen() { Column(Modifier.fillMaxSize().padding(16.dp)) { PageHeader("AI地图"); Spacer(Modifier.height(48.dp)); Icon(Icons.Default.Info, null, Modifier.size(70.dp).align(Alignment.CenterHorizontally), tint = Coral); Text("AI 地图", Modifier.fillMaxWidth().padding(top = 18.dp), textAlign = TextAlign.Center, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("功能规划中", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, color = Ink.copy(alpha = .6f)) } }
@Composable private fun ProfileScreen(notificationsEnabled: Boolean, onOpenSettings: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageHeader("我的", "个人资料")
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = CreamDeep), shape = RoundedCornerShape(20.dp)) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountCircle, null, Modifier.size(64.dp), tint = WarmOrange)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(MockData.userName, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(MockData.position, color = Ink.copy(alpha = .7f))
                        Text("模拟账号", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = WarmCard), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("组织信息", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    ProfileInfoRow("所属企业", "Pandora 演示企业")
                    DashedDivider(Ink.copy(alpha = .18f))
                    ProfileInfoRow("所属部门", MockData.department)
                    ProfileInfoRow("当前角色", "部门老总（模拟）")
                    DashedDivider(Ink.copy(alpha = .18f))
                    Text("管理团队", fontWeight = FontWeight.Bold)
                    MockData.teamLeaders.forEach { Text(it, color = Ink.copy(alpha = .75f)) }
                }
            }
            SettingsSectionLabel("基础设置")
            SettingsCard {
                SettingsRow(icon = Icons.Default.Tune, title = "全部设置", onClick = onOpenSettings) {
                    Icon(Icons.Default.ChevronRight, null, Modifier.size(20.dp), tint = Ink.copy(alpha = .4f))
                }
                DashedDivider(Ink.copy(alpha = .18f))
                SettingsRow(icon = Icons.Default.Notifications, title = "消息通知", onClick = onOpenSettings) {
                    Text(if (notificationsEnabled) "已开启" else "已关闭", color = Ink.copy(alpha = .55f), fontSize = 13.sp)
                }
            }
            Text("关于 Pandora", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Android 低保真原型 · 0.1.0\n资料与业务内容为模拟数据，用于需求讨论。", color = Ink.copy(alpha = .6f), lineHeight = 22.sp)
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
@Composable private fun DetailScreen(title: String, tasks: List<WorkTask>, onBack: () -> Unit) {
    val isLog = title.startsWith("日志 · ")
    val logContent = if (isLog) title.substringAfter("§", "") else null
    val task = if (!isLog) tasks.firstOrNull { it.id == title || it.title == title } else null
    val displayTitle = task?.title ?: if (isLog) title.substringBefore("§") else title
    androidx.activity.compose.BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        PageHeader(displayTitle, if (isLog) null else "演示详情", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            if (!isLog) { Text("${displayTitle}详情", fontSize = 24.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)) }
            if (task != null) {
                Text("任务说明", fontWeight = FontWeight.Bold)
                Text(task.note.ifBlank { "暂无备注" }, lineHeight = 22.sp)
                Spacer(Modifier.height(10.dp))
                DetailCard("时间节点", "${task.start.year}/${task.start.month}/${task.start.day}—${task.end.year}/${task.end.month}/${task.end.day}\n${taskTimeLabel(task)}", Color(0xFFE2E2E2))
                Spacer(Modifier.height(10.dp))
                DetailCard("优先级与状态", "${task.priority} · ${task.status}", Color(0xFFE2E2E2))
                Spacer(Modifier.height(10.dp))
                Text("接收人", fontWeight = FontWeight.Bold)
                Text(task.assignee, lineHeight = 22.sp)
            } else if (isLog) {
                Text(logContent ?: "", color = Ink.copy(alpha = .75f), fontSize = 16.sp, lineHeight = 26.sp)
            } else {
                Text("这里展示低保真原型中的可读内容和返回路径。\n\n真实任务、日志、权限和 AI 服务将在后续需求确认后接入。", color = Ink.copy(alpha = .75f), lineHeight = 24.sp)
            }
            if (!isLog) {
                Spacer(Modifier.height(22.dp))
                DetailCard("当前状态", "模拟数据 · 仅用于线下讨论", CreamDeep)
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
    onClose: () -> Unit
) {
    androidx.activity.compose.BackHandler(onBack = onClose)
    var showFontDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(toast) {
        if (toast != null) {
            kotlinx.coroutines.delay(1800)
            toast = null
        }
    }

    Column(Modifier.fillMaxSize().background(WarmDashboard)) {
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
                }
                Spacer(Modifier.height(18.dp))
                SettingsSectionLabel("数据")
                SettingsCard {
                    SettingsRow(
                        icon = Icons.Default.CleaningServices,
                        title = "清除缓存",
                        onClick = { showClearCacheDialog = true }
                    ) {
                        Text("12.3 MB", color = Ink.copy(alpha = 0.5f), fontSize = 13.sp)
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
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("清除缓存？") },
            text = { Text("将清除 12.3 MB 临时文件，不会影响你的数据。") },
            confirmButton = { TextButton({ showClearCacheDialog = false; toast = "已清除缓存" }) { Text("清除", color = WarmOrange, fontWeight = FontWeight.Bold) } },
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
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("退出登录？") },
            text = { Text("退出后需要重新登录才能使用。") },
            confirmButton = { TextButton({ showLogoutDialog = false; onClose() }) { Text("退出", color = CoralDark, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton({ showLogoutDialog = false }) { Text("取消", color = Ink.copy(alpha = 0.7f)) } }
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
