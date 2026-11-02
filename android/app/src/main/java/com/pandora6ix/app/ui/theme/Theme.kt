package com.pandora6ix.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** 品牌与状态固定色：值来自语义 Token，旧名称保留给尚未迁移的调用点 */
@Deprecated("使用 PandoraTheme.colors.brandCoral（仅品牌标识）或 primary")
val Coral = Color(0xFFFF8377)
@Deprecated("使用 PandoraTheme.colors.error")
val CoralDark = Color(0xFFA84844)
/** 正文主色，等同 textPrimary；降低透明度请改用 textSecondary/textTertiary */
val Ink = Color(0xFF302B2A)
@Deprecated("使用 PandoraTheme.colors.errorContainer")
val Peach = Color(0xFFFFE0D8)
@Deprecated("使用 PandoraTheme.colors.successContainer")
val Mint = Color(0xFFDDEEDC)
@Deprecated("使用 PandoraTheme.colors.success")
val MintDeep = Color(0xFF386345)
val Lilac = Color(0xFFE7DDF4)
val Sky = Color(0xFFDCECF2)

/** 任务重要程度的统一视觉元数据：文字颜色与文案（集中映射见 StatusColors.kt） */
@Composable
@ReadOnlyComposable
fun priorityMeta(level: Int): Pair<Color, String> =
    PandoraTheme.colors.statusColors(priorityTone(level)).content to priorityLabel(level)

/**
 * 应用主题：key/名称/按账号保存方式不变，颜色统一由 [PandoraColorTokens] 提供。
 * 旧字段保留为计算属性并指向语义 Token，便于逐页迁移。
 */
data class AppTheme(val key: String, val label: String, val colors: PandoraColorTokens) {
    val background: Color get() = colors.appBackground
    /** 旧导航/深层背景：底部导航按规范使用 AppBackground */
    val backgroundDeep: Color get() = colors.appBackground
    /** 旧首页黄底：规范删除 dashboard 大面积背景角色 */
    val dashboard: Color get() = colors.appBackground
    val card: Color get() = colors.surface
    val cardDeep: Color get() = colors.primaryContainer
    val line: Color get() = colors.border
    val accent: Color get() = colors.primary
    val button: Color get() = colors.primary
}

val appThemes = listOf(
    AppTheme("warm", "暖米", WarmColors),
    AppTheme("mint", "薄荷", MintColors),
    AppTheme("sky", "晴空", SkyColors),
    AppTheme("sakura", "樱花", SakuraColors),
    AppTheme("lilac", "紫藤", LilacColors)
)

fun appThemeFor(key: String?): AppTheme = appThemes.firstOrNull { it.key == key } ?: appThemes.first()

val LocalAppTheme = staticCompositionLocalOf { appThemes.first() }

/** 统一取 Token 的入口 */
object PandoraTheme {
    val colors: PandoraColorTokens
        @Composable @ReadOnlyComposable get() = LocalPandoraColors.current
    val typography: PandoraTypography get() = PandoraTypography
    val spacing: PandoraSpacing get() = PandoraSpacing
    val shapes: PandoraShapes get() = PandoraShapes
}

// 以下旧别名随主题切换，已转接到语义 Token；新代码请直接使用 PandoraTheme.colors
/** 普通内容卡片/输入底 → surface */
val Cream: Color
    @Composable @ReadOnlyComposable get() = LocalPandoraColors.current.surface

/** 选中态/紧凑控件淡底 → primaryContainer */
val CreamDeep: Color
    @Composable @ReadOnlyComposable get() = LocalPandoraColors.current.primaryContainer

/** 内容卡片底色 → surface */
val WarmCard: Color
    @Composable @ReadOnlyComposable get() = LocalPandoraColors.current.surface

/** 旧首页/设置页背景 → appBackground */
val WarmDashboard: Color
    @Composable @ReadOnlyComposable get() = LocalPandoraColors.current.appBackground

/** 强调色：图标选中、开关、链接 → primary */
val WarmOrange: Color
    @Composable @ReadOnlyComposable get() = LocalPandoraColors.current.primary

/** 卡片细边框 → border */
val HairLine: Color
    @Composable @ReadOnlyComposable get() = LocalPandoraColors.current.border

/** 实心主按钮底色 → primary */
val ThemeButton: Color
    @Composable @ReadOnlyComposable get() = LocalPandoraColors.current.primary

/** 主按钮上的文字与图标 → textOnPrimary */
val OnThemeButton: Color
    @Composable @ReadOnlyComposable get() = LocalPandoraColors.current.textOnPrimary

/** 按规范第 10 节由同一套 Token 生成 Material3 配色 */
fun PandoraColorTokens.toMaterialColorScheme(): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = textOnPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = textPrimary,
    secondary = secondary,
    onSecondary = textOnPrimary,
    secondaryContainer = primaryContainer,
    onSecondaryContainer = textPrimary,
    tertiary = info,
    onTertiary = textOnPrimary,
    background = appBackground,
    onBackground = textPrimary,
    surface = elevatedSurface,
    onSurface = textPrimary,
    surfaceVariant = neutralContainer,
    onSurfaceVariant = textSecondary,
    surfaceDim = elevatedSurface,
    surfaceBright = elevatedSurface,
    surfaceContainerLowest = elevatedSurface,
    surfaceContainerLow = elevatedSurface,
    surfaceContainer = elevatedSurface,
    surfaceContainerHigh = elevatedSurface,
    surfaceContainerHighest = elevatedSurface,
    surfaceTint = Color.Transparent,
    inverseSurface = snackbarContainer,
    inverseOnSurface = snackbarContent,
    inversePrimary = primaryContainer,
    error = error,
    onError = textOnPrimary,
    errorContainer = errorContainer,
    onErrorContainer = error,
    outline = inputBorder,
    outlineVariant = border,
    scrim = Color.Black
)

/**
 * 主题入口：同一个 [theme] 同时提供 LocalAppTheme、语义 Token 与 Material3，
 * 不再固定另一套 Coral 配色。
 */
@Composable
fun PandoraTheme(theme: AppTheme = appThemes.first(), content: @Composable () -> Unit) {
    // 五套主题都是浅色背景：系统栏图标统一深色（targetSdk 35 强制 edge-to-edge，默认图标为白色）
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        view.context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }
    CompositionLocalProvider(LocalAppTheme provides theme, LocalPandoraColors provides theme.colors) {
        MaterialTheme(
            colorScheme = theme.colors.toMaterialColorScheme(),
            typography = PandoraMaterialTypography,
            shapes = PandoraMaterialShapes,
            content = content
        )
    }
}

@Composable
fun Pandora6ixTheme(content: @Composable () -> Unit) = PandoraTheme(content = content)

/**
 * 全局开关配色：开启用 primary 轨道，关闭用中性边圈，避免关闭态像错误或禁用。
 */
@Composable
fun pandoraSwitchColors() = PandoraTheme.colors.let { c ->
    androidx.compose.material3.SwitchDefaults.colors(
        checkedThumbColor = c.elevatedSurface,
        checkedTrackColor = c.primary,
        uncheckedThumbColor = c.inputBorder,
        uncheckedTrackColor = c.neutralContainer,
        uncheckedBorderColor = c.inputBorder
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
