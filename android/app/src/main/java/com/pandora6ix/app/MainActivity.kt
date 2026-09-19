package com.pandora6ix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.mock.*
import com.pandora6ix.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PandoraApp() } }
}

private enum class Route { HOME, VIEW, LOGS, AI, ME }
private enum class ViewMode(val label: String) { DAY("日视图"), WEEK("周视图"), MONTH("月视图") }
private data class NavItem(val route: Route, val label: String, val icon: ImageVector)

@Composable
fun PandoraApp() {
    PandoraTheme {
        var route by rememberSaveable { mutableStateOf(Route.HOME.name) }
        var modeName by rememberSaveable { mutableStateOf(ViewMode.WEEK.name) }
        var dateKey by rememberSaveable { mutableStateOf("2026-09-19") }
        var detail by rememberSaveable { mutableStateOf<String?>(null) }
        val date = dateKey.split("-").map(String::toInt).let { DemoDate(it[0], it[1], it[2]) }
        val current = Route.valueOf(route)
        val nav = listOf(NavItem(Route.HOME, "首页", Icons.Default.Home), NavItem(Route.VIEW, "视图", Icons.Default.Menu), NavItem(Route.LOGS, "日志", Icons.Default.List), NavItem(Route.AI, "AI地图", Icons.Default.Info), NavItem(Route.ME, "我的", Icons.Default.AccountCircle))
        Scaffold(containerColor = Cream, bottomBar = { NavigationBar(containerColor = CreamDeep) { nav.forEach { item -> NavigationBarItem(selected = current == item.route, onClick = { route = item.route.name; detail = null }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 11.sp) }) } } }) { padding ->
            Surface(Modifier.padding(padding).fillMaxSize(), color = Cream) {
                if (detail != null) DetailScreen(detail!!, onBack = { detail = null }) else AnimatedContent(current, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { target ->
                    when (target) {
                        Route.HOME -> HomeScreen { detail = it }
                        Route.VIEW -> ViewScreen(ViewMode.valueOf(modeName), { modeName = it.name }, date, { dateKey = "${it.year}-${it.month}-${it.day}" }) { detail = it }
                        Route.LOGS -> LogsScreen { detail = it }
                        Route.AI -> AiMapScreen()
                        Route.ME -> ProfileScreen()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun PageHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null) {
    TopAppBar(title = { Column { Text(title, fontWeight = FontWeight.Bold); subtitle?.let { Text(it, fontSize = 12.sp, color = Ink.copy(alpha = .6f)) } } }, navigationIcon = { if (onBack != null) IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
}

@Composable private fun HomeScreen(onOpen: (String) -> Unit) {
    var expandedPanel by rememberSaveable { mutableStateOf<String?>(null) }
    val panels = listOf(
        "公司十大重要事项" to MockData.companyHighlights,
        "公司十大派发任务" to MockData.companyTasks.map { it.title },
        "个人十大重要事项" to MockData.personalImportant,
        "个人日志" to MockData.logs.map { it.content }
    )
    Box(Modifier.fillMaxSize().background(WarmDashboard)) {
      Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        PageHeader("Pandora", "今天 · ${MockData.demoToday.shortLabel()}")
        Text("工作总览", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { HomePanel(panels[0].first, panels[0].second, Modifier.weight(1f)) { expandedPanel = panels[0].first }; HomePanel(panels[1].first, panels[1].second, Modifier.weight(1f)) { expandedPanel = panels[1].first } }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { HomePanel(panels[2].first, panels[2].second, Modifier.weight(1f)) { expandedPanel = panels[2].first }; HomePanel(panels[3].first, panels[3].second, Modifier.weight(1f)) { expandedPanel = panels[3].first } }
        Text("点击板块可展开查看完整十条内容", color = Ink.copy(alpha = .65f), fontSize = 13.sp, modifier = Modifier.padding(vertical = 16.dp))
      }
      val selected = panels.firstOrNull { it.first == expandedPanel }
      if (selected != null) {
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).clickable { expandedPanel = null })
          val scale by animateFloatAsState(1f, label = "panel-scale")
          Card(Modifier.align(Alignment.Center).fillMaxWidth(.9f).fillMaxHeight(.78f).scale(scale).clickable { }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WarmCard), elevation = CardDefaults.cardElevation(10.dp)) {
              Column(Modifier.fillMaxSize().padding(18.dp)) {
                  Row(verticalAlignment = Alignment.CenterVertically) { Text(selected.first, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ expandedPanel = null }) { Icon(Icons.Default.Close, "关闭") } }
                  Text("共 ${selected.second.take(10).size} 条 · 演示数据", color = Ink.copy(alpha = .6f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
                  Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) { selected.second.take(10).forEachIndexed { i, item -> Text("${i + 1}. $item", fontSize = 15.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) } }
              }
          }
      }
    }
}

@Composable private fun HomePanel(title: String, items: List<String>, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier.height(310.dp).clickable(onClick = onClick).border(2.dp, WarmOrange, RoundedCornerShape(22.dp)), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = WarmCard)) {
        Column(Modifier.padding(13.dp)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); Spacer(Modifier.height(7.dp)); Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { items.take(10).forEachIndexed { i, text -> Text("${i + 1}. $text", fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(vertical = 3.dp)) } }; Text("展开查看 →", color = Color(0xFFB34E4A), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ViewScreen(mode: ViewMode, onModeChange: (ViewMode) -> Unit, cursor: DemoDate, onCursorChange: (DemoDate) -> Unit, onOpen: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PageHeader("工作视图", "任务与日志共用演示数据")
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box { Card(Modifier.clickable { expanded = true }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) { Text(mode.label, fontWeight = FontWeight.Bold); Icon(Icons.Default.ArrowDropDown, null, Modifier.size(18.dp)) } }; DropdownMenu(expanded, { expanded = false }) { ViewMode.values().forEach { option -> DropdownMenuItem({ Text(option.label) }, { onModeChange(option); expanded = false }) } } }
            Spacer(Modifier.weight(1f)); IconButton({ onCursorChange(cursor.plusDays(-1)) }) { Icon(Icons.Default.ArrowBack, "上一个") }; IconButton({ onCursorChange(cursor.plusDays(1)) }) { Icon(Icons.Default.ArrowForward, "下一个") }
        }
        when (mode) { ViewMode.DAY -> DayView(cursor, onOpen); ViewMode.WEEK -> WeekView(cursor, onOpen); ViewMode.MONTH -> MonthView(cursor, onCursorChange, onOpen) }
    }
}

@Composable private fun WeekView(cursor: DemoDate, onOpen: (String) -> Unit) {
    val start = mondayOfWeek(cursor); val dates = (0..6).map { start.plusDays(it) }; val names = listOf("一", "二", "三", "四", "五", "六", "日")
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) { Text("${start.monthLabel()} · ${start.shortLabel()}—${dates.last().shortLabel()}", fontWeight = FontWeight.Bold, fontSize = 18.sp); Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) { dates.forEachIndexed { i, date -> Text("${date.day}\n周${names[i]}", Modifier.weight(1f), fontSize = 12.sp, textAlign = TextAlign.Center) } }; Text("任务横条", color = Ink.copy(alpha = .6f), fontSize = 13.sp); MockData.companyTasks.forEach { TaskBar(it, dates, onOpen) } }
}

@Composable private fun TaskBar(task: WorkTask, dates: List<DemoDate>, onOpen: (String) -> Unit) {
    val colors = listOf(Peach, Sky, Mint, Lilac)
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { dates.forEachIndexed { i, date -> val active = date >= task.start && date <= task.end; Box(Modifier.weight(1f).height(34.dp).padding(1.dp).clip(RoundedCornerShape(6.dp)).background(if (active) colors[task.colorIndex] else Color.White.copy(alpha = .35f)).clickable(enabled = active) { onOpen(task.title) }, contentAlignment = Alignment.Center) { if (active && (date == task.start || i == 0)) Text(task.title, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(3.dp)) } } }
}

@Composable private fun MonthView(cursor: DemoDate, onCursorChange: (DemoDate) -> Unit, onOpen: (String) -> Unit) {
    val first = DemoDate(cursor.year, cursor.month, 1); val offset = first.ordinal() - mondayOfWeek(first).ordinal(); val cells = (0 until 42).map { first.plusDays(it - offset) }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(12.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(cursor.monthLabel(), fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); IconButton({ onCursorChange(cursor.plusDays(-28)) }) { Icon(Icons.Default.ArrowBack, "上月") }; IconButton({ onCursorChange(cursor.plusDays(35)) }) { Icon(Icons.Default.ArrowForward, "下月") } }; Row(Modifier.fillMaxWidth()) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp) } }; cells.chunked(7).forEach { week -> Row(Modifier.fillMaxWidth()) { week.forEach { MonthCell(it, cursor.month, onOpen) } } } }
}

@Composable private fun RowScope.MonthCell(date: DemoDate, month: Int, onOpen: (String) -> Unit) {
    val tasks = MockData.companyTasks.filter { date >= it.start && date <= it.end }
    Column(Modifier.weight(1f).height(82.dp).border(.5.dp, Coral.copy(alpha = .2f)).padding(3.dp).clickable { onOpen("${date.shortLabel()}的工作") }) { Text("${date.day}", fontSize = 12.sp, color = if (date.month == month) Ink else Ink.copy(alpha = .3f)); tasks.take(2).forEach { task -> Text(task.title, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(Peach.copy(alpha = .75f)).padding(horizontal = 2.dp).clickable { onOpen(task.title) }) } }
}

@Composable private fun DayView(cursor: DemoDate, onOpen: (String) -> Unit) {
    val tasks = MockData.companyTasks.filter { cursor >= it.start && cursor <= it.end }; val logs = MockData.logs.filter { it.date == cursor }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text(cursor.shortLabel(), fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("${tasks.size} 项任务 · ${logs.size} 条日志", color = Ink.copy(alpha = .6f)) }; item { Text("当日任务", fontWeight = FontWeight.Bold) }; items(tasks) { DetailCard(it.title, "${it.start.shortLabel()}—${it.end.shortLabel()} · ${it.status}", Peach) { onOpen(it.title) } }; item { Text("工作日志", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }; items(logs) { DetailCard(it.time, it.content, Lilac) { onOpen("日志 · ${it.time}") } }; if (tasks.isEmpty() && logs.isEmpty()) item { Text("这一天还没有演示记录", color = Ink.copy(alpha = .6f)) } }
}

@Composable private fun LogsScreen(onOpen: (String) -> Unit) { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { PageHeader("日志", "工作日志与任务管理"); listOf("工作日志", "我的任务", "任务派发", "待我审核").forEach { DetailCard(it, "进入${it}演示入口", Color.White) { onOpen(it) } }; Text("本轮仅提供可点击的原型入口，数据不会持久化。", fontSize = 12.sp, color = Ink.copy(alpha = .6f), modifier = Modifier.padding(top = 14.dp)) } }
@Composable private fun AiMapScreen() { Column(Modifier.fillMaxSize().padding(16.dp)) { PageHeader("AI地图"); Spacer(Modifier.height(48.dp)); Icon(Icons.Default.Info, null, Modifier.size(70.dp).align(Alignment.CenterHorizontally), tint = Coral); Text("AI 地图", Modifier.fillMaxWidth().padding(top = 18.dp), textAlign = TextAlign.Center, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("功能规划中", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, color = Ink.copy(alpha = .6f)) } }
@Composable private fun ProfileScreen() { Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) { PageHeader("我的", "个人资料"); Card(Modifier.fillMaxWidth().padding(top = 20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(22.dp)) { Icon(Icons.Default.AccountCircle, null, Modifier.size(64.dp), tint = Coral); Text(MockData.userName, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text(MockData.department, color = Ink.copy(alpha = .65f)); Text(MockData.position, color = Ink.copy(alpha = .65f)); Spacer(Modifier.height(18.dp)); Text("企业：Pandora 演示企业"); Text("当前身份：普通员工（模拟）") } } } }
@Composable private fun DetailCard(title: String, body: String, color: Color, onClick: () -> Unit = {}) { Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = color.copy(alpha = .78f)), shape = RoundedCornerShape(16.dp)) { Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, fontSize = 13.sp, color = Ink.copy(alpha = .75f), maxLines = 2, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun DetailScreen(title: String, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        PageHeader(title, "演示详情", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("${title}详情", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            Text("这里展示低保真原型中的可读内容和返回路径。\n\n真实任务、日志、权限和 AI 服务将在后续需求确认后接入。", color = Ink.copy(alpha = .75f), lineHeight = 24.sp)
            Spacer(Modifier.height(22.dp))
            DetailCard("当前状态", "模拟数据 · 仅用于线下讨论", CreamDeep)
        }
    }
}
