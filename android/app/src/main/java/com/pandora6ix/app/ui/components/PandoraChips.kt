package com.pandora6ix.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pandora6ix.app.ui.theme.PandoraDimensions
import com.pandora6ix.app.ui.theme.PandoraShapes
import com.pandora6ix.app.ui.theme.PandoraSpacing
import com.pandora6ix.app.ui.theme.PandoraTheme
import com.pandora6ix.app.ui.theme.PandoraTypography
import com.pandora6ix.app.ui.theme.StatusTone
import com.pandora6ix.app.ui.theme.contrastRatio
import com.pandora6ix.app.ui.theme.statusColors

/** 服务端标签色解析：无效值返回 null，由调用方落到 NeutralContainer */
fun parseTagColor(hex: String?): Color? =
    hex?.takeIf { it.isNotBlank() }?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }

/** 标签文字在 TextPrimary 与白色中按更高对比度选择（规范 2.3） */
fun tagContentColor(background: Color, textPrimary: Color = Color(0xFF302B2A)): Color =
    if (contrastRatio(textPrimary, background) >= contrastRatio(Color.White, background)) textPrimary else Color.White

/**
 * 业务标签：服务端原色 100% 填充 + 1dp 中性边框，主标签附“主”标识。
 * 不再按页面各自 35%/40% 透明或 lerp 加深。
 */
@Composable
fun PandoraTagChip(
    name: String,
    colorHex: String?,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    maxWidth: Dp = 180.dp,
    onClick: (() -> Unit)? = null
) {
    val c = PandoraTheme.colors
    val bg = parseTagColor(colorHex) ?: c.neutralContainer
    val fg = tagContentColor(bg, c.textPrimary)
    val label = if (primary) "主 · $name" else name
    // 外部给最小高度（如 32dp 可点击标签）时文字保持垂直居中
    val body: @Composable () -> Unit = {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Text(label, Modifier.padding(horizontal = PandoraSpacing.space8, vertical = PandoraSpacing.space2),
                style = PandoraTypography.caption, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
    val border = BorderStroke(PandoraDimensions.borderWidth, c.border)
    if (onClick != null) Surface(onClick = onClick, modifier = modifier.widthIn(max = maxWidth), shape = PandoraShapes.pill, color = bg, border = border, content = body)
    else Surface(modifier.widthIn(max = maxWidth), shape = PandoraShapes.pill, color = bg, border = border, content = body)
}

/** 状态标签：语义 Container 底 + 对应文字色，文字必填（不能只靠颜色表达状态） */
@Composable
fun PandoraStatusChip(text: String, tone: StatusTone, modifier: Modifier = Modifier, maxWidth: Dp = 120.dp) {
    val colors = PandoraTheme.colors.statusColors(tone)
    Surface(modifier.widthIn(max = maxWidth), shape = PandoraShapes.pill, color = colors.container) {
        Text(text, Modifier.padding(horizontal = PandoraSpacing.space8, vertical = PandoraSpacing.space2),
            style = PandoraTypography.caption, color = colors.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** 筛选/范围选择胶囊：选中 PrimaryContainer + TextPrimary（暖米 Primary 在淡底上不足 4.5:1），未选透明 + 中性边框 */
@Composable
fun PandoraChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = PandoraTheme.colors
    Surface(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = PandoraDimensions.chipButtonHeight)
            .semantics { this.selected = selected; role = Role.Tab },
        shape = PandoraShapes.pill,
        color = if (selected) c.primaryContainer else Color.Transparent,
        border = if (selected) null else BorderStroke(PandoraDimensions.borderWidth, c.border)
    ) {
        Row(Modifier.padding(horizontal = PandoraSpacing.space12, vertical = PandoraSpacing.space4),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(label, style = PandoraTypography.labelLarge, color = if (selected) c.textPrimary else c.textSecondary, maxLines = 1, softWrap = false)
        }
    }
}
