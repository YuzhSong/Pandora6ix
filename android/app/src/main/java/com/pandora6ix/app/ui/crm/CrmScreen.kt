package com.pandora6ix.app.ui.crm

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pandora6ix.app.data.ApiClient
import com.pandora6ix.app.data.ApiException
import com.pandora6ix.app.ui.components.PandoraEmptyState
import com.pandora6ix.app.ui.components.PandoraListItem
import com.pandora6ix.app.ui.components.PandoraPanel
import com.pandora6ix.app.ui.components.PandoraTopBar
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

enum class CrmDirectory(val title: String, val path: String) {
    CUSTOMERS("我的客户", "customers"),
    BUSINESSES("我参与的业务", "businesses"),
}

@Serializable
private data class Participant(val displayName: String, val responsibility: String)

@Serializable
private data class CrmRecord(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    val kind: String,
    val status: String? = null,
    val ownerName: String,
    val category: String? = null,
    val contact: String? = null,
    val customerName: String? = null,
    val eventDate: String? = null,
    val eventTime: String? = null,
    val notes: String? = null,
    val participants: List<Participant> = emptyList(),
) {
    val label get() = name ?: title.orEmpty()
}

private data class DirectoryState(
    val rows: List<CrmRecord>? = null,
    val error: String? = null,
)

@Composable
fun CrmScreen(api: ApiClient, directory: CrmDirectory, onBack: () -> Unit) {
    var selectedId by rememberSaveable(directory) { mutableStateOf<String?>(null) }
    var offset by rememberSaveable(directory) { mutableIntStateOf(0) }
    var revision by rememberSaveable(directory) { mutableIntStateOf(0) }
    val back = { if (selectedId != null) selectedId = null else onBack() }
    BackHandler(onBack = back)
    val selected = selectedId
    val path = "/crm/${directory.path}"
    val state = key(path, selected, offset, revision) {
        val result by produceState(DirectoryState(), api) {
            try {
                val rows = if (selected == null) {
                    api.get("$path?mine=true&offset=$offset", ListSerializer(CrmRecord.serializer()))
                } else {
                    listOf(api.get("$path/$selected", CrmRecord.serializer()))
                }
                value = DirectoryState(rows)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                value = DirectoryState(error = (error as? ApiException)?.friendlyMessage() ?: "加载失败，请重试")
            }
        }
        result
    }

    Column(Modifier.fillMaxSize()) {
        PandoraTopBar(if (selected == null) directory.title else "详情", onBack = back) {
            TextButton(onClick = { revision++ }) { Text("刷新") }
        }
        val rows = state.rows
        val error = state.error
        when {
            error != null -> PandoraEmptyState(error, actionText = "重试", onAction = { revision++ })
            rows == null -> CircularProgressIndicator()
            selected != null -> CrmDetail(rows.single())
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(PandoraSpacing.space16),
                verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8),
            ) {
                if (rows.isEmpty()) item { PandoraEmptyState("暂无记录") }
                items(rows, key = { it.id }) { record ->
                    PandoraPanel(Modifier.fillMaxWidth()) {
                        PandoraListItem(record.label, onClick = { selectedId = record.id })
                        Text(
                            listOfNotNull(kindLabel(record.kind), record.eventDate, record.customerName)
                                .joinToString(" · "),
                            style = PandoraTypography.bodyMedium,
                            color = PandoraTheme.colors.textSecondary,
                        )
                    }
                }
                if (offset > 0 || rows.size == 100) item {
                    Row(horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                        TextButton(onClick = { offset -= 100 }, enabled = offset > 0) { Text("上一页") }
                        TextButton(onClick = { offset += 100 }, enabled = rows.size == 100) { Text("下一页") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CrmDetail(record: CrmRecord) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PandoraSpacing.space16),
    ) {
        item {
            PandoraPanel(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    Text(record.label, style = PandoraTypography.titleLarge)
                    Text("类型：${kindLabel(record.kind)}")
                    Text("负责人：${record.ownerName}")
                    record.category?.let { Text("分类：$it") }
                    record.contact?.let { Text("联系方式：$it") }
                    record.customerName?.let { Text("客户：$it") }
                    record.eventDate?.let { date ->
                        Text("业务时间：" + listOfNotNull(date, record.eventTime).joinToString(" "))
                    }
                    record.status?.let { Text("状态：${statusLabel(it)}") }
                    record.notes?.let { Text("备注：$it") }
                    if (record.participants.isNotEmpty()) {
                        Text("参与员工", style = PandoraTypography.titleSmall)
                        record.participants.forEach { Text("${it.displayName} · ${it.responsibility}") }
                    }
                }
            }
        }
    }
}

private fun kindLabel(kind: String) = when (kind) {
    "PERSON" -> "个人客户"
    "COMPANY" -> "企业客户"
    "MALL" -> "线上商城"
    "PERFORMANCE" -> "演出与票务"
    "PRIVATE_DINING" -> "私宴"
    "VISIT" -> "企业参访"
    "SALON" -> "沙龙"
    else -> "其他"
}

private fun statusLabel(status: String) = when (status) {
    "PLANNED" -> "计划中"
    "IN_PROGRESS" -> "进行中"
    "COMPLETED" -> "已完成"
    "CANCELLED" -> "已取消"
    else -> status
}
