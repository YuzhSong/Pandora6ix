package com.pandora6ix.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraTheme

/**
 * 自制浮层（展开面板、个人事项编辑、通知抽屉等）的统一外壳：
 * 白色 ElevatedSurface、Dialog 24dp 角、6dp 阴影，不再各自写 10–14dp 厚阴影。
 */
@Composable
fun PandoraOverlaySurface(
    modifier: Modifier = Modifier,
    shape: Shape = PandoraShapes.dialog,
    shadowElevation: androidx.compose.ui.unit.Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val c = PandoraTheme.colors
    Surface(modifier, shape = shape, color = c.elevatedSurface, contentColor = c.textPrimary, shadowElevation = shadowElevation) {
        Box(content = content)
    }
}

/** 列表 1dp 分割线（规范第 8 节：同类列表选择分割线时统一使用 Divider） */
@Composable
fun PandoraDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, thickness = PandoraDimensions.borderWidth, color = PandoraTheme.colors.divider)
}
