package com.pandora6ix.app.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.data.CalendarRepository
import com.pandora6ix.app.data.TaskDto
import com.pandora6ix.app.data.UserRepository
import com.pandora6ix.app.data.businessTodayKey
import com.pandora6ix.app.data.toTaskItem
import com.pandora6ix.app.mock.DemoDate
import com.pandora6ix.app.ui.calendar.MonthScrollRow
import com.pandora6ix.app.ui.calendar.MonthWeekdayHeader
import com.pandora6ix.app.ui.calendar.monthWeeks
import com.pandora6ix.app.ui.calendar.weekTasksOnDate
import com.pandora6ix.app.ui.components.PandoraDialog
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import java.time.LocalDate
import java.time.YearMonth

@Composable
internal fun MemberTaskMonth(
    memberId: String,
    calendarRepo: CalendarRepository,
    businessTimezone: String?,
    name: (String) -> String,
    userRepo: UserRepository,
    onTask: (String) -> Unit
) {
    val today = LocalDate.parse(businessTodayKey(businessTimezone))
    val currentMonth = YearMonth.from(today)
    var selectedMonth by rememberSaveable(memberId) { mutableStateOf(currentMonth.toString()) }
    val month = YearMonth.parse(selectedMonth)
    val anchor = DemoDate(month.year, month.monthValue, 1)
    val calendarToday = DemoDate(today.year, today.monthValue, today.dayOfMonth)
    var openDay by remember(memberId, month) { mutableStateOf<DemoDate?>(null) }
    var retry by remember(memberId, month) { mutableIntStateOf(0) }
    val pager = remember(memberId, month, calendarRepo) {
        SummaryPager<TaskDto>(TaskDto::id) { cursor ->
            calendarRepo.tasks(
                scope = "managed",
                from = month.atDay(1).toString(),
                to = month.atEndOfMonth().toString(),
                cursor = cursor
            ).let { SummaryPage(it.items, it.nextCursor) }
        }
    }
    LaunchedEffect(pager, retry) {
        do {
            pager.load()
        } while (pager.error == null && pager.nextCursor != null)
    }

    PanelHeading("任务月历")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton({ selectedMonth = month.minusMonths(1).toString() }) {
            Icon(Icons.Default.KeyboardArrowLeft, "上个月")
        }
        Text(anchor.monthLabel(), Modifier.weight(1f), style = PandoraTypography.labelLarge)
        if (month != currentMonth) {
            TextButton({ selectedMonth = currentMonth.toString() }) { Text("本月") }
        }
        IconButton({ selectedMonth = month.plusMonths(1).toString() }) {
            Icon(Icons.Default.KeyboardArrowRight, "下个月")
        }
    }
    if (!pager.initialized || pager.loading || pager.error != null || pager.nextCursor != null) {
        CollectionFooter(pager, empty = false, emptyText = "", showMore = false) { retry++ }
        return
    }

    val records = remember(pager.items, memberId) { memberCalendarTasks(pager.items, memberId) }
    val tasks = remember(records) { records.map(TaskDto::toTaskItem) }
    val weeks = remember(month) { monthWeeks(anchor) }
    Column {
        MonthWeekdayHeader()
        weeks.forEach { week ->
            MonthScrollRow(anchor, week, tasks, calendarToday, { onTask(it.id) }, { openDay = it }, businessTimezone)
        }
    }
    if (tasks.isEmpty()) Note("本月暂无任务")

    openDay?.let { date ->
        val recordsById = remember(records) { records.associateBy(TaskDto::id) }
        val dayTasks = remember(tasks, date) { weekTasksOnDate(tasks, date) }
        PandoraDialog(onDismissRequest = { openDay = null }) {
            Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                PanelHeading("${date.shortLabel()}任务")
                if (dayTasks.isEmpty()) Note("当天暂无任务")
                else LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)
                ) {
                    items(dayTasks, key = { it.id }) { task ->
                        SummaryTaskRow(recordsById.getValue(task.id), name, userRepo, businessTimezone) {
                            openDay = null
                            onTask(task.id)
                        }
                    }
                }
                TextButton({ openDay = null }, Modifier.align(Alignment.End)) {
                    Text("关闭", color = PandoraTheme.colors.primary)
                }
            }
        }
    }
}
