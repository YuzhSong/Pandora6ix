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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.data.*
import com.pandora6ix.app.ui.components.*
import com.pandora6ix.app.ui.theme.*

internal enum class SummaryColumn(val label: String, val width: Dp) {
    TAG("标签", 112.dp),
    DEADLINE("截止日期", 92.dp),
    PRIORITY("紧急程度", 80.dp),
    STATUS("完成情况", 86.dp),
    OWNER("负责人", 112.dp)
}

private val TaskNameWidth = 124.dp
private const val NoTagsLabel = "无标签"

internal class SummaryTableRow(task: TaskDto, name: (String) -> String, businessTimezone: String?) {
    val id = task.id
    val title = task.title
    val tags = summaryTags(task)
    private val deadline = summaryDeadline(task)
    val date = deadline.first.ifBlank { "—" }
    val time = deadline.second
    val priority = priorityLabel(task.priorityLevel)
    val priorityColor = priorityTone(task.priorityLevel)
    val status = if (task.reviewStatus == "APPROVED") summaryTaskStatus(task, businessTimezone)
        else reviewStatusLabel(task.reviewStatus)
    val statusColor = when {
        task.reviewStatus != "APPROVED" -> reviewStatusTone(task.reviewStatus)
        task.taskCompletionState() == "COMPLETED" -> StatusTone.SUCCESS
        task.taskCompletionState() == "AWAITING_ACCEPTANCE" -> StatusTone.WARNING
        status == "已逾期" -> StatusTone.ERROR
        else -> StatusTone.NEUTRAL
    }
    val owners = summaryOwners(task, name).ifEmpty { listOf("—") }

    fun values(column: SummaryColumn): List<String> = when (column) {
        SummaryColumn.TAG -> tags.map { it.name }.ifEmpty { listOf(NoTagsLabel) }
        SummaryColumn.DEADLINE -> listOf(date)
        SummaryColumn.PRIORITY -> listOf(priority)
        SummaryColumn.STATUS -> listOf(status)
        SummaryColumn.OWNER -> owners
    }

    fun matches(filters: Map<SummaryColumn, String>): Boolean =
        filters.all { (column, value) -> value in values(column) }
}

private val FiltersSaver = mapSaver<Map<SummaryColumn, String>>(
    save = { filters -> filters.mapKeys { it.key.name } },
    restore = { values -> SummaryColumn.entries.mapNotNull { column ->
        (values[column.name] as? String)?.let { column to it }
    }.toMap() }
)

@Composable
internal fun TaskSummaryTable(
    pager: SummaryPager<TaskDto>,
    name: (String) -> String,
    onTask: (String) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
    businessTimezone: String? = null
) {
    val colors = PandoraTheme.colors
    val horizontal = rememberSaveable(saver = ScrollState.Saver) { ScrollState(0) }
    val listState = rememberLazyListState()
    var filters by rememberSaveable(stateSaver = FiltersSaver) { mutableStateOf(emptyMap()) }
    var openColumn by remember { mutableStateOf<SummaryColumn?>(null) }
    val rows = pager.items.map { SummaryTableRow(it, name, businessTimezone) }
    val matches = rows.filter { it.matches(filters) }
    val shape = PandoraShapes.medium
    val header: @Composable (SummaryColumn) -> Unit = { column ->
        val selected = filters[column]
        ColumnFilter(
            column, selected, expanded = openColumn == column,
            options = if (openColumn == column) rows.flatMap { it.values(column) }.distinct().sorted() else emptyList(),
            onOpen = { openColumn = column },
            onDismiss = { openColumn = null },
            onSelect = { value ->
                openColumn = null
                if (value != selected) {
                    filters = if (value == null) filters - column else filters + (column to value)
                    listState.requestScrollToItem(0)
                }
            }
        )
    }
    BoxWithConstraints(modifier.fillMaxWidth().clip(shape).border(BorderStroke(PandoraDimensions.borderWidth, colors.border), shape).background(colors.surface)) {
        val baseWidth = SummaryColumn.entries.fold(TaskNameWidth + PandoraDimensions.borderWidth) { width, column -> width + column.width }
        val extra = (maxWidth - baseWidth).coerceAtLeast(0.dp)
        val nameWidth = TaskNameWidth + extra / 2
        val widths = SummaryColumn.entries.associateWith { column ->
            column.width + when (column) {
                SummaryColumn.TAG, SummaryColumn.OWNER -> extra / 4
                else -> 0.dp
            }
        }
        Column(Modifier.fillMaxSize()) {
            TableRow(Modifier.height(48.dp), horizontal, colors.neutralContainer, nameWidth, name = {
                Text("任务名称", style = PandoraTypography.caption, fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }) {
                SummaryColumn.entries.forEach { column -> Cell(widths.getValue(column)) { header(column) } }
            }
            LazyColumn(Modifier.weight(1f).testTag("summary-task-rows"), state = listState) {
                items(matches, key = { it.id }) { row ->
                    TaskRow(row, horizontal, nameWidth, widths) { onTask(row.id) }
                }
                item(key = "footer") {
                    Box(Modifier.padding(horizontal = PandoraSpacing.space12)) {
                        CollectionFooter(pager, matches.isEmpty(), if (filters.isEmpty()) "暂无相关任务" else "没有符合筛选的任务",
                            filtered = filters.isNotEmpty(), onMore = onMore)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColumnFilter(
    column: SummaryColumn,
    selected: String?,
    expanded: Boolean,
    options: List<String>,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (String?) -> Unit
) {
    val colors = PandoraTheme.colors
    val tint = if (selected == null) colors.textSecondary else colors.primary
    Box(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxSize().clickable(role = Role.Button, onClickLabel = "筛选${column.label}", onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(column.label, style = PandoraTypography.caption, fontWeight = FontWeight.SemiBold,
                color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp), tint = tint)
        }
        PandoraDropdownMenu(expanded, onDismiss) {
            DropdownMenuItem(text = { Text(if (selected == null) "全部 ✓" else "全部") }, onClick = { onSelect(null) })
            (options + listOfNotNull(selected)).distinct().forEach { option ->
                DropdownMenuItem(
                    text = { Text(option + if (option == selected) " ✓" else "", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun TableRow(
    rowSize: Modifier,
    horizontal: ScrollState,
    background: Color,
    nameWidth: Dp,
    modifier: Modifier = Modifier,
    name: @Composable BoxScope.() -> Unit,
    cells: @Composable RowScope.() -> Unit
) {
    val colors = PandoraTheme.colors
    Column(modifier.fillMaxWidth().background(background)) {
        Row(
            Modifier.fillMaxWidth().then(rowSize).drawBehind {
                drawRect(
                    color = colors.border,
                    topLeft = Offset(nameWidth.roundToPx().toFloat(), 0f),
                    size = Size(PandoraDimensions.borderWidth.roundToPx().toFloat(), size.height)
                )
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cell(nameWidth, name)
            Spacer(Modifier.width(PandoraDimensions.borderWidth))
            Row(Modifier.horizontalScroll(horizontal), verticalAlignment = Alignment.CenterVertically, content = cells)
        }
        Box(Modifier.fillMaxWidth().height(PandoraDimensions.borderWidth).background(colors.divider))
    }
}

@Composable
private fun Cell(width: Dp, content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.width(width).padding(horizontal = PandoraSpacing.space8), contentAlignment = Alignment.CenterStart, content = content)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskRow(row: SummaryTableRow, horizontal: ScrollState, nameWidth: Dp, widths: Map<SummaryColumn, Dp>, onOpen: () -> Unit) {
    val colors = PandoraTheme.colors
    TableRow(
        Modifier.heightIn(min = 60.dp), horizontal, colors.surface, nameWidth,
        Modifier.clickable(onClickLabel = "查看任务详情", onClick = onOpen),
        name = { Text(row.title, style = PandoraTypography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis) }
    ) {
        Cell(widths.getValue(SummaryColumn.TAG)) {
            if (row.tags.isEmpty()) {
                Text(NoTagsLabel, style = PandoraTypography.caption, color = colors.textSecondary)
            } else {
                FlowRow(
                    Modifier.padding(vertical = PandoraSpacing.space4),
                    horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space4),
                    verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4)
                ) {
                    row.tags.forEach { tag -> PandoraTagChip(tag.name, tag.colorHex) }
                }
            }
        }
        Cell(widths.getValue(SummaryColumn.DEADLINE)) {
            Column {
                Text(row.date, style = PandoraTypography.bodyMedium, maxLines = 1)
                row.time?.let { Text(it, style = PandoraTypography.caption, color = colors.textSecondary) }
            }
        }
        Cell(widths.getValue(SummaryColumn.PRIORITY)) { PandoraStatusChip(row.priority, row.priorityColor, maxWidth = 64.dp) }
        Cell(widths.getValue(SummaryColumn.STATUS)) { PandoraStatusChip(row.status, row.statusColor, maxWidth = 76.dp) }
        Cell(widths.getValue(SummaryColumn.OWNER)) {
            Text(row.owners.joinToString("、"), style = PandoraTypography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}
