package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
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
    tasks: List<TaskItem>,
    canCreatePersonal: Boolean,
    canCreateDelegated: Boolean,
    availableTags: List<TagDto>,
    taskRepo: TaskRepository,
    onModeChange: (CalendarMode) -> Unit,
    onCursorChange: (DemoDate) -> Unit,
    onCreated: (TaskDto) -> Unit,
    onOpen: (TaskItem) -> Unit,
    taskScope: String = "mine",
    onTaskScopeChange: (String) -> Unit = {},
    currentUser: UserDto? = null,
    canReviewTasks: Boolean = true,
    businessTimezone: String? = null,
    loading: Boolean = false,
    error: String? = null,
    onRetry: () -> Unit = {},
    notice: String? = null
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    var adding by rememberSaveable { mutableStateOf(false) }
    // 标签筛选：选中的标签任务必须同时全部具备（AND）才在视图中显示
    var filterTags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var showTagFilter by rememberSaveable { mutableStateOf(false) }
    // 无审批权限时不应停留在"我管理的"（状态恢复等场景），自动切回"我的任务"
    LaunchedEffect(canReviewTasks, taskScope) {
        if (!canReviewTasks && taskScope == "managed") onTaskScopeChange("mine")
    }
    val today = todayDate()
    // 月视图消费定位信号；年视图用导航版本直接重建目标网格，避免旧布局参与跳转。
    var gridJumpTarget by remember { mutableStateOf<DemoDate?>(null) }
    var monthAnchorYear by remember { mutableIntStateOf(cursor.year) }
    var yearNavigationVersion by remember { mutableIntStateOf(0) }
    fun jumpToDate(date: DemoDate) {
        if (mode == CalendarMode.MONTH) monthAnchorYear = date.year
        if (mode == CalendarMode.YEAR) yearNavigationVersion++
        gridJumpTarget = if (mode == CalendarMode.MONTH) date else null
        onCursorChange(date)
    }
    // 每次进入卷轴视图都以当前日期定位，旧滚动位置不能覆盖 cursor。
    val onChangeModeRecenter: (CalendarMode) -> Unit = { target ->
        if (target == CalendarMode.MONTH) monthAnchorYear = cursor.year
        yearNavigationVersion++
        gridJumpTarget = if (target == CalendarMode.MONTH) cursor else null
        onModeChange(target)
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton({ menu = true }, colors = ButtonDefaults.textButtonColors(contentColor = Ink)) {
                        Text(mode.label, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, "选择视图")
                    }
                    DropdownMenu(menu, { menu = false }) {
                        CalendarMode.entries.forEach { option -> DropdownMenuItem({ Text(option.label) }, { menu = false; onChangeModeRecenter(option) }) }
                    }
                }
            }
            val weekStart = mondayOfWeek(cursor)
            val dates = (0..6).map { weekStart.plusDays(it) }
            var showDatePick by rememberSaveable { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(calendarPeriodTitle(mode, cursor), fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clickable { showDatePick = true })
                IconButton({ showTagFilter = true }) {
                    Icon(Icons.Default.Search, "按标签筛选任务", tint = if (filterTags.isEmpty()) Ink else WarmOrange)
                }
            }
            if (showDatePick) DateWheelDialog(
                initial = cursor,
                onDismiss = { showDatePick = false },
                onConfirm = { d -> showDatePick = false; jumpToDate(d) }
            )
            // 所有视图共用任务范围；"我管理的"仅对有审批能力的用户显示。
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf("mine" to "我的任务") + (if (canReviewTasks) listOf("managed" to "我管理的") else emptyList()) + listOf("created" to "我创建的")).forEach { (value, label) ->
                    val selected = taskScope == value
                    Text(
                        label,
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(if (selected) WarmOrange else WarmCard)
                            .clickable { if (!selected) onTaskScopeChange(value) }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Ink else Ink.copy(alpha = .65f),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
            if (notice != null) Text(notice, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), fontSize = 12.sp, color = Ink.copy(alpha = .6f))
            if (filterTags.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("仅显示：", fontSize = 12.sp, color = Ink.copy(alpha = .6f), maxLines = 1, softWrap = false)
                // 仅标签区域横向滚动，避免多选时换行挤占日历，并保持清除按钮可见。
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    filterTags.forEach { name ->
                        val chipBg = availableTags.firstOrNull { it.name == name }?.let { tagHexColor(it.colorHex).copy(alpha = .4f) } ?: Ink.copy(alpha = .15f)
                        Text(name, Modifier.clip(RoundedCornerShape(10.dp)).background(chipBg).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, color = Ink, maxLines = 1, softWrap = false)
                    }
                }
                TextButton({ filterTags = emptyList() }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("清除", fontSize = 12.sp, color = WarmOrange, maxLines = 1, softWrap = false) }
            }
            // 应用标签筛选后的任务集合：空筛选 = 全部任务
            val taskSnapshot = tasks.toList()
            val visibleTasks = remember(taskSnapshot, filterTags) { if (filterTags.isEmpty()) taskSnapshot else taskSnapshot.filter { task -> filterTags.all { name -> task.tags.any { it.name == name } } } }
            val density = LocalDensity.current
            // 月视图卷轴：索引与滚动状态上提，供捏合缩放（月→周按手势所在行定位）使用
            val monthScrollIndex = remember(monthAnchorYear) { lazy { buildMonthScrollIndex(monthAnchorYear - MonthScrollSpanYears, monthAnchorYear + MonthScrollSpanYears) } }
            val monthScrollState = key(monthAnchorYear) {
                rememberLazyListState(initialFirstVisibleItemIndex = if (mode == CalendarMode.MONTH) monthScrollIndex.value.monthItemIndex[monthKey(cursor)] ?: 0 else 0)
            }
            // 月→周捏合放大：按捏合中心的纵向位置定位到手指所在的那一周
            val onPinchZoomInCenter: (Float) -> Unit = { centerY ->
                val y = centerY - with(density) { 20.dp.toPx() }  // 扣除卷轴上方的星期表头高度
                val info = monthScrollState.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size }
                val row = info?.let { monthScrollIndex.value.items.getOrNull(it.index) }
                if (row != null && !row.isTitle) onCursorChange(row.week.first())
            }
            Box(Modifier.fillMaxWidth().weight(1f).pinchToZoom(mode, onChangeModeRecenter, onPinchZoomInCenter).swipeNavigate(mode) { forward ->
                onCursorChange(advanceCalendarDate(mode, cursor, forward))
            }) {
                when (mode) {
                    CalendarMode.YEAR -> key(yearNavigationVersion) {
                        val displayedVersion = yearNavigationVersion
                        YearGrid(cursor, visibleTasks, today,
                            onYearChange = { year ->
                                // 确认新日期后、下一次重组前，旧网格的滚动回调也必须失效。
                                if (yearNavigationVersion == displayedVersion) onCursorChange(cursor.plusYears(year - cursor.year))
                            },
                            onSelectDate = onCursorChange,
                            onSelectMonth = { yearNavigationVersion++; monthAnchorYear = it.year; gridJumpTarget = it; onCursorChange(it); onModeChange(CalendarMode.MONTH) }
                        )
                    }
                    CalendarMode.MONTH -> MonthScroll(cursor, visibleTasks, today, monthScrollIndex.value, monthScrollState, jumpTarget = gridJumpTarget, onJumpConsumed = { gridJumpTarget = null }, onCursorChange = onCursorChange, onOpen = onOpen, onOpenDay = { date -> onCursorChange(date); onModeChange(CalendarMode.DAY) })
                    CalendarMode.WEEK, CalendarMode.DAY -> Column(Modifier.fillMaxSize()) {
                        val visibleDates = if (mode == CalendarMode.DAY) listOf(cursor) else dates
                        WeekDates(visibleDates, today) { date ->
                            onCursorChange(date)
                            if (mode == CalendarMode.WEEK) onModeChange(CalendarMode.DAY)
                        }
                        HorizontalDivider(color = Ink.copy(alpha = .08f))
                        key(mode, if (mode == CalendarMode.DAY) cursor else weekStart) {
                            if (mode == CalendarMode.WEEK) WeekTaskStacks(visibleDates, visibleTasks, onOpen)
                            else TimeGrid(visibleDates, visibleTasks, true, today, businessTimezone, onSelect = { task, _ -> onOpen(task) })
                        }
                    }
                }
                // 加载/失败覆盖层：真实接口三态（不遮住顶部导航）
                if (loading) Box(Modifier.fillMaxSize().background(Cream.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CircularProgressIndicator(color = WarmOrange, strokeWidth = 3.dp)
                        Text("正在加载…", color = Ink.copy(alpha = .6f), fontSize = 13.sp)
                    }
                }
                if (error != null) Box(Modifier.fillMaxSize().background(Cream.copy(alpha = .45f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(error, color = Ink.copy(alpha = .75f))
                        Button(onRetry, colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton), shape = RoundedCornerShape(12.dp)) { Text("重试") }
                    }
                }
            }
        }
        if (canCreatePersonal || canCreateDelegated) FloatingActionButton(onClick = { adding = true }, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 48.dp), shape = CircleShape, containerColor = WarmOrange, contentColor = Color.White) { Icon(Icons.Default.Add, "添加任务", Modifier.size(30.dp)) }
        val atToday = when (mode) {
            CalendarMode.DAY -> cursor == today
            CalendarMode.WEEK -> mondayOfWeek(cursor) == mondayOfWeek(today)
            CalendarMode.MONTH -> cursor.year == today.year && cursor.month == today.month
            CalendarMode.YEAR -> cursor.year == today.year
        }
        val returnToCurrentLabel = when (mode) {
            CalendarMode.YEAR -> "回到今年"
            CalendarMode.MONTH -> "回到本月"
            CalendarMode.WEEK -> "回到本周"
            CalendarMode.DAY -> "回到今天"
        }
        if (!atToday) Button(onClick = { jumpToDate(today) }, modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(.25f).height(34.dp), shape = RoundedCornerShape(topStart = 17.dp, bottomStart = 17.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton)) { Text(returnToCurrentLabel, fontSize = 12.sp, maxLines = 1) }
    }
    if (adding && (canCreatePersonal || canCreateDelegated)) TaskEntrySheet(cursor, canCreatePersonal, canCreateDelegated, availableTags, taskRepo, businessTimezone, currentUser, onDismiss = { adding = false }, onCreated = { task -> onCreated(task) })
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
                        availableTags.forEach { tag ->
                            val selected = tag.name in pending
                            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { pending = if (selected) pending - tag.name else pending + tag.name }.padding(horizontal = 4.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(tagHexColor(tag.colorHex)))
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
    Row(Modifier.fillMaxWidth().padding(start = if (dates.size == 1) 40.dp else 8.dp, end = 8.dp, bottom = 8.dp)) {
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

// ===== 月视图卷轴：连续纵向滚动；每个月独立按星期列排布（上月尾不挤入下月首行），任务条裁剪在本月列内 =====
internal const val MonthScrollSpanYears = 5 // 以显式选定年份为基准构建

/** 卷轴内容项：月份大标题或该月的某一行（week 为含邻月补位的 7 列基准，渲染时只显示本月日） */
internal class MonthScrollItem(val month: DemoDate, val isTitle: Boolean, val week: List<DemoDate>) {
    val key: String = "${monthKey(month)}:${if (isTitle) "title" else week.first().toString()}"
}

/** 卷轴索引：全部内容项与「月份 → 项下标」映射 */
internal class MonthScrollIndex(val items: List<MonthScrollItem>, val monthItemIndex: Map<Long, Int>)

internal fun monthKey(d: DemoDate) = d.year * 12L + (d.month - 1)

internal fun buildMonthScrollIndex(startYear: Int, endYear: Int): MonthScrollIndex {
    val items = ArrayList<MonthScrollItem>()
    val monthItemIndex = HashMap<Long, Int>()
    var y = startYear
    while (y <= endYear) {
        var m = 1
        while (m <= 12) {
            val month = DemoDate(y, m, 1)
            monthItemIndex[monthKey(month)] = items.size
            items.add(MonthScrollItem(month, true, emptyList()))
            monthWeeks(month).forEach { week -> items.add(MonthScrollItem(month, false, week)) }
            m++
        }
        y++
    }
    return MonthScrollIndex(items, monthItemIndex)
}

@Composable
private fun MonthScroll(
    cursor: DemoDate,
    tasks: List<TaskItem>,
    today: DemoDate,
    index: MonthScrollIndex,
    state: LazyListState,
    jumpTarget: DemoDate?,
    onJumpConsumed: () -> Unit,
    onCursorChange: (DemoDate) -> Unit,
    onOpen: (TaskItem) -> Unit,
    onOpenDay: (DemoDate) -> Unit
) {
    val currentCursor by rememberUpdatedState(cursor)
    val currentJump by rememberUpdatedState(jumpTarget)
    val reportCursor by rememberUpdatedState(onCursorChange)
    // 滚动上报：首个可见项所属月份写回 cursor（按月平移，日期不变），驱动顶部标题与「回到本月」按钮
    LaunchedEffect(state, index) {
        snapshotFlow { state.firstVisibleItemIndex to state.isScrollInProgress }
            .collect { (idx, scrolling) ->
                if (!scrolling || currentJump != null) return@collect
                val item = index.items.getOrNull(idx) ?: return@collect
                if (monthKey(item.month) != monthKey(currentCursor)) reportCursor(currentCursor.plusMonths((monthKey(item.month) - monthKey(currentCursor)).toInt()))
            }
    }
    // 外部跳转（日期选择器/回到本月）：定位到目标月标题并置顶
    LaunchedEffect(jumpTarget, state, index) {
        val target = jumpTarget ?: return@LaunchedEffect
        index.monthItemIndex[monthKey(target)]?.let { state.scrollToItem(it) }
        onJumpConsumed()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(20.dp)) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text("周$it", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Ink.copy(alpha = .6f)) } }
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            items(index.items.size,
                key = { i -> index.items[i].key },
                contentType = { i -> index.items[i].isTitle }
            ) { i ->
                val item = index.items[i]
                if (item.isTitle) {
                    Text("${item.month.month}月", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.padding(top = 18.dp, bottom = 6.dp))
                } else {
                    MonthScrollRow(item.month, item.week, tasks, today, onCursorChange, onOpen, onOpenDay)
                }
            }
        }
    }
}

/** 月视图卷轴的一行：week 为 7 列基准（可含邻月补位日），仅渲染本月日期，任务条裁剪到本月列范围；
 *  行高固定：每天最多展示 3 条任务道，第 4 行按日显示灰色“+N”（正对该日列、不与任务重合），点击跳转当天日视图 */
@Composable
private fun MonthScrollRow(month: DemoDate, week: List<DemoDate>, tasks: List<TaskItem>, today: DemoDate, onCursorChange: (DemoDate) -> Unit, onOpen: (TaskItem) -> Unit, onOpenDay: (DemoDate) -> Unit) {
    val inMonth = week.map { it.year == month.year && it.month == month.month }
    val firstCol = inMonth.indexOfFirst { it }
    val lastCol = inMonth.lastIndexOf(true)
    if (firstCol < 0) return
    // 任务条按整周分道后裁剪到本月列范围，避免条带延伸进邻月的空白格
    val lanes = remember(month, week, tasks.toList()) {
        packTaskRows(tasks, week).mapNotNull { lane ->
            lane.mapNotNull { seg ->
                val f = maxOf(seg.first, firstCol); val l = minOf(seg.last, lastCol)
                if (f <= l) TaskSegment(seg.task, f, l) else null
            }
        }.filter { it.isNotEmpty() }
    }
    val capacity = 3  // 固定行高内可展示的任务条道数
    val shownLanes = lanes.take(capacity)
    // 每日未展示任务数：超出容量的分道中覆盖该列的任务条数
    val hiddenPerDay = IntArray(7)
    lanes.drop(capacity).forEach { lane -> lane.forEach { seg -> for (c in seg.first..seg.last) hiddenPerDay[c]++ } }
    val dayRowHeight = 30.dp
    val laneHeight = 18.dp
    val laneGap = 3.dp       // 行间距加大
    val overflowRowHeight = 16.dp  // 第 4 行：放每日“+N”
    Column(Modifier.fillMaxWidth().height(dayRowHeight + (laneHeight + laneGap) * capacity + overflowRowHeight + 2.dp)) {
        DayNumberRow(month, week, inMonth, today, onOpenDay)
        shownLanes.forEach { lane ->
            Box(Modifier.fillMaxWidth().height(laneHeight)) {
                DateLane(lane, week, 17, compact = true) { task, _ -> onOpen(task) }
            }
            Spacer(Modifier.height(laneGap))
        }
        // 第 4 行：每日“+N”，居中正对该日列，与任务条不重叠；点击进入当天日视图
        BoxWithConstraints(Modifier.fillMaxWidth().height(overflowRowHeight)) {
            val cell = maxWidth / 7
            (0..6).forEach { c ->
                if (hiddenPerDay[c] > 0 && inMonth[c]) {
                    Box(Modifier.offset(x = cell * c).width(cell), contentAlignment = Alignment.Center) {
                        Text("+${hiddenPerDay[c]}", Modifier.clickable { onOpenDay(week[c]) }, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Ink.copy(alpha = .6f), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayNumberRow(month: DemoDate, week: List<DemoDate>, inMonth: List<Boolean>, today: DemoDate, onOpenDay: (DemoDate) -> Unit) {
    Row(Modifier.fillMaxWidth().height(30.dp)) {
        week.forEachIndexed { col, date ->
            Box(Modifier.weight(1f).fillMaxHeight().clickable(enabled = inMonth[col]) { onOpenDay(date) }, contentAlignment = Alignment.Center) {
                if (inMonth[col]) Text("${date.day}", fontSize = 16.sp, lineHeight = 16.sp, fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal, color = Ink, modifier = Modifier.clip(CircleShape).background(if (date == today) Peach else Color.Transparent).padding(horizontal = 7.dp, vertical = 1.dp))
            }
        }
    }
}

@Composable private fun DateLane(lane: List<TaskSegment>, dates: List<DemoDate>, height: Int, compact: Boolean, onSelect: (TaskItem, DemoDate) -> Unit) {
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

// 任务颜色：优先服务端 displayColor（主标签颜色），其次主标签目录 colorHex；
// 无标签的任务使用中性灰（不同于任何标签色），标签已改为可选
private fun taskColor(task: TaskItem): Color =
    task.displayColorHex?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() }
        ?: task.tags.firstOrNull()?.let { tag -> tag.colorHex.takeIf { it.isNotBlank() }?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() } }
        ?: Ink.copy(alpha = 0.12f)
// 主标签已排在首位：保留色相生成深色版本，原色不透明时同样能区分。
private fun taskEdgeColor(task: TaskItem): Color {
    val rawColor = task.tags.firstOrNull()?.colorHex?.let { hex -> runCatching { android.graphics.Color.parseColor(hex) }.getOrNull() }
        ?: task.displayColorHex?.let { hex -> runCatching { android.graphics.Color.parseColor(hex) }.getOrNull() }
        ?: return Ink.copy(alpha = .55f)
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(rawColor, hsv)
    // 灰色保持中性；彩色适度提高饱和度并降低明度，不改变色相。
    if (hsv[1] > .02f) hsv[1] = (hsv[1] + .20f).coerceAtMost(1f)
    hsv[2] *= .70f
    return Color(android.graphics.Color.HSVToColor(hsv))
}
private fun Modifier.calendarGuides(): Modifier = drawBehind {
    val effect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
    for (i in 1..6) drawLine(Ink.copy(alpha = .06f), Offset(size.width * i / 7, 0f), Offset(size.width * i / 7, size.height), pathEffect = effect)
    drawLine(Ink.copy(alpha = .08f), Offset(0f, size.height), Offset(size.width, size.height), pathEffect = effect)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun TimeGrid(dates: List<DemoDate>, tasks: List<TaskItem>, dayMode: Boolean, today: DemoDate, businessTimezone: String?, onSelect: (TaskItem, DemoDate) -> Unit) {
    val hourHeight = if (dayMode) 60.dp else 56.dp
    val density = LocalDensity.current
    var selection by remember(dates) { mutableStateOf<Pair<DemoDate, Int>?>(null) }
    val selectAt by rememberUpdatedState({ date: DemoDate, minute: Int ->
        val candidates = timelineTasksAt(tasks, date, minute)
        if (candidates.size == 1) onSelect(candidates.single(), date)
        else if (candidates.size > 1) selection = date to minute
    })
    // 初始定位到当前时间上一小时半（按公司业务时区），不再从凌晨开始
    val nowMinute = businessNowMinute(businessTimezone)
    val initialHour = ((nowMinute - 90) / 60).coerceIn(0, 22)
    val scroll = rememberScrollState(with(density) { (hourHeight * initialHour).roundToPx() })
    val todayIndex = dates.indexOfFirst { it == today }
    Row(Modifier.fillMaxSize().padding(end = 8.dp).verticalScroll(scroll)) {
        Column(Modifier.width(40.dp)) {
            (0..23).forEach { hour -> Text("%02d".format(hour), Modifier.fillMaxWidth().height(hourHeight).padding(top = 2.dp), textAlign = TextAlign.Center, fontSize = 11.sp, color = Ink.copy(alpha = .5f)) }
            // 底部留白：避免悬浮“添加任务”按钮遮挡 24:00 附近内容
            Spacer(Modifier.height(72.dp))
        }
        BoxWithConstraints(Modifier.weight(1f).height(hourHeight * 24 + 72.dp).drawBehind {
            val dash = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
            for (day in 1 until dates.size) drawLine(Ink.copy(alpha = .07f), Offset(size.width * day / dates.size, 0f), Offset(size.width * day / dates.size, size.height), pathEffect = dash)
        }.drawWithContent {
            drawContent()
            // 整点实线覆盖任务色块，长任务内仍能辨认每小时边界。
            for (hour in 0..24) {
                val y = hourHeight.toPx() * hour
                drawLine(Ink.copy(alpha = .10f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            // “现在”指示线：今天的列内一条红线，随时间实时对位
            if (todayIndex >= 0) {
                val y = hourHeight.toPx() * nowMinute / 60f
                val left = size.width * todayIndex / dates.size
                drawLine(Color(0xFFE5484D), Offset(left, y), Offset(size.width * (todayIndex + 1) / dates.size, y), strokeWidth = 1.5.dp.toPx())
                drawCircle(Color(0xFFE5484D), radius = 3.dp.toPx(), center = Offset(left, y))
            }
        }) {
            val dayWidth = maxWidth / dates.size
            dates.forEachIndexed { index, date ->
                // 日视图并列分栏；周视图保留错位叠放。全天任务仍占据整日高度。
                val placements = if (dayMode) parallelTimedTasks(tasks, date) else staggerTimedTasks(tasks, date)
                val maxDepth = placements.maxOfOrNull { it.depth } ?: 0
                val indentStep = if (maxDepth == 0) 0.dp else minOf(6.dp, dayWidth * .35f / maxDepth)
                placements.forEach { (task, range, depth, columnCount) ->
                    val cardWidth = if (dayMode) dayWidth / columnCount else dayWidth - indentStep * depth
                    val indent = if (dayMode) cardWidth * depth else indentStep * depth
                    val edgeColor = taskEdgeColor(task)
                    // 跨天任务在当天裁剪显示，并标注起止来源
                    val spansFrom = date > task.start
                    val spansInto = date < task.end
                    // 半透明任务色必须有实色衬底，否则同刻开始的底层标题会透出。
                    Box(Modifier.offset(x = dayWidth * index + indent, y = hourHeight * (range.first / 60f)).width(cardWidth).height(hourHeight * ((range.last - range.first + 1) / 60f)).padding(1.dp).clip(RoundedCornerShape(4.dp)).background(WarmCard.copy(alpha = 1f)).background(taskColor(task))
                        .drawBehind {
                            val stroke = minOf(3.dp.toPx(), size.width)
                            drawLine(edgeColor, Offset(stroke / 2, 0f), Offset(stroke / 2, size.height), strokeWidth = stroke)
                        }
                        .semantics {
                            role = Role.Button
                            onClick("选择${task.title}") {
                                if (dayMode) onSelect(task, date) else selectAt(date, range.first)
                                true
                            }
                        }
                        .pointerInput(task.id, date, range, hourHeight, dayMode) {
                            detectTapGestures { position ->
                                if (dayMode) {
                                    onSelect(task, date)
                                    return@detectTapGestures
                                }
                                val minute = (range.first + ((position.y + 1.dp.toPx()) / hourHeight.toPx() * 60).toInt())
                                    .coerceIn(range.first, range.last)
                                selectAt(date, minute)
                            }
                        }) {
                        Column(Modifier.padding(start = 7.dp, end = 4.dp, top = 2.dp, bottom = 2.dp)) {
                            Text(task.title, fontSize = if (dayMode) 13.sp else 11.sp, lineHeight = if (dayMode) 16.sp else 13.sp, maxLines = if (dayMode) 2 else 1, overflow = TextOverflow.Ellipsis)
                            if (dayMode && !isAllDay(task) && range.last - range.first >= 30) Text("${if (spansFrom) "自昨日 " else ""}${minuteLabel(range.first)}—${minuteLabel(range.last + 1)}${if (spansInto) " →次日" else ""}", fontSize = 11.sp, color = Ink.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
    selection?.let { (date, minute) ->
        val candidates = timelineTasksAt(tasks, date, minute)
        LaunchedEffect(candidates.isEmpty()) { if (candidates.isEmpty()) selection = null }
        ModalBottomSheet(
            onDismissRequest = { selection = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = WarmCard
        ) {
            Text("选择任务", Modifier.padding(horizontal = 20.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("${date.year}年${date.month}月${date.day}日 · ${minuteLabel(minute)} · ${candidates.size} 项任务",
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp), fontSize = 13.sp, color = Ink.copy(alpha = .65f))
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 480.dp)) {
                items(candidates.size, key = { candidates[it].id }) { index ->
                    val candidate = candidates[index]
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable {
                        selection = null
                        onSelect(candidate, date)
                    }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(WarmCard.copy(alpha = 1f)).background(taskColor(candidate)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(candidate.title, fontSize = 16.sp, color = Ink)
                            Text(taskSelectionTimeLabel(candidate), Modifier.padding(top = 4.dp), fontSize = 12.sp, color = Ink.copy(alpha = .65f))
                        }
                    }
                    HorizontalDivider(color = Ink.copy(alpha = .08f))
                }
            }
            TextButton(onClick = { selection = null }, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("取消") }
        }
    }
}

@Composable private fun WeekTaskStacks(dates: List<DemoDate>, tasks: List<TaskItem>, onOpen: (TaskItem) -> Unit) {
    val allDayRows = weekAllDayRows(tasks, dates)
    val timedTasks = tasks.filterNot(::isAllDay)
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 80.dp)
    ) {
        allDayRows.forEach { lane ->
            DateLane(lane, dates, height = 28, compact = false) { task, _ -> onOpen(task) }
            Spacer(Modifier.height(4.dp))
        }
        if (allDayRows.isNotEmpty()) Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
        dates.forEach { date ->
            Column(Modifier.weight(1f).padding(horizontal = 2.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weekTasksOnDate(timedTasks, date).forEach { task ->
                    val edge = taskEdgeColor(task)
                    Column(
                        Modifier.fillMaxWidth().height(88.dp).clip(RoundedCornerShape(4.dp))
                            .background(WarmCard).background(taskColor(task))
                            .clickable(role = Role.Button, onClickLabel = "查看${task.title}") { onOpen(task) }
                            .drawBehind {
                                drawLine(edge, Offset(1.dp.toPx(), 0f), Offset(1.dp.toPx(), size.height), strokeWidth = 2.dp.toPx())
                            }.padding(start = 4.dp, end = 2.dp, top = 4.dp, bottom = 4.dp)
                    ) {
                        Text(task.title, Modifier.weight(1f), fontSize = 11.sp, lineHeight = 14.sp,
                            maxLines = 3, overflow = TextOverflow.Ellipsis, color = Ink)
                        val time = if (isAllDay(task)) "全天" else minuteLabel(task.startMinute!!)
                        Text(if (date > task.start) "续 $time" else time,
                            fontSize = 10.sp, lineHeight = 12.sp, maxLines = 1,
                            overflow = TextOverflow.Ellipsis, color = Ink.copy(alpha = .7f))
                    }
                }
            }
        }
        }
    }
}

private fun taskSelectionTimeLabel(task: TaskItem): String {
    fun dateLabel(date: DemoDate) = "${date.year}/${date.month}/${date.day}"
    val dates = if (task.start == task.end) dateLabel(task.start) else "${dateLabel(task.start)}—${dateLabel(task.end)}"
    return if (isAllDay(task)) "$dates · 全天"
        else if (task.start == task.end) "$dates · ${taskTimeLabel(task)}"
        else "${dateLabel(task.start)} ${minuteLabel(task.startMinute!!)}—${dateLabel(task.end)} ${minuteLabel(task.endMinute!!)}"
}

// 双指缩放：两指张开（间距由短到长）切到更小时间周期，两指收拢切到更大周期
// 在 Initial 通道提前截获：一旦出现多指就消费该手势全部事件，避免两指分别触发子项点击（如月视图两行周）与触摸反馈
private fun Modifier.pinchToZoom(mode: CalendarMode, onModeChange: (CalendarMode) -> Unit, onZoomedInCenterY: ((Float) -> Unit)? = null): Modifier = pointerInput(mode, onZoomedInCenterY) {
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
                        if (target != mode) {
                            onModeChange(target)
                            // 月视图放大到周视图：上报捏合中心的纵向位置，用于定位到手指所在周
                            if (mode == CalendarMode.MONTH && target == CalendarMode.WEEK) {
                                onZoomedInCenterY?.invoke((pressed[0].position.y + pressed[1].position.y) / 2f)
                            }
                        }
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
        if (mode == CalendarMode.YEAR || mode == CalendarMode.MONTH) return@pointerInput
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

// 年窗口与初始位置一起生效。显式导航由父级 key 重建；边缘重锚保留首月和像素偏移。
@Composable private fun YearGrid(
    cursor: DemoDate,
    tasks: List<TaskItem>,
    today: DemoDate,
    onYearChange: (Int) -> Unit,
    onSelectDate: (DemoDate) -> Unit,
    onSelectMonth: (DemoDate) -> Unit
) {
    var window by remember { mutableStateOf(YearGridWindow(cursor.year, cursor.month)) }
    val taskSnapshot = tasks.toList()
    val currentCursor by rememberUpdatedState(cursor)
    val reportYear by rememberUpdatedState(onYearChange)
    key(window) {
        val activeWindow = window
        // 不恢复其他窗口的滚动状态；当前 cursor 已由上层保存。
        val state = remember { LazyGridState(activeWindow.initialIndex, activeWindow.scrollOffset) }
        LaunchedEffect(state) {
            snapshotFlow { state.layoutInfo.visibleItemsInfo.firstOrNull()?.index }
                .collect { index ->
                    if (index == null) return@collect
                    val year = activeWindow.monthAt(index).year
                    if (year != currentCursor.year) reportYear(year)
                    window = activeWindow.recenteredAt(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
                }
        }
        LazyVerticalGrid(state = state, columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = 96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(activeWindow.itemCount, key = { monthKey(activeWindow.monthAt(it)) }) { index ->
                val month = activeWindow.monthAt(index)
                val weeks = remember(month) { monthWeeks(month) }
                val dayTaskColors = remember(month, taskSnapshot) {
                    val lastDay = month.plusMonths(1).plusDays(-1)
                    val monthTasks = taskSnapshot.filter { it.end >= month && it.start <= lastDay }
                    (1..lastDay.day).associateWith { day ->
                        val date = month.copy(day = day)
                        monthTasks.firstOrNull { date >= it.start && date <= it.end }?.let(::taskColor) ?: Color.Transparent
                    }
                }
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(WarmCard).border(1.dp, CreamDeep, RoundedCornerShape(10.dp)).padding(4.dp)) {
                    Text(if (month.month == 1) "${month.year}年1月" else "${month.month}月", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth().clickable { onSelectMonth(month) }.padding(horizontal = 2.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 1.dp)) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text(it, Modifier.weight(1f), fontSize = 8.sp, lineHeight = 8.sp, textAlign = TextAlign.Center, color = Ink.copy(alpha = .45f)) } }
                    weeks.forEach { week ->
                        Row(Modifier.fillMaxWidth()) {
                            week.forEach { date ->
                                val inMonth = date.month == month.month
                                val marker = if (inMonth) dayTaskColors.getValue(date.day) else Color.Transparent
                                Box(Modifier.weight(1f).height(24.dp).clickable { onSelectDate(date) }.drawBehind {
                                    if (date == today) drawCircle(Peach, radius = 10.dp.toPx(), center = Offset(size.width / 2, 10.dp.toPx()))
                                    if (marker != Color.Transparent) drawCircle(marker, radius = 2.dp.toPx(), center = Offset(size.width / 2, 22.dp.toPx()))
                                }, contentAlignment = Alignment.TopCenter) {
                                    Text("${date.day}", Modifier.height(20.dp).wrapContentHeight(Alignment.CenterVertically), fontSize = 9.sp, lineHeight = 9.sp, fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal, color = if (inMonth) Ink else Ink.copy(alpha = .25f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
