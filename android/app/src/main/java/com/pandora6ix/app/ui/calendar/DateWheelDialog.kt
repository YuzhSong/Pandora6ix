package com.pandora6ix.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pandora6ix.app.mock.DemoDate
import com.pandora6ix.app.mock.todayDate
import com.pandora6ix.app.ui.theme.CreamDeep
import com.pandora6ix.app.ui.theme.Ink

/**
 * 滚轮选择器共用组件（添加任务与视图页头部共用）：
 * - 日期：年(1900–2100)/月(1–12)/日三列；日列按所选年月真实天数（万年历语义），年月变化时日自动收敛
 * - 时间：时(00–23)/分(00–59) 两列
 */
@Composable
internal fun DateWheelDialog(initial: DemoDate, onDismiss: () -> Unit, onConfirm: (DemoDate) -> Unit) {
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.month) }
    var day by remember { mutableStateOf(initial.day) }
    val years = (1900..2100).toList()
    fun daysInMonth(y: Int, m: Int): Int =
        java.util.Calendar.getInstance().apply { clear(); set(y, m - 1, 1) }.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    // 年月变化时把日收敛到该月真实天数内（如 31 日切到 4 月 → 30 日）
    LaunchedEffect(year, month) {
        val max = daysInMonth(year, month)
        if (day > max) day = max
    }
    val dayValues = (1..daysInMonth(year, month)).map { "%02d".format(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期", fontWeight = FontWeight.Bold) },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("年", fontSize = 13.sp, color = Ink.copy(alpha = .6f))
                    WheelColumn(years.map { it.toString() }, (year - years.first()).coerceIn(0, years.size - 1), { year = years[it] })
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("月", fontSize = 13.sp, color = Ink.copy(alpha = .6f))
                    WheelColumn((1..12).map { "%02d".format(it) }, month - 1, { month = it + 1 })
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("日", fontSize = 13.sp, color = Ink.copy(alpha = .6f))
                    // 日列数量随年月变化：以年月为 key 重建滚轮，定位到收敛后的日
                    key(year, month) { WheelColumn(dayValues, (day - 1).coerceIn(0, dayValues.size - 1), { day = it + 1 }) }
                }
            }
        },
        confirmButton = { TextButton({ onConfirm(DemoDate(year, month, day)) }) { Text("确定", color = Ink, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text("取消", color = Ink.copy(alpha = .6f)) } }
    )
}

/** 单列滚轮：固定 5 行视口、吸附停位，中间行为选中值；点击直接选中 */
@Composable
internal fun WheelColumn(values: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    val itemHeight = 40.dp
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    // 滚动停位即选中值
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { index -> if (index in values.indices && index != selected) onSelected(index) }
    }
    // 选中值变化（含点击）时对中定位
    LaunchedEffect(selected) {
        if (state.firstVisibleItemIndex != selected || state.firstVisibleItemScrollOffset != 0) state.scrollToItem(selected)
    }
    Box(Modifier.height(itemHeight * 5).width(72.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemHeight).clip(RoundedCornerShape(10.dp)).background(CreamDeep))
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxWidth(),
            flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = itemHeight * 2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(count = values.size, key = { it }) { index ->
                Box(Modifier.fillMaxWidth().height(itemHeight).clickable { onSelected(index) }, contentAlignment = Alignment.Center) {
                    Text(
                        values[index],
                        fontSize = if (index == selected) 18.sp else 15.sp,
                        fontWeight = if (index == selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == selected) Ink else Ink.copy(alpha = .35f)
                    )
                }
            }
        }
    }
}
