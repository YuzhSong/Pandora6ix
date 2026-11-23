package com.pandora6ix.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.data.DailyItem
import com.pandora6ix.app.data.DailyItemSchedule
import com.pandora6ix.app.data.DailyItemYears
import com.pandora6ix.app.data.schedule
import com.pandora6ix.app.data.withSchedule
import com.pandora6ix.app.ui.calendar.WheelColumn
import com.pandora6ix.app.ui.calendar.hiddenCalendarCounts
import com.pandora6ix.app.ui.calendar.packCalendarRows
import com.pandora6ix.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.launch

private val WeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
private val DailyWeekHeight = PandoraCalendarDimens.monthWeekHeight * 0.75f
private val DailyDayHeight = 24.dp
private val DailyPreviewHeight = 14.dp
private val DailyLaneGap = 1.dp

private data class ScheduledDailyItem(val item: DailyItem, val schedule: DailyItemSchedule)
private data class DailyBarKey(val itemId: String, val firstDate: LocalDate)
private data class DailyDrag(val entry: ScheduledDailyItem, val anchor: LocalDate, val position: Offset)

private fun dateAt(
    position: Offset,
    layout: LazyListLayoutInfo,
    months: List<YearMonth>,
    weekHeight: Int,
    layoutDirection: LayoutDirection
): LocalDate? {
    val (width, height) = layout.viewportSize
    if (width == 0 || weekHeight == 0 || position.x < 0 || position.x >= width ||
        position.y < 0 || position.y >= height
    ) return null
    val visible = layout.visibleItemsInfo.firstOrNull {
        position.y >= it.offset && position.y < it.offset + it.size
    } ?: return null
    val month = months[visible.index]
    val firstColumn = month.atDay(1).dayOfWeek.value - 1
    val week = ((position.y - visible.offset) / weekHeight).toInt()
    val columnFromLeft = (position.x * 7 / width).toInt()
    val column = if (layoutDirection == LayoutDirection.Ltr) columnFromLeft else 6 - columnFromLeft
    val day = week * 7 + column - firstColumn + 1
    return if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
}

@Composable
fun DailyItemsPanel(
    modifier: Modifier = Modifier,
    items: List<DailyItem>,
    onSave: (DailyItem) -> Unit,
    onToggle: (DailyItem) -> Unit,
    onDelete: (DailyItem) -> Unit
) {
    val colors = PandoraTheme.colors
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val today = LocalDate.now()
    val scope = rememberCoroutineScope()
    var selectedKey by rememberSaveable { mutableStateOf(today.toString()) }
    var openDayKey by rememberSaveable { mutableStateOf<String?>(null) }
    var openItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var showMonthWheel by remember { mutableStateOf(false) }
    val months = remember { DailyItemYears.flatMap { y -> (1..12).map { YearMonth.of(y, it) } } }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val selected = LocalDate.parse(selectedKey)
    val month = YearMonth.parse(monthKey)
    val listState = rememberLazyListState(months.indexOf(YearMonth.from(today)).coerceAtLeast(0))
    val weekHeight = with(density) { DailyWeekHeight.roundToPx() }
    val itemSnapshot = items.toList()
    val scheduled = remember(itemSnapshot) {
        itemSnapshot.mapNotNull { item -> item.schedule?.let { ScheduledDailyItem(item, it) } }
            .sortedWith(compareBy<ScheduledDailyItem> { it.schedule.start }
                .thenBy { it.schedule.startMinute ?: 0 }.thenBy { it.item.id })
    }
    val scheduledById by rememberUpdatedState(remember(scheduled) { scheduled.associateBy { it.item.id } })
    val save by rememberUpdatedState(onSave)
    val barCoordinates = remember { mutableMapOf<DailyBarKey, LayoutCoordinates>() }
    var viewportCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var drag by remember { mutableStateOf<DailyDrag?>(null) }
    val dragging by remember { derivedStateOf { drag != null } }
    val dropTarget by remember(listState, months, weekHeight, layoutDirection) {
        derivedStateOf {
            drag?.let { active ->
                dateAt(active.position, listState.layoutInfo, months, weekHeight, layoutDirection)?.takeIf {
                    active.entry.schedule.moved(active.anchor, it) != null
                }
            }
        }
    }

    fun scrollToDate(date: LocalDate) {
        val targetMonth = YearMonth.from(date)
        val index = months.indexOf(targetMonth)
        if (index < 0) return
        val week = (targetMonth.atDay(1).dayOfWeek.value - 1 + date.dayOfMonth - 1) / 7
        selectedKey = date.toString()
        scope.launch { listState.scrollToItem(index, week * weekHeight) }
    }

    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            layout.visibleItemsInfo.maxByOrNull {
                (minOf(it.offset + it.size, layout.viewportEndOffset) -
                    maxOf(it.offset, layout.viewportStartOffset)).coerceAtLeast(0)
            }?.index
        }.collect { index ->
            index?.let { months.getOrNull(it)?.let { visible -> monthKey = visible.toString() } }
        }
    }
    LaunchedEffect(dragging, listState, density) {
        if (!dragging) return@LaunchedEffect
        listState.scroll {
            while (drag != null) {
                withFrameNanos { }
                val point = drag?.position ?: break
                val (width, height) = listState.layoutInfo.viewportSize
                if (point.x !in 0f..width.toFloat() || point.y !in 0f..height.toFloat()) continue
                val edge = with(density) { 36.dp.toPx() }.coerceAtMost(height / 4f)
                if (edge <= 0f) continue
                val fraction = when {
                    point.y < edge -> (point.y - edge) / edge
                    point.y > height - edge -> (point.y - (height - edge)) / edge
                    else -> 0f
                }
                if (fraction != 0f) scrollBy(fraction * with(density) { 12.dp.toPx() })
            }
        }
    }

    PandoraPanel(
        modifier,
        contentPadding = PaddingValues(start = PandoraSpacing.space12, end = PandoraSpacing.space12,
            top = PandoraSpacing.space8, bottom = PandoraSpacing.space16)
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("每日事项", style = PandoraTypography.labelLarge, color = colors.textPrimary,
                modifier = Modifier.weight(1f).semantics { heading() }, maxLines = 1)
            Row(
                Modifier.clip(PandoraShapes.small).clickable { showMonthWheel = true }
                    .padding(horizontal = PandoraSpacing.space8, vertical = PandoraSpacing.space4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${month.year}年${month.monthValue}月", style = PandoraTypography.labelLarge, color = colors.textPrimary)
                Icon(Icons.Default.KeyboardArrowDown, "选择月份", Modifier.size(16.dp), tint = colors.textSecondary)
            }
            TextButton({ scrollToDate(today) }) { Text("今天", style = PandoraTypography.labelLarge) }
            IconButton({ creating = true }, Modifier.size(40.dp)) {
                Icon(Icons.Default.Add, "添加事项", tint = colors.primary)
            }
        }
        HorizontalDivider(Modifier.padding(top = PandoraSpacing.space4), color = colors.divider)
        Row(Modifier.fillMaxWidth().padding(top = PandoraSpacing.space4)) {
            WeekdayLabels.forEach {
                Text(it, Modifier.weight(1f), style = PandoraTypography.caption,
                    color = colors.textTertiary, textAlign = TextAlign.Center)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = PandoraSpacing.space12)
                .onGloballyPositioned { viewportCoordinates = it }
                .pointerInput(listState, weekHeight, layoutDirection) {
                    // Keep the gesture on the viewport so scrolling the source bar off screen cannot cancel it.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val viewport = viewportCoordinates?.takeIf { it.isAttached } ?: return@awaitEachGesture
                        val anchor = dateAt(down.position, listState.layoutInfo, months, weekHeight, layoutDirection)
                            ?: return@awaitEachGesture
                        val source = barCoordinates.entries.firstOrNull { (_, coordinates) ->
                            coordinates.isAttached && viewport.localBoundingBoxOf(coordinates).contains(down.position)
                        }?.key ?: return@awaitEachGesture
                        val entry = scheduledById[source.itemId]?.takeIf { anchor in it.schedule } ?: return@awaitEachGesture
                        val press = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                        drag = DailyDrag(entry, anchor, press.position)
                        try {
                            while (true) {
                                val change = awaitPointerEvent(PointerEventPass.Initial).changes
                                    .firstOrNull { it.id == press.id } ?: break
                                if (change.isConsumed) break
                                change.consume()
                                if (change.pressed) {
                                    drag = DailyDrag(entry, anchor, change.position)
                                } else {
                                    val target = dateAt(change.position, listState.layoutInfo, months, weekHeight, layoutDirection)
                                    val latest = scheduledById[entry.item.id]
                                    if (target != null && latest != null) {
                                        latest.schedule.moved(anchor, target)?.let { moved ->
                                            save(latest.item.withSchedule(moved))
                                            selectedKey = target.toString()
                                        }
                                    }
                                    break
                                }
                            }
                        } finally {
                            drag = null
                        }
                    }
                },
            contentPadding = PaddingValues(bottom = PandoraSpacing.space8)
        ) {
            items(count = months.size, key = { months[it].toString() }) { index ->
                MonthGrid(
                    month = months[index],
                    entries = scheduled,
                    selected = selected,
                    today = today,
                    dropTarget = dropTarget,
                    onOpenDay = { date -> selectedKey = date.toString(); openDayKey = date.toString() },
                    onOpenItem = { openItemId = it.id },
                    onToggle = onToggle,
                    barCoordinates = barCoordinates
                )
                if (index < months.lastIndex) {
                    DashedDivider(Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space8), colors.divider)
                }
            }
        }
    }

    if (showMonthWheel) {
        MonthWheelDialog(
            initial = month,
            onDismiss = { showMonthWheel = false },
            onConfirm = { target ->
                showMonthWheel = false
                scrollToDate(target.atDay(1))
            }
        )
    }
    val openItem = scheduledById[openItemId]?.item
    when {
        creating || (editing && openItem != null) -> {
            val existing = openItem.takeUnless { creating }
            DailyItemEditor(
                item = existing,
                initialDate = selected,
                onDismiss = { creating = false; editing = false },
                onSave = { content, schedule ->
                    val item = existing?.copy(content = content)
                        ?: DailyItem(UUID.randomUUID().toString(), schedule.start.toString(), content)
                    onSave(item.withSchedule(schedule))
                    if (creating) scrollToDate(schedule.start)
                    creating = false
                    editing = false
                }
            )
        }
        openItem != null -> {
            DailyItemDetails(openItem, onDismiss = { openItemId = null }, onEdit = { editing = true })
        }
        openDayKey != null -> {
            val date = LocalDate.parse(openDayKey)
            DailyDayDialog(
                date = date,
                items = scheduled.filter { date in it.schedule }.map { it.item },
                onDismiss = { openDayKey = null },
                onOpenItem = { openItemId = it.id },
                onToggle = onToggle,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    entries: List<ScheduledDailyItem>,
    selected: LocalDate,
    today: LocalDate,
    dropTarget: LocalDate?,
    onOpenDay: (LocalDate) -> Unit,
    onOpenItem: (DailyItem) -> Unit,
    onToggle: (DailyItem) -> Unit,
    barCoordinates: MutableMap<DailyBarKey, LayoutCoordinates>
) {
    val colors = PandoraTheme.colors
    val capacity = PandoraCalendarDimens.monthVisibleItemCount
    val firstDay = month.atDay(1)
    val lastDay = month.atEndOfMonth()
    val weeks = remember(month) {
        val start = firstDay.minusDays((firstDay.dayOfWeek.value - 1).toLong())
        val count = (firstDay.dayOfWeek.value - 1 + month.lengthOfMonth() + 6) / 7
        List(count) { week -> List(7) { day -> start.plusDays((week * 7 + day).toLong()) } }
    }
    val monthEntries = remember(month, entries) {
        entries.filter { it.schedule.start <= lastDay && it.schedule.lastCoveredDay >= firstDay }
    }
    Column {
        weeks.forEach { week ->
            val firstColumn = week.indexOfFirst { it >= firstDay }
            val lastColumn = week.indexOfLast { it <= lastDay }
            val lanes = remember(week, monthEntries) {
                packCalendarRows(monthEntries, week.subList(firstColumn, lastColumn + 1),
                    { it.schedule.start }, { it.schedule.lastCoveredDay })
            }
            val hidden = remember(lanes) { hiddenCalendarCounts(lanes, lastColumn - firstColumn + 1, capacity) }
            BoxWithConstraints(Modifier.fillMaxWidth().height(DailyWeekHeight)) {
                val cellWidth = maxWidth / 7
                for (column in firstColumn..lastColumn) {
                    val date = week[column]
                    if (date == dropTarget) {
                        Box(Modifier.offset(x = cellWidth * column).width(cellWidth).fillMaxHeight()
                            .border(1.dp, colors.primary, PandoraShapes.small)
                            .background(colors.primaryContainer.copy(alpha = 0.55f), PandoraShapes.small))
                    }
                    Box(
                        Modifier.offset(x = cellWidth * column).width(cellWidth).height(DailyDayHeight)
                            .clickable(onClickLabel = "查看${date.monthValue}月${date.dayOfMonth}日事项") { onOpenDay(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier.size(DailyDayHeight).clip(CircleShape)
                                .background(if (date == selected) colors.primaryContainer else Color.Transparent),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(date.dayOfMonth.toString(), style = PandoraTypography.bodyMedium,
                                color = if (date == today) colors.primary else colors.textPrimary,
                                fontWeight = if (date == today) FontWeight.Bold else null)
                        }
                    }
                    val hiddenCount = hidden[column - firstColumn]
                    if (hiddenCount > 0) {
                        Box(
                            Modifier.offset(x = cellWidth * column, y = DailyDayHeight + (DailyPreviewHeight + DailyLaneGap) * capacity)
                                .width(cellWidth).height(DailyPreviewHeight)
                                .clickable(onClickLabel = "查看${date.monthValue}月${date.dayOfMonth}日其余${hiddenCount}项事项") { onOpenDay(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("+$hiddenCount", style = PandoraTypography.calendarMicro, fontWeight = FontWeight.Medium,
                                color = colors.textSecondary, maxLines = 1)
                        }
                    }
                }
                lanes.take(capacity).forEachIndexed { lane, segments ->
                    segments.forEach { segment ->
                        val column = firstColumn + segment.first
                        val item = segment.item.item
                        val barKey = DailyBarKey(item.id, week[column])
                        key(barKey) {
                            DisposableEffect(barKey) { onDispose { barCoordinates.remove(barKey) } }
                            Row(
                                Modifier.offset(x = cellWidth * column, y = DailyDayHeight + (DailyPreviewHeight + DailyLaneGap) * lane)
                                    .width(cellWidth * (segment.last - segment.first + 1)).height(DailyPreviewHeight)
                                    .padding(horizontal = 1.dp).clip(RoundedCornerShape(PandoraCalendarDimens.monthBarRadius))
                                    .background(if (item.done) colors.divider else colors.primaryContainer)
                                    .onGloballyPositioned { barCoordinates[barKey] = it },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DailyItemCompletionCircle(
                                    item, onToggle, Modifier.width(14.dp).fillMaxHeight(), diameter = 9.dp
                                )
                                Box(
                                    Modifier.weight(1f).fillMaxHeight()
                                        .clickable(onClickLabel = "查看${item.content}") { onOpenItem(item) }
                                        .padding(end = PandoraSpacing.space2),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(item.content, style = PandoraTypography.calendarMicro,
                                        color = if (item.done) colors.textTertiary else colors.textPrimary,
                                        textDecoration = if (item.done) TextDecoration.LineThrough else null,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthWheelDialog(initial: YearMonth, onDismiss: () -> Unit, onConfirm: (YearMonth) -> Unit) {
    var year by remember { mutableIntStateOf(initial.year) }
    var month by remember { mutableIntStateOf(initial.monthValue) }
    val years = DailyItemYears.toList()
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择月份") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("年", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
                    WheelColumn(years.map { it.toString() }, year - years.first(), { year = years[it] })
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("月", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
                    WheelColumn((1..12).map { "%02d".format(it) }, month - 1, { month = it + 1 })
                }
            }
        },
        confirmButton = { PandoraDialogConfirm("确定", { onConfirm(YearMonth.of(year, month)) }) },
        dismissButton = { PandoraDialogDismiss(onClick = onDismiss) }
    )
}

@Composable
private fun DailyItemCompletionCircle(
    item: DailyItem,
    onToggle: (DailyItem) -> Unit,
    modifier: Modifier,
    diameter: Dp
) {
    val colors = PandoraTheme.colors
    Box(
        modifier.toggleable(value = item.done, role = Role.Checkbox, onValueChange = { onToggle(item) })
            .semantics { contentDescription = item.content },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.size(diameter).clip(CircleShape)
                .background(if (item.done) colors.primary else Color.Transparent)
                .border(1.dp, if (item.done) colors.primary else colors.textSecondary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (item.done) Icon(Icons.Default.Check, null, Modifier.fillMaxSize(0.75f), tint = colors.textOnPrimary)
        }
    }
}

@Composable
private fun DailyDayDialog(
    date: LocalDate,
    items: List<DailyItem>,
    onDismiss: () -> Unit,
    onOpenItem: (DailyItem) -> Unit,
    onToggle: (DailyItem) -> Unit,
    onDelete: (DailyItem) -> Unit
) {
    val colors = PandoraTheme.colors
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${date.monthValue}月${date.dayOfMonth}日 · 周${WeekdayLabels[date.dayOfWeek.value - 1]}") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space2)
            ) {
                if (items.isEmpty()) Text("当日暂无事项", color = colors.textTertiary)
                items.forEach { item ->
                    Row(Modifier.fillMaxWidth().heightIn(min = PandoraSpacing.space40),
                        verticalAlignment = Alignment.CenterVertically) {
                        DailyItemCompletionCircle(item, onToggle, Modifier.size(22.dp), diameter = 22.dp)
                        Text(
                            item.content,
                            modifier = Modifier.weight(1f).clickable(onClickLabel = "查看事项") { onOpenItem(item) }
                                .padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8),
                            style = PandoraTypography.bodyMedium,
                            color = if (item.done) colors.textTertiary else colors.textPrimary,
                            textDecoration = if (item.done) TextDecoration.LineThrough else null,
                            maxLines = 2, overflow = TextOverflow.Ellipsis
                        )
                        IconButton({ onDelete(item) }, Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, "删除事项", Modifier.size(16.dp), tint = colors.textTertiary)
                        }
                    }
                }
            }
        },
        confirmButton = { PandoraDialogConfirm("完成", onDismiss) }
    )
}

@Composable
private fun DashedDivider(modifier: Modifier, color: Color) {
    Canvas(modifier.height(1.dp)) {
        drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2),
            strokeWidth = size.height, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
    }
}
