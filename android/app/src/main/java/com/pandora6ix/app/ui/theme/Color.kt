package com.pandora6ix.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Pandora 语义颜色 Token（docs/ANDROID_UI_DESIGN_SYSTEM.md 第 2 节）。
 * 五套主题只替换背景三层与主色系，正文中性色、状态色和白色浮层保持一致。
 */
@Immutable
data class PandoraColorTokens(
    val appBackground: Color,
    val surface: Color,
    val elevatedSurface: Color,
    val primary: Color,
    val primaryPressed: Color,
    val primaryContainer: Color,
    val primaryDisabled: Color = Color(0xFFE8DDD7),
    val textPrimary: Color = Color(0xFF302B2A),
    val textSecondary: Color = Color(0xFF615B59),
    val textTertiary: Color = Color(0xFF6F6967),
    val textDisabled: Color = Color(0xFF9A9490),
    val textOnPrimary: Color = Color.White,
    val brandCoral: Color = Color(0xFFFF8377),
    val secondary: Color = Color(0xFF615B59),
    val border: Color = Color(0xFFD8D3CF),
    /** 卡片描边：原主题同 border；卡通主题为深色手绘描边 */
    val cardBorder: Color = Color(0xFFD8D3CF),
    val inputBorder: Color = Color(0xFF827A75),
    val divider: Color = Color(0xFFE8E3DF),
    val success: Color = Color(0xFF386345),
    val successContainer: Color = Color(0xFFDDEEDC),
    val warning: Color = Color(0xFF78580B),
    val warningContainer: Color = Color(0xFFFFF2C9),
    val error: Color = Color(0xFFA84844),
    val errorContainer: Color = Color(0xFFFFE0D8),
    val info: Color = Color(0xFF355F7A),
    val infoContainer: Color = Color(0xFFDCECF2),
    val neutralContainer: Color = Color(0xFFEEEAE6),
    val scrim: Color = Color(0x66000000),
    val snackbarContainer: Color = Color(0xFF302B2A),
    val snackbarContent: Color = Color.White,
    val ripple: Color = Color(0x1F302B2A)
) {
    /** 用户确认的白色功能面板（汇总、成员、任务详情）复用浮层白，不另设底色 */
    val panelSurface: Color get() = elevatedSurface
    val focus: Color get() = primary
}

private val ElevatedWhite = Color(0xFFFFFFFF)

val WarmColors = PandoraColorTokens(
    appBackground = Color(0xFFFFFBF2), surface = Color(0xFFFFFCF4), elevatedSurface = ElevatedWhite,
    primary = Color(0xFFA65E49), primaryPressed = Color(0xFF8D4C3B), primaryContainer = Color(0xFFF3E3DC)
)
val MintColors = PandoraColorTokens(
    appBackground = Color(0xFFE8F4EC), surface = Color(0xFFF4FBF7), elevatedSurface = ElevatedWhite,
    primary = Color(0xFF487657), primaryPressed = Color(0xFF3D634A), primaryContainer = Color(0xFFDDEEDC)
)
val SkyColors = PandoraColorTokens(
    appBackground = Color(0xFFE9F1FA), surface = Color(0xFFF6FAFF), elevatedSurface = ElevatedWhite,
    primary = Color(0xFF3F6F96), primaryPressed = Color(0xFF335B7C), primaryContainer = Color(0xFFDCECF2)
)
val SakuraColors = PandoraColorTokens(
    appBackground = Color(0xFFFBF0F3), surface = Color(0xFFFDF8FA), elevatedSurface = ElevatedWhite,
    primary = Color(0xFF9E526A), primaryPressed = Color(0xFF834257), primaryContainer = Color(0xFFF6E2E9)
)
val LilacColors = PandoraColorTokens(
    appBackground = Color(0xFFF1EDFA), surface = Color(0xFFFAF8FF), elevatedSurface = ElevatedWhite,
    primary = Color(0xFF6E5B99), primaryPressed = Color(0xFF59487E), primaryContainer = Color(0xFFE7DDF4)
)

// ---- 卡通主题：配色取自参考素材，按钮/链接主色加深以满足 4.5:1 ----
private val ToonOutline = Color(0xFF3A322E)

val ToonCreamColors = PandoraColorTokens(
    appBackground = Color(0xFFFFFEF2), surface = Color(0xFFFFFCEB), elevatedSurface = ElevatedWhite,
    primary = Color(0xFFB04A3A), primaryPressed = Color(0xFF943C2F), primaryContainer = Color(0xFFFFE4DC),
    cardBorder = ToonOutline
)
val ToonBreezeColors = PandoraColorTokens(
    appBackground = Color(0xFFF1FBFF), surface = Color(0xFFF8FDFF), elevatedSurface = ElevatedWhite,
    primary = Color(0xFF2D6A88), primaryPressed = Color(0xFF24566F), primaryContainer = Color(0xFFD9EFFA),
    cardBorder = ToonOutline
)
val ToonFarmColors = PandoraColorTokens(
    appBackground = Color(0xFFFFF6C5), surface = Color(0xFFFFFBE6), elevatedSurface = ElevatedWhite,
    primary = Color(0xFF96541C), primaryPressed = Color(0xFF7C4517), primaryContainer = Color(0xFFFFE7B0),
    cardBorder = ToonOutline
)
val ToonSpringColors = PandoraColorTokens(
    appBackground = Color(0xFFEAF6C2), surface = Color(0xFFF7FBE6), elevatedSurface = ElevatedWhite,
    primary = Color(0xFF44702A), primaryPressed = Color(0xFF385D22), primaryContainer = Color(0xFFDDEDB4),
    cardBorder = ToonOutline
)

val LocalPandoraColors = staticCompositionLocalOf { WarmColors }

/** sRGB 相对亮度（WCAG 2.x），用于对比度校验与标签文字取色 */
fun relativeLuminance(color: Color): Double {
    fun channel(c: Float): Double {
        val v = c.toDouble()
        return if (v <= 0.03928) v / 12.92 else Math.pow((v + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)
}

fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}
