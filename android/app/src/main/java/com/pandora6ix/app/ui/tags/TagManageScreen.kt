package com.pandora6ix.app.ui.tags

import com.pandora6ix.app.ui.theme.*
import com.pandora6ix.app.ui.components.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.pandora6ix.app.data.ApiException
import com.pandora6ix.app.data.SessionExpiredException
import com.pandora6ix.app.data.TagDto
import com.pandora6ix.app.data.TagPermissions
import com.pandora6ix.app.data.TagRepository
import com.pandora6ix.app.ui.calendar.tagHexColor
import com.pandora6ix.app.ui.theme.CreamDeep
import com.pandora6ix.app.ui.theme.ThemeButton
import com.pandora6ix.app.ui.theme.OnThemeButton
import com.pandora6ix.app.ui.theme.Ink
import com.pandora6ix.app.ui.theme.WarmCard
import com.pandora6ix.app.ui.theme.WarmOrange
import kotlinx.coroutines.launch

/** 浅色候选色板：全部适配黑色文字（不同标签可选相同颜色） */
val TagColorPalette = listOf(
    0xFFFDEBD3, 0xFFFFE1E1, 0xFFDCF3E5, 0xFFE0EDFF,
    0xFFEFE4FF, 0xFFFFE6F0, 0xFFF6F1DB, 0xFFDDF3EE,
    0xFFFFF2CF, 0xFFE8F1DC, 0xFFFFE8D9, 0xFFE9E9F8
).map { Color(it) }

/**
 * 标签管理：创建 / 删除 / 更改名称与配色（不同标签可选相同颜色）。
 * 权限由 TagPermissions 钩子控制（当前默认全部允许，未来接入等级化权限）；服务端为最终裁决。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManageScreen(tagRepo: TagRepository, permissions: TagPermissions, reloadTags: () -> Unit, onBack: () -> Unit) {
    var tags by remember { mutableStateOf<List<TagDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var loadRetry by remember { mutableStateOf(0) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<TagDto?>(null) }
    var deleting by remember { mutableStateOf<TagDto?>(null) }
    var busy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        loading = true; loadError = null
        scope.launch {
            try {
                tags = tagRepo.tags()
            } catch (e: SessionExpiredException) {
                // 401 已由全局回调处理
            } catch (e: Exception) {
                loadError = (e as? ApiException)?.friendlyMessage() ?: "加载失败，请重试"
            } finally {
                loading = false
            }
        }
    }
    LaunchedEffect(loadRetry) { load() }

    androidx.activity.compose.BackHandler(onBack = onBack)
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Column { Text("标签管理", fontWeight = FontWeight.Bold); Text("任务与日志的标签配色", fontSize = 12.sp, color = Ink.copy(alpha = .6f)) } },
            navigationIcon = { IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
        Box(Modifier.fillMaxSize()) {
            when {
                loading && tags.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center).size(36.dp), color = WarmOrange)
                loadError != null -> Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(loadError!!, color = Ink.copy(alpha = .7f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    PandoraButton("重试", { loadRetry++ }, variant = PandoraButtonVariant.Outline)
                }
                else -> Column(Modifier.fillMaxSize()) {
                    LazyColumn(Modifier.fillMaxSize().weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tags, key = { it.id }) { tag ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(WarmCard).padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(26.dp).clip(CircleShape).background(tagHexColor(tag.colorHex)).border(1.dp, Ink.copy(alpha = .12f), CircleShape))
                                Spacer(Modifier.width(12.dp))
                                Text(tag.name, Modifier.weight(1f), fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (permissions.canUpdate) IconButton({ editing = tag; showEditor = true }, Modifier.size(36.dp)) { Icon(Icons.Default.Edit, "编辑标签", Modifier.size(18.dp), tint = Ink.copy(alpha = .65f)) }
                                if (permissions.canDelete) IconButton({ deleting = tag; actionError = null }, Modifier.size(36.dp)) { Icon(Icons.Default.Delete, "删除标签", Modifier.size(18.dp), tint = com.pandora6ix.app.ui.theme.CoralDark.copy(alpha = .75f)) }
                            }
                        }
                        if (tags.isEmpty()) item {
                            Text("暂无标签。创建第一个标签后，可在添加任务与日志中使用。", Modifier.padding(8.dp), fontSize = 13.sp, color = Ink.copy(alpha = .55f))
                        }
                    }
                    if (permissions.canCreate) Box(Modifier.fillMaxWidth().padding(16.dp)) {
                        PandoraButton({ editing = null; showEditor = true; actionError = null }, Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("新建标签")
                        }
                    }
                }
            }
        }
    }

    if (showEditor) TagEditDialog(
        initialName = editing?.name ?: "",
        initialColorHex = editing?.colorHex ?: "",
        onSave = { name, colorHex ->
            busy = true; actionError = null
            scope.launch {
                try {
                    if (editing == null) tagRepo.create(name, colorHex)
                    else tagRepo.patch(editing!!.id, editing!!.version, name, colorHex)
                    showEditor = false
                    load()
                    reloadTags()
                } catch (e: SessionExpiredException) {
                    // 401 已由全局回调处理
                } catch (e: ApiException) {
                    actionError = e.friendlyMessage()
                    if (e.httpStatus == 409) load()
                } catch (e: Exception) {
                    actionError = "操作失败，请稍后重试"
                } finally {
                    busy = false
                }
            }
        },
        busy = busy,
        error = actionError,
        onDismiss = { if (!busy) showEditor = false }
    )

    deleting?.let { tag ->
        PandoraAlertDialog(
            onDismissRequest = { if (!busy) deleting = null },
            title = { Text("删除标签？") },
            text = { Text("「${tag.name}」将从企业与所有使用它的任务、日志中移除，且不可恢复。") },
            confirmButton = {
                if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = WarmOrange)
                else TextButton({
                    busy = true
                    scope.launch {
                        try {
                            tagRepo.delete(tag.id)
                            deleting = null
                            load()
                            reloadTags()
                        } catch (e: SessionExpiredException) {
                            // 401 已由全局回调处理
                        } catch (e: ApiException) {
                            actionError = e.friendlyMessage()
                            if (e.httpStatus == 409) load()
                        } catch (e: Exception) {
                            actionError = "操作失败，请稍后重试"
                        } finally {
                            busy = false
                        }
                    }
                }) { Text("删除", style = PandoraTypography.labelLarge, color = PandoraTheme.colors.error) }
            },
            dismissButton = { if (!busy) PandoraDialogDismiss("取消", { deleting = null }) }
        )
    }
}

/** 新建/编辑对话框：名称 + 浅色色板（黑色文字预览，不同标签可选相同颜色） */
@Composable
private fun TagEditDialog(initialName: String, initialColorHex: String, busy: Boolean, error: String?, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var colorIndex by remember {
        mutableStateOf(TagColorPalette.indexOfFirst { tagHexToHex6(it).equals(initialColorHex, ignoreCase = true) }.takeIf { it >= 0 } ?: 0)
    }
    PandoraDialog(onDismissRequest = onDismiss) {
            val colors = PandoraTheme.colors
            Column(verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
                Text(if (initialName.isBlank()) "新建标签" else "编辑标签", style = PandoraTypography.titleMedium, color = colors.textPrimary)
                PandoraTextField(
                    name, { name = it },
                    Modifier.fillMaxWidth(), label = { Text("标签名称") }, singleLine = true,
                    isError = name.isBlank()
                )
                Text("配色（可与其他标签相同）", style = PandoraTypography.bodyMedium, color = colors.textSecondary)
                LazyVerticalGrid(columns = GridCells.Fixed(6), modifier = Modifier.height(112.dp), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    items(TagColorPalette.size) { i ->
                        val color = TagColorPalette[i]
                        val selected = i == colorIndex
                        // 色块视觉 40dp，外围由网格行距补足触达；选中用 2dp Focus 边框 + “字”预览
                        Box(
                            Modifier.size(40.dp).clip(PandoraShapes.circle)
                                .background(color)
                                .border(if (selected) PandoraDimensions.focusWidth else PandoraDimensions.borderWidth, if (selected) colors.focus else colors.border, PandoraShapes.circle)
                                .clickable(onClickLabel = "选择第${i + 1}种颜色") { colorIndex = i },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) Text("字", style = PandoraTypography.labelLarge, color = tagContentColor(color, colors.textPrimary))
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().clip(PandoraShapes.medium).background(TagColorPalette[colorIndex]).border(PandoraDimensions.borderWidth, colors.border, PandoraShapes.medium).padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
                    Text(name.ifBlank { "标签预览" }, color = tagContentColor(TagColorPalette[colorIndex], colors.textPrimary), style = PandoraTypography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                error?.let { Text(it, color = colors.error, style = PandoraTypography.bodyMedium) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                    PandoraButton("取消", { onDismiss() }, Modifier.weight(1f), variant = PandoraButtonVariant.Secondary, enabled = !busy)
                    PandoraButton(
                        if (initialName.isBlank()) "创建" else "保存",
                        { onSave(name.trim(), tagHexToHex6(TagColorPalette[colorIndex])) },
                        Modifier.weight(1f), enabled = name.isNotBlank(), loading = busy
                    )
                }
            }
    }
}

/** Color → #RRGGBB（服务端契约要求 #RRGGBB；Compose sRGB 的 ARGB 存于 value 高 32 位，须先 shr 32） */
private fun tagHexToHex6(color: Color): String {
    val argb = (color.value shr 32).toLong() and 0xFFFFFFFFL
    return "#%02X%02X%02X".format((argb shr 16 and 0xFF).toInt(), (argb shr 8 and 0xFF).toInt(), (argb and 0xFF).toInt())
}
