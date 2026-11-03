package com.pandora6ix.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography

/** 二级页顶部栏：最小 64dp、TitleLarge 标题、可选 Caption 副标题、48dp 返回触区 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PandoraTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val c = PandoraTheme.colors
    TopAppBar(
        title = {
            Column {
                Text(title, Modifier.semantics { heading() }, style = PandoraTypography.titleLarge, color = c.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                subtitle?.let { Text(it, style = PandoraTypography.caption, color = c.textSecondary) }
            }
        },
        navigationIcon = { if (onBack != null) IconButton(onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = c.textPrimary) } },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, actionIconContentColor = c.textSecondary)
    )
}

/**
 * 一级页面标题行（导图、日志、AI地图、我的）：TitleLarge、最小 64dp，
 * 与页面内容共用 16dp 左对齐线；视图页保留日历自身的模式标题。
 */
@Composable
fun PandoraPageTitle(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth().heightIn(min = PandoraDimensions.topBarMinHeight).padding(vertical = PandoraSpacing.space8), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = PandoraTypography.titleLarge, color = PandoraTheme.colors.textPrimary)
        actions()
    }
}

/** Tab 行：最小 48dp、LabelLarge、选中 Primary 2dp 下划线，未选 TextSecondary */
@Composable
fun PandoraTabRow(tabs: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = PandoraTheme.colors
    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        containerColor = Color.Transparent,
        contentColor = c.primary,
        indicator = { positions ->
            if (selectedIndex in positions.indices) TabRowDefaults.SecondaryIndicator(
                Modifier.tabIndicatorOffset(positions[selectedIndex]), height = PandoraDimensions.focusWidth, color = c.primary
            )
        },
        divider = { PandoraDivider() }
    ) {
        tabs.forEachIndexed { index, label ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onSelect(index) },
                modifier = Modifier.heightIn(min = PandoraDimensions.touchTarget),
                selectedContentColor = c.primary,
                unselectedContentColor = c.textSecondary
            ) { Text(label, Modifier.padding(vertical = PandoraSpacing.space12), style = PandoraTypography.labelLarge) }
        }
    }
}

/**
 * 人员/设置列表行（规范第 8 节）：头像/图标 + 12dp 间隔 + 标题 BodyLarge/副标题 BodyMedium，
 * 单行最小 56dp、双行最小 72dp，按内容增长。
 */
@Composable
fun PandoraListItem(
    headline: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val c = PandoraTheme.colors
    Row(
        modifier.fillMaxWidth()
            .defaultMinSize(minHeight = if (supporting == null) 56.dp else 72.dp)
            .let { if (onClick != null) it.clickable(role = Role.Button, onClick = onClick) else it }
            .padding(horizontal = PandoraSpacing.space16, vertical = PandoraSpacing.space12),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(PandoraSpacing.space12))
        }
        Column(Modifier.weight(1f)) {
            Text(headline, style = PandoraTypography.bodyLarge, color = c.textPrimary)
            supporting?.let { Text(it, style = PandoraTypography.bodyMedium, color = c.textSecondary) }
        }
        if (trailing != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8), content = trailing)
    }
}

/** 暂态提示（Snackbar 外观）：Ink 底白字、12dp 角、无额外遮罩 */
@Composable
fun PandoraToast(message: String, modifier: Modifier = Modifier) {
    val c = PandoraTheme.colors
    Surface(modifier, shape = PandoraShapes.medium, color = c.snackbarContainer, contentColor = c.snackbarContent) {
        Box(Modifier.padding(horizontal = PandoraSpacing.space16, vertical = PandoraSpacing.space12)) {
            Text(message, style = PandoraTypography.bodyMedium, color = c.snackbarContent)
        }
    }
}

/** 列表加载/空/错误状态的统一占位 */
@Composable
fun PandoraEmptyState(message: String, modifier: Modifier = Modifier, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val c = PandoraTheme.colors
    Column(modifier.fillMaxWidth().padding(PandoraSpacing.space24), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space12)) {
        Text(message, style = PandoraTypography.bodyMedium, color = c.textSecondary)
        if (actionText != null && onAction != null) PandoraButton(actionText, onAction, variant = PandoraButtonVariant.Outline)
    }
}

/** 中性图标按钮：24dp 图标、48dp 触区；选中 Primary、危险 Error */
@Composable
fun PandoraIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    destructive: Boolean = false,
    enabled: Boolean = true
) {
    val c = PandoraTheme.colors
    IconButton(onClick, modifier, enabled = enabled) {
        Icon(icon, contentDescription, Modifier.size(PandoraDimensions.icon),
            tint = when { !enabled -> c.textDisabled; destructive -> c.error; selected -> c.primary; else -> c.textSecondary })
    }
}
