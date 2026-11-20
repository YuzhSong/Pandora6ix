package com.pandora6ix.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.pandora6ix.app.data.DailyItem
import com.pandora6ix.app.data.DailyItemSchedule
import com.pandora6ix.app.data.DailyItemYears
import com.pandora6ix.app.mock.DemoDate
import com.pandora6ix.app.ui.calendar.DateWheelDialog
import com.pandora6ix.app.ui.calendar.TimeWheelDialog
import com.pandora6ix.app.ui.calendar.minuteLabel
import com.pandora6ix.app.ui.theme.*
import java.time.LocalDate

@Composable
internal fun DailyItemEditor(
    item: DailyItem?,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onSave: (String, DailyItemSchedule) -> Unit
) {
    var content by rememberSaveable { mutableStateOf(item?.content.orEmpty()) }
    var startKey by rememberSaveable { mutableStateOf(item?.date ?: initialDate.toString()) }
    var endKey by rememberSaveable { mutableStateOf(item?.endDate ?: startKey) }
    var startMinute by rememberSaveable { mutableIntStateOf(item?.startMinute ?: 9 * 60) }
    var endMinute by rememberSaveable { mutableIntStateOf(item?.endMinute ?: 10 * 60) }
    var allDay by rememberSaveable { mutableStateOf(item != null && item.startMinute == null) }
    var picker by rememberSaveable { mutableStateOf<SchedulePicker?>(null) }
    var showError by rememberSaveable { mutableStateOf(false) }
    val start = LocalDate.parse(startKey)
    val end = LocalDate.parse(endKey)
    val range = DailyItemSchedule(
        start, end, startMinute.takeUnless { allDay }, endMinute.takeUnless { allDay }
    )
    val error = when {
        content.isBlank() -> "请输入事项内容"
        !range.isValid && allDay -> "结束日期不能早于开始日期"
        !range.isValid -> "结束时间必须晚于开始时间"
        else -> null
    }
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "添加事项" else "编辑事项") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)
            ) {
                PandoraTextField(
                    content, { content = it }, Modifier.fillMaxWidth(),
                    label = { Text("事项内容") }, minLines = 2, maxLines = 4
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("全天", Modifier.weight(1f), style = PandoraTypography.labelLarge)
                    Switch(allDay, { allDay = it }, colors = pandoraSwitchColors())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    OutlinedButton({ picker = SchedulePicker.StartDate }, Modifier.weight(1f)) {
                        Text("开始日期\n$start", style = PandoraTypography.bodyMedium)
                    }
                    OutlinedButton({ picker = SchedulePicker.EndDate }, Modifier.weight(1f)) {
                        Text("结束日期\n$end", style = PandoraTypography.bodyMedium)
                    }
                }
                if (!allDay) {
                    Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        OutlinedButton({ picker = SchedulePicker.StartTime }, Modifier.weight(1f)) {
                            Text("开始 ${minuteLabel(startMinute)}", style = PandoraTypography.bodyMedium)
                        }
                        OutlinedButton({ picker = SchedulePicker.EndTime }, Modifier.weight(1f)) {
                            Text("结束 ${minuteLabel(endMinute)}", style = PandoraTypography.bodyMedium)
                        }
                    }
                }
                if (showError && error != null) {
                    Text(error, style = PandoraTypography.bodyMedium, color = PandoraTheme.colors.error)
                }
            }
        },
        confirmButton = {
            PandoraDialogConfirm("保存", {
                if (error == null) onSave(content.trim(), range) else showError = true
            })
        },
        dismissButton = { PandoraDialogDismiss(onClick = onDismiss) }
    )

    when (picker) {
        SchedulePicker.StartDate, SchedulePicker.EndDate -> {
            val editingStart = picker == SchedulePicker.StartDate
            val date = if (editingStart) start else end
            DateWheelDialog(
                initial = DemoDate(date.year, date.monthValue, date.dayOfMonth),
                onDismiss = { picker = null },
                onConfirm = { value ->
                    val key = LocalDate.of(value.year, value.month, value.day).toString()
                    if (editingStart) startKey = key else endKey = key
                    picker = null
                },
                yearRange = DailyItemYears
            )
        }
        SchedulePicker.StartTime, SchedulePicker.EndTime -> {
            val editingStart = picker == SchedulePicker.StartTime
            TimeWheelDialog(
                initialMinute = if (editingStart) startMinute else endMinute,
                onDismiss = { picker = null },
                onConfirm = { value ->
                    if (editingStart) startMinute = value else endMinute = value
                    picker = null
                }
            )
        }
        null -> Unit
    }
}

@Composable
internal fun DailyItemDetails(item: DailyItem, onDismiss: () -> Unit, onEdit: () -> Unit) {
    val colors = PandoraTheme.colors
    val allDay = item.startMinute == null && item.endMinute == null
    PandoraAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("事项详情") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)
            ) {
                Text(item.content, style = PandoraTypography.bodyLarge, color = colors.textPrimary)
                HorizontalDivider(color = colors.divider)
                Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    if (allDay) {
                        Text("全天", style = PandoraTypography.labelLarge, color = colors.primary)
                    }
                    DailyItemDetailTime(
                        if (allDay) "开始日期" else "开始时间", item.date, item.startMinute
                    )
                    DailyItemDetailTime(
                        if (allDay) "结束日期" else "结束时间", item.endDate, item.endMinute
                    )
                }
            }
        },
        confirmButton = { PandoraDialogConfirm("编辑", onEdit) },
        dismissButton = { PandoraDialogDismiss("关闭", onDismiss) }
    )
}

@Composable
private fun DailyItemDetailTime(label: String, date: String, minute: Int?) {
    val colors = PandoraTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)
    ) {
        Text(label, style = PandoraTypography.bodyMedium, color = colors.textSecondary)
        Text(
            if (minute == null) date else "$date ${minuteLabel(minute)}",
            Modifier.weight(1f),
            style = PandoraTypography.bodyMedium,
            color = colors.textPrimary,
            textAlign = TextAlign.End
        )
    }
}

private enum class SchedulePicker { StartDate, EndDate, StartTime, EndTime }
