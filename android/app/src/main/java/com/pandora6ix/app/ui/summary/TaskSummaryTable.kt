package com.pandora6ix.app.ui.summary

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.data.*
import com.pandora6ix.app.ui.components.*
import com.pandora6ix.app.ui.theme.*

/** 冻结的“任务名称”列；其余列在同一个横向滚动状态下整表同步滑动 */
private val NameWidth = 120.dp
private val Columns = listOf("截止日期" to 92.dp, "紧急程度" to 76.dp, "完成情况" to 88.dp, "进度备注" to 200.dp, "负责人" to 120.dp)
private val RowHeight = 60.dp

/**
 * 表格式任务汇总（参照甲方使用的在线表格）：表头固定、首列固定，其余列左右滑动；
 * 按主标签分组显示组标题行，点任务行进入原任务详情。
 */
@Composable
internal fun TaskSummaryTable(
    pager: SummaryPager<TaskDto>,
    name: (String) -> String,
    onTask: (String) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    businessTimezone: String? = null,
    showScrollHint: Boolean = true
) {
    val colors = PandoraTheme.colors
    val horizontal = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val listState = rememberLazyListState()
    val groups = remember(pager.items) { summaryTableGroups(pager.items) }
    val shape = PandoraShapes.medium
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)) {
        // 只在还没滑动过时提示一次，滑动后不再占位
        if (showScrollHint && horizontal.value == 0 && horizontal.maxValue > 0) Text(
            "左右滑动查看更多列，或点右上角横屏查看",
            style = PandoraTypography.caption, color = colors.textSecondary
        )
        Column(Modifier.weight(1f).fillMaxWidth().clip(shape).border(BorderStroke(PandoraDimensions.borderWidth, colors.border), shape).background(colors.surface)) {
            TableRow(44.dp, horizontal, colors.neutralContainer, name = { HeaderText("任务名称") }) {
                Columns.forEach { (label, width) -> Cell(width) { HeaderText(label) } }
            }
            LazyColumn(Modifier.weight(1f).testTag("summary-task-rows"), state = listState) {
                groups.forEachIndexed { index, (group, tasks) ->
                    item(key = "group:$index:$group") { GroupRow(group, tasks.size) }
                    items(tasks, key = { it.id }) { task -> TaskRow(task, name, horizontal, businessTimezone) { onTask(task.id) } }
                }
                item(key = "footer") {
                    Box(Modifier.padding(horizontal = PandoraSpacing.space12)) {
                        CollectionFooter(pager, pager.items.isEmpty(), "暂无相关任务", onMore = onMore)
                    }
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    height: Dp,
    horizontal: ScrollState,
    background: Color,
    modifier: Modifier = Modifier,
    name: @Composable BoxScope.() -> Unit,
    cells: @Composable RowScope.() -> Unit
) {
    val colors = PandoraTheme.colors
    Column(modifier.fillMaxWidth().background(background)) {
        Row(Modifier.fillMaxWidth().height(height)) {
            Box(Modifier.width(NameWidth).fillMaxHeight().padding(horizontal = PandoraSpacing.space8), contentAlignment = Alignment.CenterStart, content = name)
            Box(Modifier.width(PandoraDimensions.borderWidth).fillMaxHeight().background(colors.border))
            Row(Modifier.fillMaxHeight().horizontalScroll(horizontal), verticalAlignment = Alignment.CenterVertically, content = cells)
        }
        Box(Modifier.fillMaxWidth().height(PandoraDimensions.borderWidth).background(colors.divider))
    }
}

@Composable
private fun Cell(width: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.width(width).fillMaxHeight().padding(horizontal = PandoraSpacing.space8), contentAlignment = Alignment.CenterStart, content = content)
}

@Composable
private fun HeaderText(text: String) {
    Text(text, style = PandoraTypography.caption, fontWeight = FontWeight.SemiBold, color = PandoraTheme.colors.textSecondary, maxLines = 1)
}

@Composable
private fun GroupRow(group: String, count: Int) {
    val colors = PandoraTheme.colors
    Column(Modifier.fillMaxWidth().background(colors.primaryContainer)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 36.dp).padding(horizontal = PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
            Text(group, style = PandoraTypography.labelLarge, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            Text(" · $count", style = PandoraTypography.caption, color = colors.textSecondary)
        }
        Box(Modifier.fillMaxWidth().height(PandoraDimensions.borderWidth).background(colors.divider))
    }
}

@Composable
private fun TaskRow(task: TaskDto, name: (String) -> String, horizontal: ScrollState, businessTimezone: String?, onOpen: () -> Unit) {
    val colors = PandoraTheme.colors
    TableRow(
        RowHeight, horizontal, colors.surface,
        Modifier.clickable(onClickLabel = "查看任务详情", onClick = onOpen),
        name = { Text(task.title, style = PandoraTypography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    ) {
        val (date, time) = summaryDeadline(task)
        Cell(Columns[0].second) {
            Column {
                Text(date, style = PandoraTypography.bodyMedium, maxLines = 1)
                time?.let { Text(it, style = PandoraTypography.caption, color = colors.textSecondary) }
            }
        }
        Cell(Columns[1].second) { PandoraStatusChip(priorityLabel(task.priorityLevel), priorityTone(task.priorityLevel), maxWidth = 64.dp) }
        Cell(Columns[2].second) {
            // 未通过审批的任务先显示审批状态，审批通过后显示完成情况
            if (task.reviewStatus != "APPROVED") PandoraStatusChip(reviewStatusLabel(task.reviewStatus), reviewStatusTone(task.reviewStatus), maxWidth = 76.dp)
            else PandoraStatusChip(summaryTaskStatus(task, businessTimezone), when {
                task.taskCompletionState() == "COMPLETED" -> StatusTone.SUCCESS
                task.taskCompletionState() == "AWAITING_ACCEPTANCE" -> StatusTone.WARNING
                task.isTaskOverdue(businessTimezone) -> StatusTone.ERROR
                else -> StatusTone.NEUTRAL
            }, maxWidth = 76.dp)
        }
        Cell(Columns[3].second) {
            Text(summaryProgressNote(task), style = PandoraTypography.bodyMedium, color = colors.textSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Cell(Columns[4].second) {
            Text(summaryOwners(task, name), style = PandoraTypography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
