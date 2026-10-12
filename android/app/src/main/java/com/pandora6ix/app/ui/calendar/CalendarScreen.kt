package com.pandora6ix.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
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
import kotlin.math.abs

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
    // 标签筛选：选中的标签任务必须同时全部具备（AND）才在视图中显示
    var filterTags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showTagFilter by rememberSaveable { mutableStateOf(false) }
    val today = todayDate()
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton({ menu = true }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) {
                        Text(mode.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, "选择视图")
                    }
                    DropdownMenu(menu, { menu = false }) {
                        CalendarMode.entries.forEach { option -> DropdownMenuItem({ Text(option.label) }, { menu = false; onModeChange(option) }) }
                    }
                }
            }
            val weekStart = mondayOfWeek(cursor)
            val dates = (0..6).map { weekStart.plusDays(it) }
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(when (mode) {
                    CalendarMode.YEAR -> "${cursor.year}年"
                    CalendarMode.MONTH -> cursor.monthLabel()
                    CalendarMode.WEEK -> "${weekStart.shortLabel()}—${dates.last().shortLabel()}"
                    CalendarMode.DAY -> cursor.shortLabelWithWeekday()
                }, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton({ showTagFilter = true }) {
                    Icon(Icons.Default.Search, "按标签筛选任务", tint = if (filterTags.isEmpty()) Ink else WarmOrange)
                }
            }
            if (filterTags.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("仅显示：", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                filterTags.forEach { name ->
                    Text(name, Modifier.clip(RoundedCornerShape(10.dp)).background(taskTagColor(name)?.let { Color(it).copy(alpha = .4f) } ?: Ink.copy(alpha = .15f)).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, color = Ink)
                }
                TextButton({ filterTags = emptyList() }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("清除", fontSize = 12.sp, color = WarmOrange) }
            }
            // 应用标签筛选后的任务集合：空筛选 = 全部任务
            val visibleTasks = remember(tasks, filterTags) { if (filterTags.isEmpty()) tasks else tasks.filter { task -> filterTags.all { it in task.tags } } }
            val yearGridState = rememberLazyGridState(initialFirstVisibleItemIndex = YearGridAnchorIndex)
            Box(Modifier.fillMaxWidth().weight(1f).pinchToZoom(mode, onModeChange).swipeNavigate(mode) { forward ->
                onCursorChange(when (mode) {
                    CalendarMode.MONTH -> cursor.plusMonths(if (forward) 1 else -1)
                    CalendarMode.WEEK -> cursor.plusDays(if (forward) 7 else -7)
                    CalendarMode.DAY -> cursor.plusDays(if (forward) 1 else -1)
                    else -> cursor
                })
            }) {
                when (mode) {
                    CalendarMode.YEAR -> YearGrid(cursor, visibleTasks, today, yearGridState, onYearChange = { onCursorChange(cursor.plusYears(it - cursor.year)) }, onSelectDate = onCursorChange, onSelectMonth = { onCursorChange(it); onModeChange(CalendarMode.MONTH) })
                    CalendarMode.MONTH -> CompactMonth(cursor, visibleTasks, today, onSelect = onCursorChange, onOpen = onOpen)
                    CalendarMode.WEEK, CalendarMode.DAY -> Column(Modifier.fillMaxSize()) {
                        val visibleDates = if (mode == CalendarMode.DAY) listOf(cursor) else dates
                        WeekDates(visibleDates, today) { date ->
                            onCursorChange(date)
                            if (mode == CalendarMode.WEEK) onModeChange(CalendarMode.DAY)
                        }
                        AllDayTasks(visibleDates, visibleTasks, onSelect = { task, date -> onCursorChange(date); onOpen(task) })
                        HorizontalDivider(color = Ink.copy(alpha = .08f))
                        key(mode, if (mode == CalendarMode.DAY) cursor else weekStart) {
                            TimeGrid(visibleDates, visibleTasks, mode == CalendarMode.DAY) { task, _ -> onOpen(task) }
                        }
                    }
                }
            }
        }
        if (canCreateTasks) FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 48.dp), shape = CircleShape, containerColor = WarmOrange, contentColor = Color.White) { Icon(Icons.Default.Add, "添加任务", Modifier.size(30.dp)) }
        val atToday = when (mode) {
            CalendarMode.DAY -> cursor == today
            CalendarMode.WEEK -> mondayOfWeek(cursor) == mondayOfWeek(today)
            CalendarMode.MONTH -> cursor.year == today.year && cursor.month == today.month
            CalendarMode.YEAR -> cursor.year == today.year
        }
        if (!atToday) Button(onClick = { onCursorChange(today) }, modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(.25f).height(34.dp), shape = RoundedCornerShape(topStart = 17.dp, bottomStart = 17.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = Ink)) { Text("回到今天", fontSize = 12.sp, maxLines = 1) }
    }
    if (adding && canCreateTasks) TaskEntrySheet(cursor, canCreateTasks, onDismiss = { adding = false }, onSave = { task -> if (canCreateTasks) { onCreate(task); onCursorChange(task.start); adding = false } })
    if (showTagFilter) {
        var pending by remember { mutableStateOf(filterTags) }
        AlertDialog(
            onDismissRequest = { showTagFilter = false },
            title = { Text("按标签筛选") },
            text = {
                Column {
                    Text("可多选；任务需同时包含所选的全部标签才会显示。", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                    Spacer(Modifier.height(6.dp))
                    Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                        MockData.taskTags.forEach { tag ->
                            val selected = tag.name in pending
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { pending = if (selected) pending - tag.name else pending + tag.name }.padding(horizontal = 4.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(Color(tag.color)))
                                Spacer(Modifier.width(10.dp))
                                Text(tag.name, Modifier.weight(1f), fontSize = 15.sp)
                                if (selected) Icon(Icons.Default.Check, "已选", Modifier.size(18.dp), tint = Ink)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton({ filterTags = pending; showTagFilter = false }) { Text("确定", color = WarmOrange, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton({ showTagFilter = false }) { Text("取消", color = Ink.copy(alpha = .7f)) } }
        )
    }
}

@Composable private fun WeekDates(dates: List<DemoDate>, today: DemoDate, onSelect: (DemoDate) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 40.dp, end = 8.dp, bottom = 8.dp)) {
        dates.forEach { date ->
            Column(Modifier.weight(1f).clickable { onSelect(date) }, horizontalAlignment = Alignment.CenterHorizontally) {
                Text("周${date.weekdayLabel()}", fontSize = 12.sp, color = Ink.copy(alpha = .65f))
                Box(Modifier.padding(top = 3.dp).size(32.dp).clip(CircleShape).background(if (date == today) CreamDeep else Color.Transparent), contentAlignment = Alignment.Center) {
                    Text("${date.day}", fontSize = 17.sp, lineHeight = 17.sp, fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
}

@Composable private fun CompactMonth(cursor: DemoDate, tasks: List<WorkTask>, today: DemoDate, onSelect: (DemoDate) -> Unit, onOpen: (WorkTask) -> Unit) {
    var overflowDates by remember { mutableStateOf<List<DemoDate>?>(null) }
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
                                Text("${date.day}", fontSize = 15.sp, lineHeight = 15.sp, fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal, color = if (date.month == cursor.month) Ink else Ink.copy(alpha = .3f), modifier = Modifier.clip(CircleShape).background(if (date == today) Peach else Color.Transparent).padding(horizontal = 6.dp, vertical = 1.dp))
                            }
                        }
                    }
                    lanes.take(visible).forEach { lane -> DateLane(lane, week, 17, compact = true) { task, _ -> onOpen(task) }; Spacer(Modifier.height(1.dp)) }
                    val remaining = lanes.drop(visible).sumOf { it.size }
                    if (remaining > 0) Box(Modifier.fillMaxWidth().height(18.dp).clickable { overflowDates = week }, contentAlignment = Alignment.CenterEnd) { Text("+$remaining 条计划 ›", Modifier.padding(end = 6.dp), fontSize = 11.sp, color = Ink.copy(alpha = .7f)) }
                }
            }
        }
    }
    overflowDates?.let { dates ->
        AlertDialog(onDismissRequest = { overflowDates = null }, title = { Text("本周计划") }, text = {
            Column(Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) {
                tasks.filter { it.end >= dates.first() && it.start <= dates.last() }.forEach { task ->
                    TextButton({ overflowDates = null; onOpen(task) }, Modifier.fillMaxWidth()) { Text(task.title, Modifier.fillMaxWidth()) }
                }
            }
        }, confirmButton = { TextButton({ overflowDates = null }) { Text("关闭") } })
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

// 任务颜色：优先取主标签颜色，无标签任务回退到既有色板
private fun taskColor(task: WorkTask): Color =
    task.tags.firstOrNull()?.let { name -> taskTagColor(name)?.let { Color(it) } }
        ?: listOf(Peach, Sky, Mint, Lilac)[task.colorIndex.mod(4)]
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

// 双指缩放：两指张开（间距由短到长）切到更小时间周期，两指收拢切到更大周期
// 在 Initial 通道提前截获：一旦出现多指就消费该手势全部事件，避免两指分别触发子项点击（如月视图两行周）与触摸反馈
private fun Modifier.pinchToZoom(mode: CalendarMode, onModeChange: (CalendarMode) -> Unit): Modifier = pointerInput(mode) {
    awaitEachGesture {
        var accumulated = 1f
        var multiTouch = false
        awaitFirstDown(requireUnconsumed = false)
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            val pressed = event.changes.filter { it.pressed }
            if (pressed.size >= 2) {
                multiTouch = true
                val current = (pressed[0].position - pressed[1].position).getDistance()
                val previous = (pressed[0].previousPosition - pressed[1].previousPosition).getDistance()
                if (previous > 1f && current > 1f) {
                    accumulated *= current / previous
                    val target = when {
                        accumulated >= 1.35f -> mode.zoomIn()
                        accumulated <= 0.74f -> mode.zoomOut()
                        else -> null
                    }
                    if (target != null) {
                        accumulated = 1f
                        if (target != mode) onModeChange(target)
                    }
                }
            }
            if (multiTouch) event.changes.forEach { it.consume() }
            if (event.changes.none { it.pressed }) break
        }
    }
}

// 单指滑动导航：月视图上下滑动切换月份，周/日视图左右滑动切换日期，替代原左右箭头
// 年视图不参与分片切换，其年份变化由网格的连续纵向滚动直接驱动
// 在 Initial 通道观察，触发后消费剩余事件避免误触子项点击；出现多指（交给双指缩放）或事件已被消费时放弃本次手势
@Composable
private fun Modifier.swipeNavigate(mode: CalendarMode, onAdvance: (Boolean) -> Unit): Modifier {
    val advance by rememberUpdatedState(onAdvance)
    return pointerInput(mode) {
        if (mode == CalendarMode.YEAR) return@pointerInput
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var dx = 0f
            var dy = 0f
            var triggered = false
            val threshold = 48.dp.toPx()
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.changes.none { it.pressed }) break
                if (event.changes.count { it.pressed } > 1 || event.changes.any { it.isConsumed }) break
                val change = event.changes.firstOrNull { it.id == down.id } ?: continue
                val delta = change.positionChange()
                dx += delta.x
                dy += delta.y
                if (!triggered) {
                    val horizontal = mode == CalendarMode.WEEK || mode == CalendarMode.DAY
                    val forward: Boolean? = when {
                        horizontal && abs(dx) > threshold && abs(dx) > abs(dy) -> dx < 0
                        !horizontal && abs(dy) > threshold && abs(dy) > abs(dx) -> dy < 0
                        else -> null
                    }
                    if (forward != null) {
                        advance(forward)
                        triggered = true
                    }
                }
                if (triggered) event.changes.forEach { it.consume() }
            }
        }
    }
}

// 年视图：跨多年连续滚动的月份网格（锚点前后各 YearGridSpanYears 年），拖动跟手、跨年自然衔接，不再分片跳年
// 顶部年份随滚动实时上报（驱动日期行标题与「回到今天」状态）；滚动接近列表边缘时静默重锚并保持视觉位置，近似无限滚动
// 外部年份跳转（如「回到今天」）时定位到目标年一月
private const val YearGridSpanYears = 12
private const val YearGridAnchorIndex = YearGridSpanYears * 12

@Composable private fun YearGrid(
    cursor: DemoDate,
    tasks: List<WorkTask>,
    today: DemoDate,
    state: LazyGridState,
    onYearChange: (Int) -> Unit,
    onSelectDate: (DemoDate) -> Unit,
    onSelectMonth: (DemoDate) -> Unit
) {
    var anchorYear by remember { mutableStateOf(cursor.year) }
    var lastReportedYear by remember { mutableStateOf(cursor.year) }
    val startYear = anchorYear - YearGridSpanYears
    val currentStartYear by rememberUpdatedState(startYear)
    val reportYear by rememberUpdatedState(onYearChange)
    LaunchedEffect(state) {
        snapshotFlow { state.layoutInfo.visibleItemsInfo.firstOrNull()?.index }
            .collect { index ->
                if (index == null) return@collect
                val year = currentStartYear + index / 12
                lastReportedYear = year
                reportYear(year)
                if (year <= currentStartYear + 1 || year >= currentStartYear + YearGridSpanYears * 2 - 2) {
                    val offset = state.firstVisibleItemScrollOffset
                    anchorYear = year
                    state.scrollToItem(YearGridAnchorIndex + index % 12, offset)
                }
            }
    }
    LaunchedEffect(cursor.year) {
        if (cursor.year != lastReportedYear) {
            val targetIndex = (cursor.year - startYear) * 12
            lastReportedYear = cursor.year
            if (targetIndex in 0 until YearGridSpanYears * 2 * 12) {
                state.scrollToItem(targetIndex)
            } else {
                anchorYear = cursor.year
                state.scrollToItem(YearGridAnchorIndex)
            }
        }
    }
    LazyVerticalGrid(state = state, columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(YearGridSpanYears * 2 * 12) { index ->
            val month = DemoDate(currentStartYear + index / 12, index % 12 + 1, 1)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(WarmCard).border(1.dp, CreamDeep, RoundedCornerShape(10.dp)).padding(4.dp)) {
                Text(if (month.month == 1) "${month.year}年1月" else "${month.month}月", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().clickable { onSelectMonth(month) }.padding(horizontal = 2.dp))
                Row(Modifier.fillMaxWidth().padding(top = 1.dp)) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text(it, Modifier.weight(1f), fontSize = 8.sp, lineHeight = 8.sp, textAlign = TextAlign.Center, color = Ink.copy(alpha = .45f)) } }
                monthWeeks(month).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            val inMonth = date.month == month.month
                            val planTask = if (inMonth) tasks.firstOrNull { date >= it.start && date <= it.end } else null
                            Column(Modifier.weight(1f).clickable { onSelectDate(date) }, horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(20.dp).clip(CircleShape).background(if (date == today) Peach else Color.Transparent), contentAlignment = Alignment.Center) {
                                    Text("${date.day}", fontSize = 9.sp, lineHeight = 9.sp, fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal, color = if (inMonth) Ink else Ink.copy(alpha = .25f))
                                }
                                Box(Modifier.size(4.dp).clip(CircleShape).background(planTask?.let { taskColor(it) } ?: Color.Transparent))
                            }
                        }
                    }
                }
            }
        }
    }
}
