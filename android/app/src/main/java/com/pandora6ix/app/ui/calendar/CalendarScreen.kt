package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.data.*

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
    // 年/月视图外部跳转信号（日期选择器选定、「回到今年」）：带年月精确定位；消费后置空避免与滚动上报互相干扰
    var gridJumpTarget by remember { mutableStateOf<DemoDate?>(null) }
    // 视图模式切换：从周视图切回月视图时，月卷轴回卷到 cursor 所在月
    val onChangeModeRecenter: (CalendarMode) -> Unit = { target ->
        if (mode == CalendarMode.WEEK && target == CalendarMode.MONTH) gridJumpTarget = cursor
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
                Text(when (mode) {
                    CalendarMode.YEAR -> "${cursor.year}年"
                    CalendarMode.MONTH -> cursor.monthLabel()
                    CalendarMode.WEEK -> "${weekStart.shortLabel()}—${dates.last().shortLabel()}"
                    CalendarMode.DAY -> cursor.shortLabelWithWeekday()
                }, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).clickable { showDatePick = true })
                IconButton({ showTagFilter = true }) {
                    Icon(Icons.Default.Search, "按标签筛选任务", tint = if (filterTags.isEmpty()) Ink else WarmOrange)
                }
            }
            if (showDatePick) DateWheelDialog(
                initial = cursor,
                onDismiss = { showDatePick = false },
                onConfirm = { d -> showDatePick = false; gridJumpTarget = d; onCursorChange(d) }
            )
            // 年视图任务范围：GET /tasks?scope=mine|managed|created（服务端校验可见范围）；"我管理的"仅对有审批能力的用户显示
            if (mode == CalendarMode.YEAR) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        color = if (selected) Ink else Ink.copy(alpha = .65f)
                    )
                }
            }
            if (notice != null) Text(notice, Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), fontSize = 12.sp, color = Ink.copy(alpha = .6f))
            // 年视图圆点图例：圆点表示当天有任务，颜色为该任务主标签色
            if (mode == CalendarMode.YEAR) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(Peach))
                Spacer(Modifier.width(4.dp))
                Text("当天有任务；圆点颜色为任务主标签颜色，点击日期查看详情", fontSize = 11.sp, color = Ink.copy(alpha = .5f))
            }
            if (filterTags.isNotEmpty()) Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("仅显示：", fontSize = 12.sp, color = Ink.copy(alpha = .6f))
                filterTags.forEach { name ->
                    val chipBg = availableTags.firstOrNull { it.name == name }?.let { tagHexColor(it.colorHex).copy(alpha = .4f) } ?: Ink.copy(alpha = .15f)
                    Text(name, Modifier.clip(RoundedCornerShape(10.dp)).background(chipBg).padding(horizontal = 8.dp, vertical = 2.dp), fontSize = 12.sp, color = Ink)
                }
                TextButton({ filterTags = emptyList() }, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("清除", fontSize = 12.sp, color = WarmOrange) }
            }
            // 应用标签筛选后的任务集合：空筛选 = 全部任务
            val visibleTasks = remember(tasks, filterTags) { if (filterTags.isEmpty()) tasks else tasks.filter { task -> filterTags.all { name -> task.tags.any { it.name == name } } } }
            val density = LocalDensity.current
            // 月视图卷轴：索引与滚动状态上提，供捏合缩放（月→周按手势所在行定位）使用
            val monthScrollIndex = remember(today.year) { buildMonthScrollIndex(today.year - MonthScrollSpanYears, today.year + MonthScrollSpanYears) }
            val monthScrollState = rememberLazyListState(initialFirstVisibleItemIndex = monthScrollIndex.monthItemIndex[monthKey(cursor)] ?: 0)
            // 月→周捏合放大：按捏合中心的纵向位置定位到手指所在的那一周
            val onPinchZoomInCenter: (Float) -> Unit = { centerY ->
                val y = centerY - with(density) { 20.dp.toPx() }  // 扣除卷轴上方的星期表头高度
                val info = monthScrollState.layoutInfo.visibleItemsInfo.firstOrNull { y >= it.offset && y < it.offset + it.size }
                val row = info?.let { monthScrollIndex.items.getOrNull(it.index) }
                if (row != null && !row.isTitle) onCursorChange(row.week.first())
            }
            val yearGridState = rememberLazyGridState(initialFirstVisibleItemIndex = YearGridAnchorIndex)
            Box(Modifier.fillMaxWidth().weight(1f).pinchToZoom(mode, onChangeModeRecenter, onPinchZoomInCenter).swipeNavigate(mode) { forward ->
                onCursorChange(when (mode) {
                    CalendarMode.MONTH -> cursor.plusMonths(if (forward) 1 else -1)
                    CalendarMode.WEEK -> cursor.plusDays(if (forward) 7 else -7)
                    CalendarMode.DAY -> cursor.plusDays(if (forward) 1 else -1)
                    else -> cursor
                })
            }) {
                when (mode) {
                    CalendarMode.YEAR -> YearGrid(cursor, visibleTasks, today, yearGridState, jumpTarget = gridJumpTarget, onJumpConsumed = { gridJumpTarget = null }, onYearChange = { onCursorChange(cursor.plusYears(it - cursor.year)) }, onSelectDate = onCursorChange, onSelectMonth = { onCursorChange(it); onModeChange(CalendarMode.MONTH) })
                    CalendarMode.MONTH -> MonthScroll(cursor, visibleTasks, today, monthScrollIndex, monthScrollState, jumpTarget = gridJumpTarget, onJumpConsumed = { gridJumpTarget = null }, onCursorChange = onCursorChange, onOpen = onOpen, onOpenDay = { date -> onCursorChange(date); onModeChange(CalendarMode.DAY) })
                    CalendarMode.WEEK, CalendarMode.DAY -> Column(Modifier.fillMaxSize()) {
                        val visibleDates = if (mode == CalendarMode.DAY) listOf(cursor) else dates
                        WeekDates(visibleDates, today) { date ->
                            onCursorChange(date)
                            if (mode == CalendarMode.WEEK) onModeChange(CalendarMode.DAY)
                        }
                        AllDayTasks(visibleDates, visibleTasks, onSelect = { task, date -> onCursorChange(date); onOpen(task) })
                        HorizontalDivider(color = Ink.copy(alpha = .08f))
                        key(mode, if (mode == CalendarMode.DAY) cursor else weekStart) {
                            TimeGrid(visibleDates, visibleTasks, mode == CalendarMode.DAY, today, businessTimezone, onSelect = { task, _ -> onOpen(task) })
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
        if (!atToday) Button(onClick = { gridJumpTarget = today; onCursorChange(today) }, modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(.25f).height(34.dp), shape = RoundedCornerShape(topStart = 17.dp, bottomStart = 17.dp), contentPadding = PaddingValues(horizontal = 4.dp), colors = ButtonDefaults.buttonColors(containerColor = ThemeButton, contentColor = OnThemeButton)) { Text("回到今年", fontSize = 12.sp, maxLines = 1) }
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

// ===== 月视图卷轴：连续纵向滚动；每个月独立按星期列排布（上月尾不挤入下月首行），任务条裁剪在本月列内 =====
private const val MonthScrollSpanYears = 5  // 以今天为基准前后各 5 年静态构建，无需重锚

/** 卷轴内容项：月份大标题或该月的某一行（week 为含邻月补位的 7 列基准，渲染时只显示本月日） */
private class MonthScrollItem(val month: DemoDate, val isTitle: Boolean, val week: List<DemoDate>)

/** 卷轴索引：全部内容项与「月份 → 项下标」映射 */
private class MonthScrollIndex(val items: List<MonthScrollItem>, val monthItemIndex: Map<Long, Int>)

private fun monthKey(d: DemoDate) = d.year * 12L + (d.month - 1)

private fun buildMonthScrollIndex(startYear: Int, endYear: Int): MonthScrollIndex {
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
    // 滚动上报：首个可见项所属月份写回 cursor（按月平移，日期不变），驱动顶部标题与「回到今年」按钮
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }
            .collect { idx ->
                val item = index.items.getOrNull(idx) ?: return@collect
                if (monthKey(item.month) != monthKey(currentCursor)) onCursorChange(currentCursor.plusMonths((monthKey(item.month) - monthKey(currentCursor)).toInt()))
            }
    }
    // 外部跳转（日期选择器/回到今年）：定位到目标月标题并置顶
    LaunchedEffect(jumpTarget) {
        val target = jumpTarget ?: return@LaunchedEffect
        index.monthItemIndex[monthKey(target)]?.let { state.scrollToItem(it) }
        onJumpConsumed()
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp)) {
        Row(Modifier.fillMaxWidth().height(20.dp)) { listOf("一", "二", "三", "四", "五", "六", "日").forEach { Text("周$it", Modifier.weight(1f), fontSize = 11.sp, textAlign = TextAlign.Center, color = Ink.copy(alpha = .6f)) } }
        LazyColumn(state = state, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
            items(index.items.size) { i ->
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
    val lanes = packTaskRows(tasks, week).mapNotNull { lane ->
        lane.mapNotNull { seg ->
            val f = maxOf(seg.first, firstCol); val l = minOf(seg.last, lastCol)
            if (f <= l) TaskSegment(seg.task, f, l) else null
        }
    }.filter { it.isNotEmpty() }
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
            BoxWithConstraints(Modifier.fillMaxWidth().height(laneHeight)) {
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

@Composable private fun AllDayTasks(dates: List<DemoDate>, tasks: List<TaskItem>, onSelect: (TaskItem, DemoDate) -> Unit) {
    val lanes = packTaskRows(tasks.filter(::isAllDay), dates)
    Row(Modifier.fillMaxWidth().padding(end = 8.dp, bottom = 6.dp)) {
        Text("全天", Modifier.width(40.dp).padding(top = 4.dp), fontSize = 12.sp, textAlign = TextAlign.Center, color = Ink.copy(alpha = .7f))
        Column(Modifier.weight(1f).heightIn(max = 150.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            if (lanes.isEmpty()) Text("暂无全天任务", Modifier.padding(vertical = 7.dp), fontSize = 12.sp, color = Ink.copy(alpha = .45f))
            lanes.forEach { DateLane(it, dates, 25, compact = false, onSelect = onSelect) }
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

// 任务颜色：优先服务端 displayColor（主标签颜色），无则用主标签目录 colorHex，最后回退既有色板
private fun taskColor(task: TaskItem): Color =
    task.displayColorHex?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() }
        ?: task.tags.firstOrNull()?.let { tag -> tag.colorHex.takeIf { it.isNotBlank() }?.let { hex -> runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrNull() } }
        ?: listOf(Peach, Sky, Mint, Lilac)[task.priorityLevel.mod(4)]
private fun Modifier.calendarGuides(): Modifier = drawBehind {
    val effect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 4.dp.toPx()))
    for (i in 1..6) drawLine(Ink.copy(alpha = .06f), Offset(size.width * i / 7, 0f), Offset(size.width * i / 7, size.height), pathEffect = effect)
    drawLine(Ink.copy(alpha = .08f), Offset(0f, size.height), Offset(size.width, size.height), pathEffect = effect)
}

@Composable private fun TimeGrid(dates: List<DemoDate>, tasks: List<TaskItem>, dayMode: Boolean, today: DemoDate, businessTimezone: String?, onSelect: (TaskItem, DemoDate) -> Unit) {
    val hourHeight = if (dayMode) 60.dp else 56.dp
    val density = LocalDensity.current
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
            for (hour in 0..24) drawLine(Ink.copy(alpha = .08f), Offset(0f, hourHeight.toPx() * hour), Offset(size.width, hourHeight.toPx() * hour), pathEffect = dash)
            for (day in 1 until dates.size) drawLine(Ink.copy(alpha = .07f), Offset(size.width * day / dates.size, 0f), Offset(size.width * day / dates.size, size.height), pathEffect = dash)
        }.drawWithContent {
            drawContent()
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
                val entries = tasks.mapNotNull { task -> timeRangeOnDate(task, date)?.let { task to it } }.filter { !it.second.isEmpty() }.sortedBy { it.second.first }
                // 时间重叠的任务直接上下堆叠（后绘制的覆盖先绘制的），各占整列宽度，随时间轴垂直排布
                entries.forEach { (task, range) ->
                    // 跨天任务在当天裁剪显示，并标注起止来源
                    val spansFrom = date > task.start
                    val spansInto = date < task.end
                    Box(Modifier.offset(x = dayWidth * index, y = hourHeight * (range.first / 60f)).width(dayWidth).height(hourHeight * ((range.last - range.first + 1) / 60f)).padding(1.dp).clip(RoundedCornerShape(4.dp)).background(taskColor(task)).clickable { onSelect(task, date) }) {
                        Column(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Text(task.title, fontSize = if (dayMode) 13.sp else 11.sp, lineHeight = if (dayMode) 16.sp else 13.sp, maxLines = if (dayMode) 2 else 1, overflow = TextOverflow.Ellipsis)
                            if (dayMode && range.last - range.first >= 30) Text("${if (spansFrom) "自昨日 " else ""}${minuteLabel(range.first)}—${minuteLabel(range.last + 1)}${if (spansInto) " →次日" else ""}", fontSize = 11.sp, color = Ink.copy(alpha = .7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
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

// 年视图：跨多年连续滚动的月份网格（锚点前后各 YearGridSpanYears 年），拖动跟手、跨年自然衔接，不再分片跳年
// 顶部年份随滚动实时上报（驱动日期行标题）；滚动接近列表边缘时静默重锚并保持视觉位置，近似无限滚动
// 外部跳转（日期选择器选定）定位到目标年月对应格并置顶
private const val YearGridSpanYears = 12
private const val YearGridAnchorIndex = YearGridSpanYears * 12

@Composable private fun YearGrid(
    cursor: DemoDate,
    tasks: List<TaskItem>,
    today: DemoDate,
    state: LazyGridState,
    jumpTarget: DemoDate?,
    onJumpConsumed: () -> Unit,
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
    // 外部跳转（日期选择器选定）：定位到目标年月对应格并置顶；与滚动上报互不干扰（jumpTarget 消费后即清空）
    LaunchedEffect(jumpTarget) {
        val target = jumpTarget ?: return@LaunchedEffect
        val targetIndex = (target.year - startYear) * 12 + (target.month - 1)
        lastReportedYear = target.year
        if (targetIndex in 0 until YearGridSpanYears * 2 * 12) {
            state.scrollToItem(targetIndex)
        } else {
            anchorYear = target.year
            state.scrollToItem(YearGridAnchorIndex + (target.month - 1))
        }
        onJumpConsumed()
    }
    LazyVerticalGrid(state = state, columns = GridCells.Fixed(2), modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = 96.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
