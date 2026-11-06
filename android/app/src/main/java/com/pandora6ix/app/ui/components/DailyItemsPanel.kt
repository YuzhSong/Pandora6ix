package com.pandora6ix.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.data.DailyItem
import com.pandora6ix.app.ui.calendar.WheelColumn
import com.pandora6ix.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch

private val WeekdayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * 首页「每日事项」面板：月视图日历样式（周一开头网格）。
 * - 月历为连续滚动的月份列表（与公司页月视图一致）：上下滑动连贯浏览各月，标题年月随滚动位置实时更新
 * - 点击年月标题弹出「年 / 月」滚轮（复用公司页 WheelColumn，无日列）
 * - 单击日期：打开该日事项弹窗（勾选 / 删除 / 左下角「添加」展开输入行 / 右下角「完成」），所有调整即时生效
 * 数据由调用方持有并持久化；本组件只负责展示与回调。
 */
@Composable
fun DailyItemsPanel(
    modifier: Modifier = Modifier,
    items: List<DailyItem>,
    onAdd: (date: LocalDate, content: String) -> Unit,
    onToggle: (DailyItem) -> Unit,
    onDelete: (DailyItem) -> Unit
) {
    val colors = PandoraTheme.colors
    val today = LocalDate.now()
    val scope = rememberCoroutineScope()
    var selectedKey by rememberSaveable { mutableStateOf(today.toString()) }
    var showMonthWheel by remember { mutableStateOf(false) }
    // 当前打开的日期弹窗
    var openDayKey by rememberSaveable { mutableStateOf<String?>(null) }
    // 连续月份列表：滚动位置即当前年月（1990-2100）
    val months = remember { (1990..2100).flatMap { y -> (1..12).map { YearMonth.of(y, it) } } }
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.from(today).toString()) }
    val selectedDate = runCatching { LocalDate.parse(selectedKey) }.getOrDefault(today)
    val month = runCatching { YearMonth.parse(monthKey) }.getOrDefault(YearMonth.from(today))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = months.indexOf(YearMonth.from(today)).coerceAtLeast(0))
    // 滚动位置驱动标题年月：取视口内可见面积最大的月份——滚过某月一半以上标题才切换（两个方向对称）
    LaunchedEffect(listState) {
        snapshotFlow {
            val layout = listState.layoutInfo
            val top = layout.viewportStartOffset
            val bottom = layout.viewportEndOffset
            layout.visibleItemsInfo.maxByOrNull {
                (minOf(it.offset + it.size, bottom) - maxOf(it.offset, top)).coerceAtLeast(0)
            }?.index
        }.collect { index ->
            index?.let { i -> months.getOrNull(i)?.let { m -> if (m.toString() != monthKey) monthKey = m.toString() } }
        }
    }

    PandoraPanel(modifier, contentPadding = PaddingValues(start = PandoraSpacing.space12, end = PandoraSpacing.space12, top = PandoraSpacing.space8, bottom = PandoraSpacing.space16)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("每日事项", style = PandoraTypography.labelLarge, color = colors.textPrimary, modifier = Modifier.weight(1f).semantics { heading() })
            // 点击年月弹出滚轮（借鉴公司页日期选择，只有年与月两列）；月份也可连贯上下滑动浏览
            Row(
                Modifier.clip(PandoraShapes.small).clickable { showMonthWheel = true }
                    .padding(horizontal = PandoraSpacing.space8, vertical = PandoraSpacing.space4),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${month.year}年${month.monthValue}月", style = PandoraTypography.labelLarge, color = colors.textPrimary, textAlign = TextAlign.Center)
                Icon(Icons.Default.KeyboardArrowDown, "选择月份", Modifier.size(16.dp), tint = colors.textSecondary)
            }
            TextButton({
                scope.launch {
                    listState.scrollToItem(months.indexOf(YearMonth.from(today)).coerceAtLeast(0))
                }
                selectedKey = today.toString()
            }) { Text("今天", style = PandoraTypography.labelLarge) }
        }
        HorizontalDivider(Modifier.padding(top = PandoraSpacing.space4), color = colors.divider)
        Row(Modifier.fillMaxWidth().padding(top = PandoraSpacing.space4)) {
            WeekdayLabels.forEach {
                Text(it, Modifier.weight(1f), style = PandoraTypography.caption, color = colors.textTertiary, textAlign = TextAlign.Center)
            }
        }
        // 星期表头与日期之间留出呼吸空间
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = PandoraSpacing.space12),
            contentPadding = PaddingValues(bottom = PandoraSpacing.space8)
        ) {
            items(count = months.size, key = { months[it].toString() }) { index ->
                MonthGrid(
                    month = months[index],
                    items = items,
                    selectedDate = selectedDate,
                    today = today,
                    onOpenDay = { key -> selectedKey = key; openDayKey = key }
                )
                // 月与月之间的分隔虚线（最后一个月不加），避免月末恰为周日时两月贴在一起
                if (index < months.lastIndex) {
                    DashedDivider(
                        Modifier.fillMaxWidth().padding(vertical = PandoraSpacing.space8),
                        color = colors.divider
                    )
                }
            }
        }
    }
    if (showMonthWheel) {
        MonthWheelDialog(
            initial = month,
            onDismiss = { showMonthWheel = false },
            onConfirm = { ym ->
                showMonthWheel = false
                // 立即定位到所选月份，不做平滑滚动动画
                months.indexOf(ym).takeIf { it >= 0 }?.let { index -> scope.launch { listState.scrollToItem(index) } }
            }
        )
    }
    val openDay = openDayKey?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
    if (openDay != null) {
        DailyDayDialog(
            date = openDay,
            items = items,
            onDismiss = { openDayKey = null },
            onAdd = { content -> onAdd(openDay, content) },
            onToggle = onToggle,
            onDelete = onDelete
        )
    }
}

/** 单月网格：与公司月视图共用固定周行尺寸，每天三条预览及独立的溢出行。 */
@Composable
private fun MonthGrid(
    month: YearMonth,
    items: List<DailyItem>,
    selectedDate: LocalDate,
    today: LocalDate,
    onOpenDay: (String) -> Unit
) {
    val colors = PandoraTheme.colors
    val leading = month.atDay(1).dayOfWeek.value - 1
    val cells = leading + month.lengthOfMonth()
    val weeks = (cells + 6) / 7
    val itemsByDate = items.groupBy { it.date }
    Column {
        repeat(weeks) { w ->
            Row(Modifier.fillMaxWidth().height(PandoraCalendarDimens.monthWeekHeight)) {
                repeat(7) { d ->
                    val dayNumber = w * 7 + d - leading + 1
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        if (dayNumber in 1..month.lengthOfMonth()) {
                            val date = month.atDay(dayNumber)
                            val key = date.toString()
                            val dateItems = itemsByDate[key].orEmpty()
                            val capacity = PandoraCalendarDimens.monthVisibleItemCount
                            val hiddenCount = (dateItems.size - capacity).coerceAtLeast(0)
                            val selected = date == selectedDate
                            val isToday = date == today
                            Column(
                                Modifier.fillMaxSize().clickable(onClickLabel = "查看${date.monthValue}月${dayNumber}日事项") { onOpenDay(key) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier.size(PandoraCalendarDimens.monthDayRowHeight).clip(CircleShape)
                                        .background(if (selected) colors.primaryContainer else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "$dayNumber",
                                        style = PandoraTypography.bodyMedium,
                                        color = if (isToday) colors.primary else colors.textPrimary,
                                        fontWeight = if (isToday) FontWeight.Bold else null
                                    )
                                }
                                repeat(capacity) { lane ->
                                    Box(
                                        Modifier.fillMaxWidth().height(PandoraCalendarDimens.monthBarMinHeight)
                                            .padding(horizontal = 1.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        dateItems.getOrNull(lane)?.let { item ->
                                            Box(
                                                Modifier.fillMaxSize().clip(RoundedCornerShape(PandoraCalendarDimens.monthBarRadius))
                                                    .background(if (item.done) colors.divider else colors.primaryContainer),
                                                contentAlignment = Alignment.CenterStart
                                            ) {
                                                Text(
                                                    item.content,
                                                    Modifier.padding(horizontal = PandoraSpacing.space4),
                                                    style = PandoraTypography.calendarMicro,
                                                    color = if (item.done) colors.textTertiary else colors.textPrimary,
                                                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(PandoraCalendarDimens.monthLaneGap))
                                }
                                Box(
                                    Modifier.fillMaxWidth().height(PandoraCalendarDimens.monthOverflowRowHeight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (hiddenCount > 0) {
                                        Text(
                                            "+$hiddenCount",
                                            Modifier.clickable(onClickLabel = "查看${date.monthValue}月${dayNumber}日其余${hiddenCount}项事项") { onOpenDay(key) },
                                            style = PandoraTypography.calendarMicro,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textSecondary,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** 年 / 月两列滚轮：复用公司页日期选择的 WheelColumn，去掉日列 */
@Composable
private fun MonthWheelDialog(initial: YearMonth, onDismiss: () -> Unit, onConfirm: (YearMonth) -> Unit) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    val years = (1900..2100).toList()
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择月份") },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("年", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
                    WheelColumn(years.map { it.toString() }, (year - years.first()).coerceIn(0, years.size - 1), { year = years[it] })
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("月", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
                    WheelColumn((1..12).map { "%02d".format(it) }, month - 1, { month = it + 1 })
                }
            }
        },
        confirmButton = { PandoraDialogConfirm("确定", { onConfirm(YearMonth.of(year, month)) }) },
        dismissButton = { PandoraDialogDismiss("取消", onDismiss) }
    )
}

/**
 * 单日事项弹窗：显示该日全部事项（勾选完成 / 删除）；收起态为左下「添加」/ 右下「完成」，
 * 点左下「添加」展开占满一行的输入框，右下按钮变为「添加」提交、左下变「取消」收起并恢复「完成」。
 * 清单直接读 items（不再 remember 缓存），勾选 / 删除 / 新增全部即时反映。
 */
@Composable
private fun DailyDayDialog(
    date: LocalDate,
    items: List<DailyItem>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onToggle: (DailyItem) -> Unit,
    onDelete: (DailyItem) -> Unit
) {
    val colors = PandoraTheme.colors
    // 直接过滤（SnapshotStateList 的读取会被快照观察）：任何增删改立即刷新本清单与月历预览
    val dayItems = items.filter { it.date == date.toString() }
    var showInput by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    fun add() {
        val text = input.trim()
        if (text.isNotEmpty()) { onAdd(text); input = "" }
    }
    LaunchedEffect(showInput) { if (showInput) focusRequester.requestFocus() }
    PandoraDialog(onDismissRequest = onDismiss, contentPadding = PaddingValues(horizontal = PandoraSpacing.space24, vertical = PandoraSpacing.space16)) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Text(
                "${date.monthValue}月${date.dayOfMonth}日 · 周${WeekdayLabels[date.dayOfWeek.value - 1]}",
                style = PandoraTypography.titleMedium,
                color = colors.textPrimary
            )
            HorizontalDivider(Modifier.padding(top = PandoraSpacing.space12, bottom = PandoraSpacing.space8), color = colors.divider)
            Column(
                Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space2)
            ) {
                if (dayItems.isEmpty()) {
                    Text(
                        "当日暂无事项",
                        style = PandoraTypography.bodyMedium,
                        color = colors.textTertiary,
                        modifier = Modifier.padding(vertical = PandoraSpacing.space8)
                    )
                } else {
                    dayItems.forEach { item ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = PandoraSpacing.space40).padding(vertical = PandoraSpacing.space2),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier.size(22.dp).clip(CircleShape)
                                    .background(if (item.done) colors.primary else Color.Transparent)
                                    .border(1.5.dp, if (item.done) colors.primary else colors.border, CircleShape)
                                    .clickable { onToggle(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.done) Icon(Icons.Default.Check, "标记未完成", Modifier.size(14.dp), tint = colors.textOnPrimary)
                            }
                            Spacer(Modifier.width(PandoraSpacing.space12))
                            Text(
                                item.content,
                                style = PandoraTypography.bodyMedium,
                                color = if (item.done) colors.textTertiary else colors.textPrimary,
                                textDecoration = if (item.done) TextDecoration.LineThrough else null,
                                maxLines = 2,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton({ onDelete(item) }, Modifier.size(32.dp)) { Icon(Icons.Default.Close, "删除事项", Modifier.size(16.dp), tint = colors.textTertiary) }
                        }
                    }
                }
            }
            if (showInput) {
                HorizontalDivider(Modifier.padding(top = PandoraSpacing.space16), color = colors.divider)
                // 输入框与分割线之间留足间距，不再紧贴；输入框占满一整行
                Spacer(Modifier.height(PandoraSpacing.space12))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    placeholder = { Text("添加 ${date.monthValue}月${date.dayOfMonth}日 的事项…", style = PandoraTypography.bodyMedium) },
                    singleLine = true,
                    textStyle = PandoraTypography.bodyMedium,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { add() })
                )
            }
            // 展开输入行时：左下「取消」收起并清空输入、右下「添加」提交；收起时：左下「添加」、右下「完成」
            Row(Modifier.fillMaxWidth().padding(top = PandoraSpacing.space8), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (!showInput) {
                    TextButton({ showInput = true }) { Text("添加", style = PandoraTypography.labelLarge, color = colors.primary) }
                } else {
                    TextButton({ showInput = false; input = "" }) { Text("收起", style = PandoraTypography.labelLarge, color = colors.textSecondary) }
                }
                if (showInput) {
                    TextButton({ add() }, enabled = input.isNotBlank()) { Text("添加", style = PandoraTypography.labelLarge, color = colors.primary) }
                } else {
                    TextButton(onDismiss) { Text("完成", style = PandoraTypography.labelLarge, color = colors.primary) }
                }
            }
        }
    }
}

/** 分隔虚线：Compose 无内建虚线分割线，用 Canvas 自绘 */
@Composable
private fun DashedDivider(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier.height(1.dp)) {
        drawLine(
            color,
            Offset(0f, size.height / 2),
            Offset(size.width, size.height / 2),
            strokeWidth = size.height,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))
        )
    }
}
