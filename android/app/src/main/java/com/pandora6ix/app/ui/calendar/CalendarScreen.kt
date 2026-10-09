package com.pandora6ix.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.mock.*
import com.pandora6ix.app.ui.theme.*

@Composable
fun CalendarScreen(
    mode: CalendarMode,
    cursor: DemoDate,
    tasks: List<WorkTask>,
    canCreateTasks: Boolean,
    onModeChange: (CalendarMode) -> Unit,
    onCursorChange: (DemoDate) -> Unit,
    onCreate: (WorkTask) -> Unit,
    onOpen: (WorkTask) -> Unit
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton({ menu = true }) { Icon(if (mode == CalendarMode.MONTH) Icons.Default.CalendarMonth else Icons.Default.ViewWeek, "选择日周月视图", tint = Ink) }
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    TextButton({ menu = true }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) {
                        Text(mode.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(menu, { menu = false }) {
                        CalendarMode.values().forEach { option -> DropdownMenuItem(text = { Text(option.label) }, onClick = { menu = false; onModeChange(option) }) }
                    }
                }
                Spacer(Modifier.width(48.dp))
            }
            val weekStart = mondayOfWeek(cursor)
            val dates = (0..6).map { weekStart.plusDays(it) }
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(when (mode) {
                    CalendarMode.MONTH -> cursor.monthLabel()
                    CalendarMode.WEEK -> "${weekStart.shortLabel()}—${dates.last().shortLabel()}"
                    CalendarMode.DAY -> cursor.shortLabelWithWeekday()
                }, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton({ onCursorChange(when (mode) { CalendarMode.MONTH -> cursor.plusMonths(-1); CalendarMode.WEEK -> cursor.plusDays(-7); CalendarMode.DAY -> cursor.plusDays(-1) }) }) { Icon(Icons.Default.ArrowBack, "上一${if (mode == CalendarMode.MONTH) "月" else if (mode == CalendarMode.WEEK) "周" else "天"}", tint = Ink) }
                IconButton({ onCursorChange(when (mode) { CalendarMode.MONTH -> cursor.plusMonths(1); CalendarMode.WEEK -> cursor.plusDays(7); CalendarMode.DAY -> cursor.plusDays(1) }) }) { Icon(Icons.Default.ArrowForward, "下一${if (mode == CalendarMode.MONTH) "月" else if (mode == CalendarMode.WEEK) "周" else "天"}", tint = Ink) }
            }
            Box(Modifier.fillMaxWidth().weight(1f)) {
                if (mode == CalendarMode.MONTH) {
                    CompactMonth(cursor, tasks) { date -> onCursorChange(date); onModeChange(CalendarMode.WEEK) }
                } else {
                    Column(Modifier.fillMaxSize()) {
                        WeekDates(dates, cursor, mode == CalendarMode.WEEK) { date -> onCursorChange(date); onModeChange(CalendarMode.DAY) }
                        val displayedDates = if (mode == CalendarMode.DAY) listOf(cursor) else dates
                        AllDayTasks(displayedDates, tasks, onSelect = { task, date -> if (mode == CalendarMode.DAY) onOpen(task) else { onCursorChange(date); onModeChange(CalendarMode.DAY) } })
                        HorizontalDivider(color = Ink.copy(alpha = .08f))
                        key(mode, weekStart) {
                            TimeGrid(displayedDates, tasks, mode == CalendarMode.DAY) { task, date -> if (mode == CalendarMode.DAY) onOpen(task) else { onCursorChange(date); onModeChange(CalendarMode.DAY) } }
                        }
                    }
                }
            }
        }
        if (canCreateTasks) FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 48.dp), shape = CircleShape, containerColor = WarmOrange, contentColor = Color.White) { Icon(Icons.Default.Add, "添加任务", Modifier.size(30.dp)) }
        val atToday = when (mode) {
            CalendarMode.DAY -> cursor == MockData.demoToday
            CalendarMode.WEEK -> mondayOfWeek(cursor) == mondayOfWeek(MockData.demoToday)
            CalendarMode.MONTH -> cursor.year == MockData.demoToday.year && cursor.month == MockData.demoToday.month
        }
        if (!atToday) Button(onClick = { onCursorChange(MockData.demoToday) }, modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(.25f).height(34.dp), shape = RoundedCornerShape(topStart = 17.dp, bottomStart = 17.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("回到今天", fontSize = 12.sp, maxLines = 1) }
    }
    if (adding && canCreateTasks) TaskEntrySheet(cursor, canCreateTasks, onDismiss = { adding = false }, onSave = { task -> if (canCreateTasks) { onCreate(task); onCursorChange(task.start); adding = false } })
}

@Composable private fun WeekDates(dates: List<DemoDate>, selected: DemoDate, weekMode: Boolean, onSelect: (DemoDate) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 40.dp, end = 8.dp, bottom = 8.dp)) {
        dates.forEach { date ->
            Column(Modifier.weight(1f).clickable { onSelect(date) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("周${date.weekdayLabel()}", fontSize = 12.sp, color = Ink.copy(alpha = .65f))
                Box(Modifier.padding(top = 3.dp).size(32.dp).clip(CircleShape).background(if (!weekMode && date == selected) Peach else if (date == MockData.demoToday) CreamDeep else Color.Transparent), contentAlignment = Alignment.Center) {
                    Text("${date.day}", fontSize = 17.sp, fontWeight = if (date == selected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable private fun CompactMonth(cursor: DemoDate, tasks: List<WorkTask>, onSelect: (DemoDate) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(20.dp)) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text("周$it", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Ink.copy(alpha = .6f)) } }
        monthWeeks(cursor).forEach { week ->
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).calendarGuides()) {
                val lanes = packTaskRows(tasks, week)
                val capacity = ((maxHeight.value - 24f) / 18f).toInt().coerceAtLeast(1)
                val visible = if (lanes.size > capacity) (capacity - 1).coerceAtLeast(0) else lanes.size
                Column(Modifier.fillMaxSize().clickable { onSelect(week.first()) }) {
                    Row(Modifier.fillMaxWidth().height(24.dp)) {
                        week.forEach { date ->
                            Box(Modifier.weight(1f).fillMaxHeight().clickable { onSelect(date) }, contentAlignment = Alignment.Center) {
                                Text("${date.day}", fontSize = 15.sp, fontWeight = if (date == MockData.demoToday) FontWeight.Bold else FontWeight.Normal, color = if (date.month == cursor.month) Ink else Ink.copy(alpha = .3f), modifier = Modifier.clip(CircleShape).background(if (date == MockData.demoToday) Peach else Color.Transparent).padding(horizontal = 6.dp, vertical = 1.dp))
                            }
                        }
                    }
                    lanes.take(visible).forEach { lane -> DateLane(lane, week, 17, compact = true) { _, date -> onSelect(date) }; Spacer(Modifier.height(1.dp)) }
                    val remaining = lanes.drop(visible).sumOf { it.size }
                    if (remaining > 0) Box(Modifier.fillMaxWidth().height(18.dp).clickable { onSelect(week.first()) }, contentAlignment = Alignment.CenterEnd) { Text("+$remaining 条计划 ›", Modifier.padding(end = 6.dp), fontSize = 11.sp, color = Ink.copy(alpha = .7f)) }
                }
            }
        }
    }
}

@Composable private fun AllDayTasks(dates: List<DemoDate>, tasks: List<WorkTask>, onSelect: (WorkTask, DemoDate) -> Unit) {
    val lanes = packTaskRows(tasks.filter(::isAllDay), dates)
    Row(Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 6.dp)) {
        Text("全天", Modifier.width(40.dp).padding(top = 4.dp), fontSize = 12.sp, textAlign = TextAlign.Center, color = Ink.copy(alpha = .7f))
        Column(Modifier.weight(1f).heightIn(max = 150.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (lanes.isEmpty()) Text("暂无全天任务", Modifier.padding(vertical = 7.dp), fontSize = 12.sp, color = Ink.copy(alpha = .45f))
            lanes.forEach { DateLane(it, dates, 25, compact = false, onSelect = onSelect) }
        }
    }
}

@Composable private fun DateLane(lane: List<TaskSegment>, dates: List<DemoDate>, height: Int, compact: Boolean, onSelect: (WorkTask, DemoDate) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(height.dp)) {
        val cellWidth = maxWidth / dates.size
        lane.forEach { segment ->
            Box(Modifier.offset(x = cellWidth * segment.first).width(cellWidth * (segment.last - segment.first + 1)).fillMaxHeight().padding(horizontal = 1.dp).clip(RoundedCornerShape(3.dp)).background(taskColor(segment.task))
                .semantics { role = Role.Button; onClick("查看${segment.task.title}") { onSelect(segment.task, dates[segment.first]); true } }
                .pointerInput(segment.task.id, dates) { detectTapGestures { position -> val day = (position.x / (size.width.toFloat() / (segment.last - segment.first + 1))).toInt().coerceIn(0, segment.last - segment.first); onSelect(segment.task, dates[segment.first + day]) } }, contentAlignment = Alignment.CenterStart) {
                Text(segment.task.title, Modifier.padding(horizontal = 3.dp), fontSize = if (compact) 11.sp else 12.sp, lineHeight = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Ink)
            }
        }
    }
}

private fun taskColor(task: WorkTask): Color = listOf(Peach, Sky, Mint, Lilac)[task.colorIndex.mod(4)]
private fun Modifier.calendarGuides(): Modifier = drawBehind {
    val effect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
    for (i in 1..6) drawLine(Ink.copy(alpha = .06f), Offset(size.width * i / 7, 0f), Offset(size.width * i / 7, size.height), pathEffect = effect)
    drawLine(Ink.copy(alpha = .08f), Offset(0f, size.height), Offset(size.width, size.height), pathEffect = effect)
}

@Composable private fun TimeGrid(dates: List<DemoDate>, tasks: List<WorkTask>, dayMode: Boolean, onSelect: (WorkTask, DemoDate) -> Unit) {
    val hourHeight = if (dayMode) 60.dp else 56.dp
    val density = LocalDensity.current
    val scroll = rememberScrollState(with(density) { (hourHeight * 8).roundToPx() })
    Row(Modifier.fillMaxSize().padding(end = 8.dp).verticalScroll(scroll)) {
        Column(Modifier.width(40.dp)) {
            (0..23).forEach { hour -> Text("%02d".format(hour), Modifier.fillMaxWidth().height(hourHeight).padding(top = 2.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Ink.copy(alpha = .5f)) }
        }
        BoxWithConstraints(Modifier.weight(1f).height(hourHeight * 24).drawBehind {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            for (hour in 0..24) drawLine(Ink.copy(alpha = .08f), Offset(0f, hourHeight.toPx() * hour), Offset(size.width, hourHeight.toPx() * hour), pathEffect = dash)
            for (day in 1 until dates.size) drawLine(Ink.copy(alpha = .07f), Offset(size.width * day / dates.size, 0f), Offset(size.width * day / dates.size, size.height), pathEffect = dash)
        }) {
            val dayWidth = maxWidth / dates.size
            dates.forEachIndexed { index, date ->
                val entries = tasks.mapNotNull { task -> timeRangeOnDate(task, date)?.let { task to it } }.filter { !it.second.isEmpty() }.sortedBy { it.second.first }
                // One column per simultaneous cluster, allowing non-overlapping events to regain full width.
                val clusters = mutableListOf<MutableList<Pair<WorkTask, IntRange>>>()
                entries.forEach { entry ->
                    val last = clusters.lastOrNull()
                    if (last != null && entry.second.first <= last.maxOf { it.second.last }) last.add(entry) else clusters.add(mutableListOf(entry))
                }
                clusters.forEach { cluster ->
                    val laneEnds = mutableListOf<Int>()
                    val assigned = cluster.map { entry ->
                        var lane = laneEnds.indexOfFirst { it < entry.second.first }
                        if (lane < 0) { lane = laneEnds.size; laneEnds.add(entry.second.last) } else laneEnds[lane] = entry.second.last
                        Triple(entry.first, entry.second, lane)
                    }
                    val eventWidth = dayWidth / laneEnds.size
                    assigned.forEach { (task, range, lane) ->
                        Box(Modifier.offset(x = dayWidth * index + eventWidth * lane, y = hourHeight * (range.first / 60f)).width(eventWidth).height(hourHeight * ((range.last - range.first + 1) / 60f)).padding(1.dp).clip(RoundedCornerShape(4.dp)).background(taskColor(task)).clickable { onSelect(task, date) }) {
                            Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text(task.title, fontSize = if (dayMode) 13.sp else 11.sp, lineHeight = if (dayMode) 16.sp else 13.sp, maxLines = if (dayMode) 2 else 3, overflow = TextOverflow.Ellipsis)
                                if (dayMode && range.last - range.first >= 30) Text("${minuteLabel(range.first)}—${minuteLabel(range.last + 1)}", fontSize = 11.sp, color = Ink.copy(alpha = .7f))
                            }
                        }
                    }
                }
            }
        }
    }
}
