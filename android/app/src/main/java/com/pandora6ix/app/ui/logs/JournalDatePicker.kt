package com.pandora6ix.app.ui.logs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pandora6ix.app.data.parseIsoDate
import com.pandora6ix.app.mock.DemoDate
import com.pandora6ix.app.ui.calendar.WheelColumn
import com.pandora6ix.app.ui.theme.Ink
import com.pandora6ix.app.ui.theme.WarmCard
import com.pandora6ix.app.ui.theme.WarmDashboard
import com.pandora6ix.app.ui.theme.WarmOrange
import kotlinx.coroutines.launch
import java.util.Calendar

/** 今天：固定语义绿，不随主题变化 */
private val TodayGreen = Color(0xFF2E7D32)
/** 无日志日期：灰 */
private val EmptyDay = Color(0xFF9E9E9E)
private const val WEEK_HEADER = "日 一 二 三 四 五 六"

private fun daysInMonth(year: Int, month: Int): Int =
    Calendar.getInstance().apply { set(year, month - 1, 1) }
        .getActualMaximum(Calendar.DAY_OF_MONTH)

/** 1 号是周几：0=周日 */
private fun firstWeekday(year: Int, month: Int): Int =
    Calendar.getInstance().apply { set(year, month - 1, 1) }.get(Calendar.DAY_OF_WEEK) - 1

/** 按 7 列排好的日期格（null 为占位），整月一次算完避免组合期反复计算 */
private fun monthCells(year: Int, month: Int): List<List<Int?>> {
    val days = daysInMonth(year, month)
    val lead = firstWeekday(year, month)
    val cells = ArrayList<Int?>(42)
    repeat(lead) { cells.add(null) }
    for (day in 1..days) cells.add(day)
    while (cells.size % 7 != 0) cells.add(null)
    return cells.chunked(7)
}

/**
 * 日志日期选择器（微信「查找聊天记录」式）：
 * - 全屏上下滚动，从最早有日志的月份连续排到当前月份，中间没有日志的月份也显示（日期连续）
 * - 有日志的日期黑色、没有日志的灰色、今天绿色
 * - 点左上角年月可展开年/月选择并跳转到该月
 */
@Composable
fun JournalDatePickerDialog(
    loggedDates: Set<String>,
    today: String,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    val todayDate = parseIsoDate(today) ?: return
    // 连续月份：最早有日志的月份（无日志则为今天所在月）→ 本月
    val earliest = remember(loggedDates) {
        loggedDates.mapNotNull(::parseIsoDate).minOrNull() ?: todayDate
    }
    val months = remember(earliest.year, earliest.month, todayDate.year, todayDate.month) {
        buildList {
            var cursor = DemoDate(earliest.year, earliest.month, 1)
            val last = DemoDate(todayDate.year, todayDate.month, 1)
            while (cursor <= last && size < 600) {
                add(cursor)
                cursor = cursor.plusMonths(1)
            }
        }
    }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var panelOpen by remember { mutableStateOf(false) }
    val focused by remember {
        derivedStateOf { months[listState.firstVisibleItemIndex.coerceIn(0, months.lastIndex)] }
    }
    fun jumpTo(year: Int, month: Int) {
        // 该年没有所选月份（如当年还没到 12 月）时退到该年第一个月
        val index = months.indexOfFirst { it.year == year && it.month == month }
            .takeIf { it >= 0 }
            ?: months.indexOfFirst { it.year == year }.takeIf { it >= 0 }
            ?: return
        panelOpen = false
        scope.launch { listState.animateScrollToItem(index) }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = WarmDashboard) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 左上角年月：点开选年/月后跳转
                    Text(
                        "${focused.monthLabel()} ${if (panelOpen) "▲" else "▼"}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .clickable { panelOpen = !panelOpen }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton({ onSelect(null) }) { Text("全部日期", color = Ink) }
                    IconButton(onDismiss) { Icon(Icons.Default.Close, "关闭") }
                }
                if (panelOpen) {
                    // 与视图页左上角一致的滚轮选择（仅年、月两列）
                    YearMonthWheelPanel(
                        yearFrom = earliest.year,
                        yearTo = todayDate.year,
                        year = focused.year,
                        month = focused.month,
                        onCancel = { panelOpen = false },
                        onConfirm = { y, m -> jumpTo(y, m) }
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    WEEK_HEADER.split(" ").forEach { w ->
                        Text(
                            w, Modifier.weight(1f), textAlign = TextAlign.Center,
                            fontSize = 11.sp, color = EmptyDay
                        )
                    }
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().weight(1f)) {
                    itemsIndexed(months) { _, month ->
                        MonthBlock(
                            month = month,
                            loggedDates = loggedDates,
                            today = today,
                            selected = selected,
                            onPick = onSelect
                        )
                    }
                }
            }
        }
    }
}

/**
 * 年份 + 月份两列滚轮（复用视图页 `WheelColumn`）：上下滑动即选中，确定后跳转。
 * 年份范围限定为有日志的最早年份到今年，避免滑到没有任何日志的年份。
 */
@Composable
private fun YearMonthWheelPanel(
    yearFrom: Int,
    yearTo: Int,
    year: Int,
    month: Int,
    onCancel: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val years = remember(yearFrom, yearTo) { (yearFrom..yearTo).toList() }
    var pickedYear by remember(year) { mutableStateOf(year.coerceIn(yearFrom, yearTo)) }
    var pickedMonth by remember(month) { mutableStateOf(month.coerceIn(1, 12)) }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)).background(WarmCard).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("年", fontSize = 13.sp, color = EmptyDay)
                WheelColumn(
                    years.map { it.toString() },
                    (pickedYear - years.first()).coerceIn(0, years.lastIndex),
                    { pickedYear = years[it] }
                )
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("月", fontSize = 13.sp, color = EmptyDay)
                WheelColumn((1..12).map { "%02d".format(it) }, pickedMonth - 1, { pickedMonth = it + 1 })
            }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onCancel) { Text("取消", color = Ink.copy(alpha = .6f)) }
            Spacer(Modifier.width(8.dp))
            TextButton({ onConfirm(pickedYear, pickedMonth) }) { Text("确定", color = Ink, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MonthBlock(
    month: DemoDate,
    loggedDates: Set<String>,
    today: String,
    selected: String?,
    onPick: (String) -> Unit
) {
    val rows = remember(month.year, month.month) { monthCells(month.year, month.month) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 6.dp)) {
        Text(
            month.monthLabel(),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(vertical = 6.dp)
        )
        rows.forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val iso = "%04d-%02d-%02d".format(month.year, month.month, day)
                            val hasLog = iso in loggedDates
                            val isToday = iso == today
                            val color = when {
                                isToday -> TodayGreen
                                hasLog -> Ink
                                else -> EmptyDay
                            }
                            Box(
                                Modifier.fillMaxSize().clip(CircleShape)
                                    .background(if (iso == selected) WarmOrange.copy(alpha = .22f) else Color.Transparent)
                                    .clickable(enabled = hasLog) { onPick(iso) }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "$day",
                                    fontSize = 14.sp,
                                    color = color,
                                    fontWeight = if (isToday || iso == selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
