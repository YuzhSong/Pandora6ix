package com.pandora6ix.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography

/** 面板底色：普通 Surface 或用户确认的白色功能面板 */
enum class PandoraPanelTone { Surface, White }

/**
 * 基础面板：0 阴影 + 1dp 中性边框 + 16dp 角/内距（规范第 8 节）。
 * 业务面板（导图板块、汇总面板等）在此之上组合，不把业务模型塞进来。
 */
@Composable
fun PandoraPanel(
    modifier: Modifier = Modifier,
    tone: PandoraPanelTone = PandoraPanelTone.Surface,
    shape: Shape = PandoraShapes.large,
    contentPadding: PaddingValues = PaddingValues(PandoraSpacing.panelPadding),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = PandoraTheme.colors
    val color = if (tone == PandoraPanelTone.White) c.panelSurface else c.surface
    val border = BorderStroke(PandoraDimensions.borderWidth, c.border)
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, contentColor = c.textPrimary, border = border) {
        Column(Modifier.padding(contentPadding), content = content)
    } else Surface(modifier = modifier, shape = shape, color = color, contentColor = c.textPrimary, border = border) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

/** 兼容任务要求的命名：普通卡片即 PandoraPanel */
@Composable
fun PandoraCard(
    modifier: Modifier = Modifier,
    tone: PandoraPanelTone = PandoraPanelTone.Surface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) = PandoraPanel(modifier, tone, onClick = onClick, content = content)

/** 面板标题行：TitleSmall + 可选右侧动作，标题到内容 12dp 由调用方的 Column 间距控制 */
@Composable
fun PandoraPanelHeader(title: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, Modifier.weight(1f).semantics { heading() }, style = PandoraTypography.titleSmall, color = PandoraTheme.colors.textPrimary)
        actions()
    }
}

/** 任务/日志独立条目容器：Surface/12 角/1dp 边框，横 12 纵 8 */
@Composable
fun PandoraItemContainer(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    color: Color = PandoraTheme.colors.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = PandoraTheme.colors
    val border = BorderStroke(PandoraDimensions.borderWidth, c.border)
    val body: @Composable () -> Unit = {
        Column(
            Modifier.padding(horizontal = PandoraSpacing.itemHorizontal, vertical = PandoraSpacing.itemVertical),
            verticalArrangement = Arrangement.spacedBy(PandoraSpacing.space4), content = content
        )
    }
    if (onClick != null) Surface(onClick = onClick, modifier = modifier, shape = PandoraShapes.medium, color = color, contentColor = c.textPrimary, border = border, content = body)
    else Surface(modifier = modifier, shape = PandoraShapes.medium, color = color, contentColor = c.textPrimary, border = border, content = body)
}
