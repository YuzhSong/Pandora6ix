package com.pandora6ix.app.ui.calendar

import com.pandora6ix.app.ui.theme.*
import com.pandora6ix.app.ui.components.*
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
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择日期") },
        text = {
            Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1.2f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("年", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
                    WheelColumn(years.map { it.toString() }, (year - years.first()).coerceIn(0, years.size - 1), { year = years[it] })
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("月", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
                    WheelColumn((1..12).map { "%02d".format(it) }, month - 1, { month = it + 1 })
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("日", style = PandoraTypography.caption, color = PandoraTheme.colors.textSecondary)
                    // 日列数量随年月变化：以年月为 key 重建滚轮，定位到收敛后的日
                    key(year, month) { WheelColumn(dayValues, (day - 1).coerceIn(0, dayValues.size - 1), { day = it + 1 }) }
                }
            }
        },
        confirmButton = { PandoraDialogConfirm("确定", { onConfirm(DemoDate(year, month, day)) }) },
        dismissButton = { PandoraDialogDismiss("取消", onDismiss) }
    )
}

/** 单列滚轮：5 行×48dp=240dp 视口、按父级权重分宽、吸附停位，中间行为选中值；点击直接选中 */
@Composable
internal fun WheelColumn(values: List<String>, selected: Int, onSelected: (Int) -> Unit) {
    val itemHeight = PandoraDimensions.wheelRowHeight
    val colors = PandoraTheme.colors
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selected)
    // 滚动停位即选中值
    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex }.collect { index -> if (index in values.indices && index != selected) onSelected(index) }
    }
    // 选中值变化（含点击）时对中定位
    LaunchedEffect(selected) {
        if (state.firstVisibleItemIndex != selected || state.firstVisibleItemScrollOffset != 0) state.scrollToItem(selected)
    }
    Box(Modifier.height(itemHeight * 5).fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemHeight).clip(PandoraShapes.medium).background(colors.primaryContainer))
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
                        style = if (index == selected) PandoraTypography.titleSmall else PandoraTypography.bodyLarge,
                        fontWeight = if (index == selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (index == selected) colors.textPrimary else colors.textTertiary,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
