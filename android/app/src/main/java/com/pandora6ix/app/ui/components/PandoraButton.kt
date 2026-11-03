package com.pandora6ix.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography

/** 按钮变体（规范第 6 节）；调用方只选语义，不自由指定颜色 */
enum class PandoraButtonVariant { Primary, Secondary, Outline, Text, Destructive, DestructiveFilled }

/** 常规 48dp；紧凑 40dp；标签式小入口 32dp（外围触区仍由 Material 扩展到 48dp） */
enum class PandoraButtonSize { Regular, Compact, Chip }

@Composable
fun PandoraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PandoraButtonVariant = PandoraButtonVariant.Primary,
    size: PandoraButtonSize = PandoraButtonSize.Regular,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingText: String = "保存中…",
    icon: ImageVector? = null
) {
    PandoraButton(onClick, modifier, variant, size, enabled, loading) {
        Box(contentAlignment = Alignment.Center) {
            // 加载时保留原文字占位，避免按钮宽度跳动
            Row(Modifier.alpha(if (loading) 0f else 1f), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                icon?.let { Icon(it, null, Modifier.size(PandoraDimensions.iconSmall + 2.dp)) }
                Text(text, style = PandoraTypography.labelLarge, textAlign = TextAlign.Center)
            }
            if (loading) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(PandoraSpacing.space8)) {
                CircularProgressIndicator(Modifier.size(PandoraDimensions.iconSmall), strokeWidth = 2.dp, color = LocalButtonContent.current)
                Text(loadingText, style = PandoraTypography.labelLarge, maxLines = 1)
            }
        }
    }
}

private val LocalButtonContent = androidx.compose.runtime.compositionLocalOf { Color.Unspecified }

@Composable
fun PandoraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: PandoraButtonVariant = PandoraButtonVariant.Primary,
    size: PandoraButtonSize = PandoraButtonSize.Regular,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val c = PandoraTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val (container, contentColor, border) = when (variant) {
        PandoraButtonVariant.Primary -> Triple(if (pressed) c.primaryPressed else c.primary, c.textOnPrimary, null)
        PandoraButtonVariant.Secondary -> Triple(c.primaryContainer, c.textPrimary, null)
        PandoraButtonVariant.Outline -> Triple(Color.Transparent, c.primary, BorderStroke(PandoraDimensions.borderWidth, c.border))
        PandoraButtonVariant.Text -> Triple(Color.Transparent, c.primary, null)
        PandoraButtonVariant.Destructive -> Triple(if (pressed) c.errorContainer else Color.Transparent, c.error, BorderStroke(PandoraDimensions.borderWidth, c.error))
        PandoraButtonVariant.DestructiveFilled -> Triple(c.error, c.textOnPrimary, null)
    }
    val disabledContainer = when (variant) {
        PandoraButtonVariant.Primary -> c.primaryDisabled
        PandoraButtonVariant.Secondary -> c.neutralContainer
        PandoraButtonVariant.DestructiveFilled -> c.primaryDisabled
        else -> Color.Transparent
    }
    val disabledBorder = if (border != null) BorderStroke(PandoraDimensions.borderWidth, c.divider) else null
    val active = enabled && !loading
    val height = when (size) {
        PandoraButtonSize.Regular -> PandoraDimensions.buttonHeight
        PandoraButtonSize.Compact -> PandoraDimensions.compactButtonHeight
        PandoraButtonSize.Chip -> PandoraDimensions.chipButtonHeight
    }
    val horizontal = if (size == PandoraButtonSize.Regular) PandoraSpacing.space16 else PandoraSpacing.space12
    androidx.compose.runtime.CompositionLocalProvider(LocalButtonContent provides contentColor) {
        Button(
            onClick = onClick,
            modifier = modifier
                .defaultMinSize(minWidth = 64.dp, minHeight = height)
                .semantics { if (loading) stateDescription = "处理中" },
            enabled = active,
            shape = if (size == PandoraButtonSize.Chip) PandoraShapes.pill else PandoraShapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = container, contentColor = contentColor,
                disabledContainerColor = if (loading) container else disabledContainer,
                disabledContentColor = if (loading) contentColor else c.textDisabled
            ),
            border = if (active || loading) border else disabledBorder,
            elevation = null,
            contentPadding = PaddingValues(horizontal = horizontal, vertical = 0.dp),
            interactionSource = interaction,
            content = content
        )
    }
}
