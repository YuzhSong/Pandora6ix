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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
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
        val nav = listOf(
            NavItem(Route.HOME, "首页", Icons.Default.GridView),
            NavItem(Route.VIEW, "视图", Icons.Default.CalendarMonth),
            NavItem(Route.LOGS, "日志", Icons.Default.NoteAlt),
            NavItem(Route.AI, "AI地图", Icons.Default.Psychology),
            NavItem(Route.ME, "我的", Icons.Default.PersonOutline)
        )
        Scaffold(containerColor = Cream, bottomBar = { NavigationBar(containerColor = CreamDeep) { nav.forEach { item -> NavigationBarItem(selected = current == item.route, onClick = { route = item.route.name; detail = null }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = WarmOrange, selectedTextColor = WarmOrange, indicatorColor = CreamDeep)) } } }) { padding ->
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
@Composable private fun PageHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = { Column { Text(title, fontWeight = FontWeight.Bold); subtitle?.let { Text(it, fontSize = 12.sp, color = Ink.copy(alpha = .6f)) } } }, navigationIcon = { if (onBack != null) IconButton(onBack) { Icon(Icons.Default.ArrowBack, "返回") } }, actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
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
      Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(MockData.demoToday.shortLabelWithWeekday(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { }) { Surface(shape = RoundedCornerShape(50), color = CreamDeep) { Icon(Icons.Default.Email, "邮箱", Modifier.padding(9.dp), tint = Ink) } }
        }
        Text("工作总览", fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 10.dp))
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) { HomePanel(panels[0].first, panels[0].second, Modifier.weight(1f).fillMaxHeight()) { expandedPanel = panels[0].first }; HomePanel(panels[1].first, panels[1].second, Modifier.weight(1f).fillMaxHeight()) { expandedPanel = panels[1].first } }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) { HomePanel(panels[2].first, panels[2].second, Modifier.weight(1f).fillMaxHeight()) { expandedPanel = panels[2].first }; HomePanel(panels[3].first, panels[3].second, Modifier.weight(1f).fillMaxHeight()) { expandedPanel = panels[3].first } }
        }
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
    Card(modifier.clickable(onClick = onClick).border(2.dp, WarmOrange, RoundedCornerShape(22.dp)), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = WarmCard)) {
        Column(Modifier.padding(14.dp)) { Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis); HorizontalDivider(Modifier.padding(top = 7.dp, bottom = 4.dp), color = WarmOrange.copy(alpha = .65f), thickness = 1.dp); Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) { items.take(10).forEachIndexed { i, text -> Text("${i + 1}. $text", fontSize = 15.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)); if (i < items.take(10).lastIndex) Text("· · · · · · · · ·", color = WarmOrange.copy(alpha = .65f), fontSize = 10.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) } }; Text("展开查看 →", color = Color(0xFFB34E4A), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun ViewScreen(mode: ViewMode, onModeChange: (ViewMode) -> Unit, cursor: DemoDate, onCursorChange: (DemoDate) -> Unit, onOpen: (String) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PageHeader("工作视图", "任务与日志共用演示数据", actions = {
                Box(Modifier.padding(end = 12.dp)) { Card(Modifier.clickable { expanded = true }, shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Text(mode.label, fontWeight = FontWeight.Bold, fontSize = 13.sp); Icon(Icons.Default.ArrowDropDown, null, Modifier.size(17.dp)) } }; DropdownMenu(expanded, { expanded = false }) { ViewMode.values().forEach { option -> DropdownMenuItem({ Text(option.label) }, { onModeChange(option); expanded = false }) } } }
            })
            when (mode) { ViewMode.DAY -> DayView(cursor, onCursorChange, onOpen); ViewMode.WEEK -> WeekView(cursor, onCursorChange) { selected -> onModeChange(ViewMode.DAY); onCursorChange(selected) }; ViewMode.MONTH -> MonthView(cursor, onCursorChange) { selected -> onModeChange(ViewMode.WEEK); onCursorChange(selected) } }
        }
        if (cursor != MockData.demoToday) Button(onClick = { onCursorChange(MockData.demoToday) }, modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 0.dp).fillMaxWidth(.25f).height(38.dp), shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink), contentPadding = PaddingValues(horizontal = 8.dp)) { Text("回到今天", color = Color.White, fontSize = 12.sp, maxLines = 1, softWrap = false) }
    }
}

@Composable private fun WeekView(cursor: DemoDate, onCursorChange: (DemoDate) -> Unit, onSelectDate: (DemoDate) -> Unit) {
    val start = mondayOfWeek(cursor); val dates = (0..6).map { start.plusDays(it) }; val names = listOf("一", "二", "三", "四", "五", "六", "日")
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("${start.shortLabel()}—${dates.last().shortLabel()}", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.weight(1f)); IconButton({ onCursorChange(cursor.plusDays(-7)) }) { Icon(Icons.Default.ArrowBack, "上周") }; IconButton({ onCursorChange(cursor.plusDays(7)) }) { Icon(Icons.Default.ArrowForward, "下周") } }; Row(Modifier.fillMaxWidth().padding(vertical = 12.dp)) { dates.forEachIndexed { i, date -> Text("${date.day}\n周${names[i]}", Modifier.weight(1f).clickable { onSelectDate(date) }, fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Center) } }; Box(Modifier.fillMaxWidth().height(390.dp)) { Box(Modifier.matchParentSize().padding(top = 30.dp).drawBehind { for (i in 1..6) { val x = size.width * i / 7f; drawLine(color = Coral.copy(alpha = .22f), start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()))) } }) { Column { MockData.companyTasks.forEach { TaskBar(it, dates, onSelectDate) } } } } }
}

@Composable private fun TaskBar(task: WorkTask, dates: List<DemoDate>, onSelectDate: (DemoDate) -> Unit) {
    val colors = listOf(Peach, Sky, Mint, Lilac)
    val first = dates.first(); val last = dates.last()
    if (task.end < first || task.start > last) return
    val startIndex = maxOf(0, daysBetween(first, task.start)); val endIndex = minOf(6, daysBetween(first, task.end)); val span = endIndex - startIndex + 1
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (startIndex > 0) Spacer(Modifier.weight(startIndex.toFloat()))
        Box(Modifier.weight(span.toFloat()).height(44.dp).padding(1.dp).clip(RoundedCornerShape(8.dp)).background(colors[task.colorIndex]).clickable { onSelectDate(task.start) }, contentAlignment = Alignment.CenterStart) { Text(task.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(horizontal = 8.dp)) }
        if (endIndex < 6) Spacer(Modifier.weight((6 - endIndex).toFloat()))
    }
}

@Composable private fun MonthView(cursor: DemoDate, onCursorChange: (DemoDate) -> Unit, onSelectDate: (DemoDate) -> Unit) {
    val first = DemoDate(cursor.year, cursor.month, 1); val offset = first.ordinal() - mondayOfWeek(first).ordinal(); val last = first.plusMonths(1).plusDays(-1); val cellCount = ((offset + last.day + 6) / 7) * 7; val cells = (0 until cellCount).map { first.plusDays(it - offset) }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(cursor.monthLabel(), fontSize = 20.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); IconButton({ onCursorChange(cursor.plusMonths(-1)) }) { Icon(Icons.Default.ArrowBack, "上月") }; IconButton({ onCursorChange(cursor.plusMonths(1)) }) { Icon(Icons.Default.ArrowForward, "下月") } }; Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp) } }; Column(Modifier.fillMaxWidth().weight(1f)) { cells.chunked(7).take(5).forEach { week -> MonthWeekRow(week, cursor.month, onSelectDate, Modifier.weight(1f)) } } }
}

@Composable private fun MonthWeekRow(week: List<DemoDate>, month: Int, onSelectDate: (DemoDate) -> Unit, modifier: Modifier = Modifier) {
    val visibleTasks = MockData.companyTasks.filter { it.end >= week.first() && it.start <= week.last() }
    Column(modifier.fillMaxWidth().drawBehind { val guide = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx())); for (i in 1..6) { val x = size.width * i / 7f; drawLine(color = Coral.copy(alpha = .2f), start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1.dp.toPx(), pathEffect = guide) }; drawLine(color = Coral.copy(alpha = .2f), start = androidx.compose.ui.geometry.Offset(0f, size.height - 1.dp.toPx()), end = androidx.compose.ui.geometry.Offset(size.width, size.height - 1.dp.toPx()), strokeWidth = 1.dp.toPx(), pathEffect = guide) }) {
        Row(Modifier.fillMaxWidth().height(34.dp)) { week.forEach { date -> Column(Modifier.weight(1f).fillMaxHeight().clickable { onSelectDate(date) }.padding(4.dp)) { Text("${date.day}", fontSize = 14.sp, color = if (date.month == month) Ink else Ink.copy(alpha = .3f)) } } }
        Column(Modifier.fillMaxWidth().weight(1f)) {
            visibleTasks.take(2).forEach { task -> MonthTaskBar(task, week, onSelectDate, Modifier.weight(1f)) }
            repeat(maxOf(0, 2 - visibleTasks.size)) { Spacer(Modifier.weight(1f)) }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { if (visibleTasks.size > 2) Text("+${visibleTasks.size - 2}条计划", fontSize = 13.sp, color = Ink.copy(alpha = .65f)) }
        }
    }
}

@Composable private fun MonthTaskBar(task: WorkTask, dates: List<DemoDate>, onSelectDate: (DemoDate) -> Unit, modifier: Modifier = Modifier) {
    val startIndex = maxOf(0, daysBetween(dates.first(), task.start)); val endIndex = minOf(6, daysBetween(dates.first(), task.end)); val span = endIndex - startIndex + 1
    if (span <= 0) return
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { if (startIndex > 0) Spacer(Modifier.weight(startIndex.toFloat())); Box(Modifier.weight(span.toFloat()).fillMaxHeight().padding(horizontal = 2.dp, vertical = 3.dp).clip(RoundedCornerShape(6.dp)).background(Peach.copy(alpha = .75f)).clickable { onSelectDate(if (task.start < dates.first()) dates.first() else task.start) }, contentAlignment = Alignment.CenterStart) { Text(task.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 5.dp)) }; if (endIndex < 6) Spacer(Modifier.weight((6 - endIndex).toFloat())) }
}

@Composable private fun DayView(cursor: DemoDate, onCursorChange: (DemoDate) -> Unit, onOpen: (String) -> Unit) {
    val tasks = MockData.companyTasks.filter { cursor >= it.start && cursor <= it.end }; val logs = MockData.logs.filter { it.date == cursor }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Row(verticalAlignment = Alignment.CenterVertically) { Text(cursor.shortLabelWithWeekday(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ onCursorChange(cursor.plusDays(-1)) }) { Icon(Icons.Default.ArrowBack, "前一天") }; IconButton({ onCursorChange(cursor.plusDays(1)) }) { Icon(Icons.Default.ArrowForward, "后一天") } }; Text("${tasks.size} 项任务 · ${logs.size} 条日志", color = Ink.copy(alpha = .6f)) }; item { Text("当日任务", fontWeight = FontWeight.Bold) }; items(tasks) { DetailCard(it.title, "${it.start.shortLabel()}—${it.end.shortLabel()} · ${it.status}", Peach) { onOpen(it.title) } }; item { Text("工作日志", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }; items(logs) { DetailCard(it.time, it.content, Lilac) { onOpen("日志 · ${it.time}") } }; if (tasks.isEmpty() && logs.isEmpty()) item { Text("这一天还没有演示记录", color = Ink.copy(alpha = .6f)) } }
}

@Composable private fun LogsScreen(onOpen: (String) -> Unit) {
    var section by rememberSaveable { mutableStateOf("工作日志") }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageHeader("日志", "工作日志与任务管理")
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("工作日志", "任务管理").forEach { option ->
                val selected = section == option
                Button(onClick = { section = option }, colors = ButtonDefaults.buttonColors(containerColor = if (selected) WarmOrange else Color.White, contentColor = Ink), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) { Text(option) }
            }
        }
        if (section == "工作日志") {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item { Button(onClick = { onOpen("新建工作日志") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text("记录今天的工作") } }
                item { Text("最近记录", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp)) }
                items(MockData.logs) { log -> DetailCard(log.time, log.content, Lilac) { onOpen("日志 · ${log.time}") } }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                item { Text("我的任务", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                items(MockData.companyTasks) { task -> DetailCard(task.title, "${task.start.shortLabel()}—${task.end.shortLabel()} · ${task.status}", Peach) { onOpen(task.title) } }
                item { Text("任务派发与审核入口", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.padding(top = 8.dp)) }
                item { DetailCard("任务派发", "查看已派发任务并创建演示任务", Color.White) { onOpen("任务派发") } }
                item { DetailCard("待我审核", "查看需要确认的演示内容", Color.White) { onOpen("待我审核") } }
            }
        }
    }
}
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
