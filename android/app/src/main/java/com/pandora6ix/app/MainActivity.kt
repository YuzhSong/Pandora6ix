package com.pandora6ix.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
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
import com.pandora6ix.app.ui.calendar.*
import com.pandora6ix.app.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { PandoraApp() } }
}

private enum class Route { HOME, VIEW, LOGS, AI, ME }
private data class NavItem(val route: Route, val label: String, val icon: ImageVector)

@Composable
fun PandoraApp() {
    PandoraTheme {
        var route by rememberSaveable { mutableStateOf(Route.HOME.name) }
        var modeName by rememberSaveable { mutableStateOf(CalendarMode.WEEK.name) }
        var dateKey by rememberSaveable { mutableStateOf("2026-09-19") }
        var detail by rememberSaveable { mutableStateOf<String?>(null) }
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
        Scaffold(containerColor = Cream, bottomBar = { NavigationBar(containerColor = CreamDeep) { nav.forEach { item -> NavigationBarItem(selected = current == item.route, onClick = { route = item.route.name; detail = null }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 11.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = WarmOrange, selectedTextColor = WarmOrange, indicatorColor = CreamDeep)) } } }) { padding ->
            Surface(Modifier.padding(padding).fillMaxSize(), color = Cream) {
                if (detail != null) DetailScreen(detail!!, tasks, onBack = { detail = null }) else AnimatedContent(current, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "page") { target ->
                    when (target) {
                        Route.HOME -> HomeScreen(tasks) { detail = it }
                        Route.VIEW -> CalendarScreen(CalendarMode.valueOf(modeName), date, tasks, MockData.role.canCreateTasks, { modeName = it.name }, { dateKey = "${it.year}-${it.month}-${it.day}" }, { if (MockData.role.canCreateTasks) tasks.add(it) }, { detail = it.id })
                        Route.LOGS -> LogsScreen(tasks) { detail = it }
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

@Composable private fun HomeScreen(tasks: List<WorkTask>, onOpen: (String) -> Unit) {
    var expandedPanel by rememberSaveable { mutableStateOf<String?>(null) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    val panels = listOf(
        "公司十大重要事项" to MockData.companyHighlights,
        "公司十大派发任务" to tasks.filter { it.status != "草稿（演示）" }.take(10).map { it.title },
        "个人十大重要事项" to MockData.personalImportant,
        "个人日志" to MockData.logs.map { it.content }
    )
    Box(Modifier.fillMaxSize().background(WarmDashboard)) {
      Column(Modifier.fillMaxSize().padding(horizontal = 10.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(MockData.demoToday.shortLabelWithWeekday(), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = { notifications = true }) { Surface(shape = RoundedCornerShape(50), color = CreamDeep) { Icon(Icons.Default.Email, "邮箱", Modifier.padding(9.dp), tint = Ink) } }
        }
        Column(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) { HomePanel(panels[0].first, panels[0].second, Modifier.weight(1f).fillMaxHeight()) { expandedPanel = panels[0].first }; HomePanel(panels[1].first, panels[1].second, Modifier.weight(1f).fillMaxHeight()) { expandedPanel = panels[1].first } }
            Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) { HomePanel(panels[2].first, panels[2].second, Modifier.weight(1f).fillMaxHeight(), trailing = { Box(Modifier.size(24.dp).clickable { }) { Icon(Icons.Default.Edit, "编辑", Modifier.size(17.dp).align(Alignment.Center), tint = Ink) } }) { expandedPanel = panels[2].first }; HomePanel(panels[3].first, panels[3].second, Modifier.weight(1f).fillMaxHeight()) { expandedPanel = panels[3].first } }
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
      if (notifications) {
          Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).clickable { notifications = false; expandedPanel = null })
          NotificationDrawer(onClose = { notifications = false })
      }
    }
}

@Composable private fun HomePanel(title: String, items: List<String>, modifier: Modifier, trailing: @Composable RowScope.() -> Unit = {}, onClick: () -> Unit) {
    Card(modifier.clickable(onClick = onClick).border(2.dp, WarmOrange, RoundedCornerShape(22.dp)), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = WarmCard)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)); trailing()
            }
            HorizontalDivider(Modifier.padding(top = 7.dp, bottom = 4.dp), color = WarmOrange.copy(alpha = .65f))
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                items.take(10).forEachIndexed { i, text ->
                    Text("${i + 1}. $text", fontSize = 15.sp, lineHeight = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().padding(vertical = 1.33.dp))
                    if (i < items.take(10).lastIndex) Spacer(Modifier.fillMaxWidth().height(4.dp).drawBehind {
                        drawLine(WarmOrange.copy(alpha = .65f), androidx.compose.ui.geometry.Offset(size.width * .25f, size.height / 2), androidx.compose.ui.geometry.Offset(size.width * .75f, size.height / 2), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 4.dp.toPx())))
                    })
                }
            }
            Text("展开查看 →", color = Color(0xFFB34E4A), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}


@Composable private fun LogsScreen(tasks: List<WorkTask>, onOpen: (String) -> Unit) {
    var expandedLogs by rememberSaveable { mutableStateOf(false) }
    var filterOpen by rememberSaveable { mutableStateOf(false) }
    var historyFilter by rememberSaveable { mutableStateOf("全部日期") }
    var writing by rememberSaveable { mutableStateOf(false) }
    var notifications by rememberSaveable { mutableStateOf(false) }
    var draftLog by rememberSaveable { mutableStateOf("") }
    var draftTask by rememberSaveable { mutableStateOf("不关联任务") }
    var savedDraft by rememberSaveable { mutableStateOf(false) }
    var confirmPublish by rememberSaveable { mutableStateOf(false) }
    var confirmExit by rememberSaveable { mutableStateOf(false) }
    val todayEntries = remember { mutableStateListOf<WorkLog>() }
    val historyLogs = MockData.logs.filter { it.date != MockData.demoToday }.sortedWith(compareByDescending<WorkLog> { it.date.ordinal() }.thenByDescending { it.time })
    val filteredHistory = if (historyFilter == "全部日期") historyLogs else historyLogs.filter { it.date.shortLabel() == historyFilter }
    val mutedColors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Ink.copy(alpha = .7f), unfocusedBorderColor = Ink.copy(alpha = .3f), focusedLabelColor = Ink, cursorColor = Ink)
    fun closeEditor() {
        if (draftLog.isNotBlank() || draftTask != "不关联任务") confirmExit = true
        else writing = false
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            PageHeader("日志", "记录工作，回顾成长", actions = { IconButton({ notifications = true }) { Icon(Icons.Default.Email, "消息") } })
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                item { Text("今日工作日志", fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                items(todayEntries + MockData.logs.filter { it.date == MockData.demoToday }.sortedByDescending { it.time }) { log -> DetailCard(log.time, log.content, Lilac) { onOpen("日志 · ${log.time}") } }
                item { Button({ writing = true }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Ink), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(6.dp)); Text(if (savedDraft) "继续编辑草稿" else "记录今天的工作") } }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("历史日志", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                        Box {
                            OutlinedButton({ filterOpen = true }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text(historyFilter); Icon(Icons.Default.ArrowDropDown, null) }
                            DropdownMenu(filterOpen, { filterOpen = false }) {
                                (listOf("全部日期") + historyLogs.map { it.date.shortLabel() }.distinct()).forEach { option -> DropdownMenuItem({ Text(option) }, { historyFilter = option; filterOpen = false; expandedLogs = false }) }
                            }
                        }
                    }
                }
                items(filteredHistory.take(if (expandedLogs) 10 else 5)) { log -> DetailCard("${log.date.shortLabel()} · ${log.time}", log.content, Color.White) { onOpen("日志 · ${log.time}") } }
                if (filteredHistory.size > 5) item { TextButton({ expandedLogs = !expandedLogs }, Modifier.fillMaxWidth()) { Text(if (expandedLogs) "收起" else "展开") } }
                if (filteredHistory.isEmpty()) item { Text("所选日期暂无日志", color = Ink.copy(alpha = .6f)) }
            }
        }
        if (writing || notifications) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .38f)).clickable { if (writing) closeEditor() else notifications = false })
        if (writing) {
            androidx.activity.compose.BackHandler { closeEditor() }
            Card(Modifier.align(Alignment.Center).fillMaxWidth(.9f).fillMaxHeight(.8f).imePadding().clickable { }, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = WarmCard)) {
                Column(Modifier.fillMaxSize().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("记录今天的工作", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton({ closeEditor() }) { Icon(Icons.Default.Close, "关闭") } }
                    Text("1. 日志日期", fontWeight = FontWeight.Bold)
                    Text(MockData.demoToday.shortLabelWithWeekday(), Modifier.fillMaxWidth().padding(vertical = 12.dp), color = Ink.copy(alpha = .7f))
                    Text("2. 关联任务（可选）", fontWeight = FontWeight.Bold)
                    var taskMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton({ taskMenu = true }, Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Ink.copy(alpha = .3f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Text(draftTask, Modifier.weight(1f), textAlign = TextAlign.Start); Icon(Icons.Default.ArrowDropDown, null) }
                        DropdownMenu(taskMenu, { taskMenu = false }) { (listOf("不关联任务") + tasks.map { it.title }).forEach { option -> DropdownMenuItem({ Text(option) }, { draftTask = option; savedDraft = false; taskMenu = false }) } }
                    }
                    Text("3. 一句话记录完成的工作", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                    OutlinedTextField(draftLog, { draftLog = it; savedDraft = false }, Modifier.fillMaxWidth().weight(1f), placeholder = { Text("记录今天完成的工作") }, colors = mutedColors)
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton({ savedDraft = true; writing = false }, Modifier.weight(1f), enabled = draftLog.isNotBlank(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) { Text("保存为草稿") }
                        Button({ confirmPublish = true }, Modifier.weight(1f), enabled = draftLog.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("发布") }
                    }
                }
            }
        }
        if (confirmPublish) AlertDialog(onDismissRequest = { confirmPublish = false }, title = { Text("确认发布？") }, text = { Text("发布后将显示在今日工作日志中。") }, confirmButton = { TextButton({ todayEntries.add(0, WorkLog("draft-${todayEntries.size + 1}", draftLog, MockData.userName, MockData.demoToday, "刚刚")); confirmPublish = false; savedDraft = false; draftLog = ""; draftTask = "不关联任务"; writing = false }) { Text("确认发布") } }, dismissButton = { TextButton({ confirmPublish = false }) { Text("取消") } })
        if (confirmExit) AlertDialog(onDismissRequest = { confirmExit = false }, title = { Text("放弃未保存内容？") }, text = { Text("退出后当前填写内容会被清空。") }, confirmButton = { TextButton({ confirmExit = false; draftLog = ""; draftTask = "不关联任务"; savedDraft = false; writing = false }) { Text("放弃并退出") } }, dismissButton = { TextButton({ confirmExit = false }) { Text("继续编辑") } })
        if (notifications) NotificationDrawer(onClose = { notifications = false })
    }
}

@Composable private fun BoxScope.NotificationDrawer(onClose: () -> Unit) {
    Card(Modifier.align(Alignment.CenterEnd).fillMaxWidth(.75f).fillMaxHeight(), shape = RoundedCornerShape(0.dp), colors = CardDefaults.cardColors(containerColor = WarmCard), elevation = CardDefaults.cardElevation(14.dp)) {
        Column(Modifier.fillMaxSize().padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("消息", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); IconButton(onClose) { Icon(Icons.Default.Close, "关闭") } }; Text("今天收到的更新", color = Ink.copy(alpha = .6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp)); DetailCard("新任务", "完成登录模块 · 已派发", Peach); Spacer(Modifier.height(10.dp)); DetailCard("审核结果", "阶段汇报已通过", Mint); Spacer(Modifier.height(10.dp)); DetailCard("下属日报", "暂无新的日报更新", Color.White) }
    }
}
@Composable private fun AiMapScreen() { Column(Modifier.fillMaxSize().padding(16.dp)) { PageHeader("AI地图"); Spacer(Modifier.height(48.dp)); Icon(Icons.Default.Info, null, Modifier.size(70.dp).align(Alignment.CenterHorizontally), tint = Coral); Text("AI 地图", Modifier.fillMaxWidth().padding(top = 18.dp), textAlign = TextAlign.Center, fontSize = 28.sp, fontWeight = FontWeight.Bold); Text("功能规划中", Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center, color = Ink.copy(alpha = .6f)) } }
@Composable private fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        PageHeader("我的", "个人资料")
        Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("组织信息", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    ProfileInfoRow("所属企业", "Pandora 演示企业")
                    HorizontalDivider(color = Ink.copy(alpha = .1f))
                    ProfileInfoRow("所属部门", MockData.department)
                    ProfileInfoRow("当前角色", "部门老总（模拟）")
                    HorizontalDivider(color = Ink.copy(alpha = .1f))
                    Text("管理团队", fontWeight = FontWeight.Bold)
                    MockData.teamLeaders.forEach { Text(it, color = Ink.copy(alpha = .75f)) }
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
    val task = tasks.firstOrNull { it.id == title || it.title == title }
    val displayTitle = task?.title ?: title
    androidx.activity.compose.BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        PageHeader(displayTitle, "演示详情", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp)) {
            Text("${displayTitle}详情", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
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
            } else {
                Text("这里展示低保真原型中的可读内容和返回路径。\n\n真实任务、日志、权限和 AI 服务将在后续需求确认后接入。", color = Ink.copy(alpha = .75f), lineHeight = 24.sp)
            }
            Spacer(Modifier.height(22.dp))
            DetailCard("当前状态", "模拟数据 · 仅用于线下讨论", CreamDeep)
        }
    }
}
