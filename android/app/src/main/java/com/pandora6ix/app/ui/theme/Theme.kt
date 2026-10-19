package com.pandora6ix.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** 图表与标签用的品牌固定色（不随主题切换） */
val Coral = Color(0xFFFF8377)
val CoralDark = Color(0xFFB34E4A)
val Ink = Color(0xFF302B2A)
val Peach = Color(0xFFFFE0D8)
val Mint = Color(0xFFDDEEDC)
val MintDeep = Color(0xFF4C7A5C)
val Lilac = Color(0xFFE7DDF4)
val Sky = Color(0xFFDCECF2)

/**
 * 应用主题：背景、卡片、描边、强调色与按钮底色成套切换。
 * 卡片/描边与背景同一色系但深浅不同，保证层次可见又不跳色。
 */
data class AppTheme(
    val key: String,
    val label: String,
    val background: Color,
    val backgroundDeep: Color,
    val dashboard: Color,
    val card: Color,
    val cardDeep: Color,
    val line: Color,
    val accent: Color,
    val button: Color
)

val appThemes = listOf(
    AppTheme(
        "warm", "暖米",
        background = Color(0xFFFFFBF2), backgroundDeep = Color(0xFFFFF2C9), dashboard = Color(0xFFFFF1C7),
        card = Color(0xFFFFFCF4), cardDeep = Color(0xFFFFF2C9), line = Color(0xFFE3C79B),
        accent = Color(0xFFE3A083), button = Color(0xFFB8705A)
    ),
    AppTheme(
        "mint", "薄荷",
        background = Color(0xFFE8F4EC), backgroundDeep = Color(0xFFD9EAE0), dashboard = Color(0xFFE2F0E7),
        card = Color(0xFFF4FBF7), cardDeep = Color(0xFFDCEDE3), line = Color(0xFFA9C7B6),
        accent = Color(0xFF6F9E85), button = Color(0xFF4C7A5C)
    ),
    AppTheme(
        "sky", "晴空",
        background = Color(0xFFE9F1FA), backgroundDeep = Color(0xFFD9E7F4), dashboard = Color(0xFFE3EDF8),
        card = Color(0xFFF6FAFF), cardDeep = Color(0xFFDCEAF8), line = Color(0xFFAEC7E0),
        accent = Color(0xFF7FA3C9), button = Color(0xFF4A7CA5)
    ),
    AppTheme(
        "sakura", "樱花",
        background = Color(0xFFFBF0F3), backgroundDeep = Color(0xFFF7E1E8), dashboard = Color(0xFFF9E7EB),
        card = Color(0xFFFDF8FA), cardDeep = Color(0xFFF9E1E8), line = Color(0xFFE9B9C6),
        accent = Color(0xFFD98BA0), button = Color(0xFFB4657C)
    ),
    AppTheme(
        "lilac", "紫藤",
        background = Color(0xFFF1EDFA), backgroundDeep = Color(0xFFE4DEF3), dashboard = Color(0xFFEAE5F7),
        card = Color(0xFFFAF8FF), cardDeep = Color(0xFFE8E2F7), line = Color(0xFFC3B5E0),
        accent = Color(0xFF9A86C4), button = Color(0xFF6E5B99)
    )
)

val LocalAppTheme = staticCompositionLocalOf { appThemes.first() }

// 以下颜色随主题切换：既有调用点无需改动即可跟随主题
/** 页面/弹层/卡片主底色 */
val Cream: Color
    @Composable @ReadOnlyComposable get() = LocalAppTheme.current.card

/** 卡片嵌套层、选中态底色 */
val CreamDeep: Color
    @Composable @ReadOnlyComposable get() = LocalAppTheme.current.cardDeep

/** 内容卡片底色 */
val WarmCard: Color
    @Composable @ReadOnlyComposable get() = LocalAppTheme.current.card

/** 首页/设置页专属背景色 */
val WarmDashboard: Color
    @Composable @ReadOnlyComposable get() = LocalAppTheme.current.dashboard

/** 强调色：图标、开关、进度、可点文字 */
val WarmOrange: Color
    @Composable @ReadOnlyComposable get() = LocalAppTheme.current.accent

/** 描边与分隔线 */
val HairLine: Color
    @Composable @ReadOnlyComposable get() = LocalAppTheme.current.line

/** 实心按钮底色（与背景同色系、比背景深，保证白字可读） */
val ThemeButton: Color
    @Composable @ReadOnlyComposable get() = LocalAppTheme.current.button

/** 深色按钮上的文字与图标：固定白色 */
val OnThemeButton: Color
    @Composable @ReadOnlyComposable get() = Color.White

private val PandoraColors = lightColorScheme(
    primary = Coral,
    onPrimary = Color.White,
    secondary = Color(0xFFFFF2C9),
    background = Color(0xFFFFFBF2),
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    outline = Coral.copy(alpha = 0.65f)
)

@Composable
fun PandoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PandoraColors, content = content)
}

@Composable
fun Pandora6ixTheme(content: @Composable () -> Unit) = PandoraTheme(content)

/**
 * 全局开关（Switch）配色：关闭态边圈用中性 Ink 而非主题 outline（珊瑚红），
 * 避免关闭状态看起来像错误/禁用；开启态沿用主题暖橙。
 */
@Composable
fun pandoraSwitchColors() = androidx.compose.material3.SwitchDefaults.colors(
    checkedThumbColor = Cream,
    checkedTrackColor = WarmOrange,
    uncheckedThumbColor = Cream,
    uncheckedTrackColor = Ink.copy(alpha = 0.25f),
    uncheckedBorderColor = Ink.copy(alpha = 0.25f)
)
