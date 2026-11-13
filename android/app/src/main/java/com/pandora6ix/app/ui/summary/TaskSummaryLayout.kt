package com.pandora6ix.app.ui.summary

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.ui.components.*
import com.pandora6ix.app.ui.theme.*

/** Parent Scaffold owns safe insets; this toolbar never applies them a second time. */
@Composable
internal fun TaskSummaryLayout(
    compact: Boolean,
    taskScope: String,
    onScope: (String) -> Unit,
    onBack: () -> Unit,
    onRotate: () -> Unit,
    rotationLocked: Boolean,
    onRefresh: () -> Unit,
    refreshEnabled: Boolean,
    table: @Composable (Modifier) -> Unit
) {
    var scopeMenu by remember { mutableStateOf(false) }
    val actions: @Composable RowScope.() -> Unit = {
        IconButton(onRotate) {
            Icon(Icons.Default.ScreenRotation, if (rotationLocked) "恢复竖屏" else "横屏查看",
                tint = if (rotationLocked) PandoraTheme.colors.primary else PandoraTheme.colors.textSecondary)
        }
        IconButton(onRefresh, enabled = refreshEnabled) { Icon(Icons.Default.Refresh, "刷新") }
    }
    Column(Modifier.fillMaxSize()) {
        if (compact) {
            Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                Text("任务汇总", Modifier.weight(1f).semantics { heading() }, style = PandoraTypography.titleSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Box {
                    TextButton({ scopeMenu = true }) {
                        Text(TaskScopes.first { it.first == taskScope }.second, style = PandoraTypography.labelLarge)
                        Icon(Icons.Default.ArrowDropDown, "选择任务范围")
                    }
                    PandoraDropdownMenu(scopeMenu, { scopeMenu = false }) {
                        TaskScopes.forEach { (value, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { scopeMenu = false; onScope(value) })
                        }
                    }
                }
                actions()
            }
        } else {
            PandoraTopBar("任务汇总", onBack = onBack, actions = actions)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = PandoraSpacing.space12)
                .padding(bottom = if (compact) PandoraSpacing.space4 else PandoraSpacing.space12),
            verticalArrangement = Arrangement.spacedBy(if (compact) 0.dp else PandoraSpacing.space8)
        ) {
            if (!compact) PandoraTabRow(TaskScopes.map { it.second }, TaskScopes.indexOfFirst { it.first == taskScope }.coerceAtLeast(0), { onScope(TaskScopes[it].first) })
            table(Modifier.weight(1f))
        }
    }
}
